package com.dmoniak.patches.capcut

import app.morphe.patcher.annotation.CompatiblePackage
import app.morphe.patcher.annotation.Patch
import app.morphe.patcher.fingerprint.Fingerprint
import app.morphe.patcher.patch.BytecodePatch
import app.morphe.patcher.patch.PatchException
import com.dmoniak.patches.capcut.CapCutConstants.COMPATIBILITY_CAPCUT
import com.dmoniak.patches.capcut.CapCutConstants.COMPATIBILITY_CAPCUT_ALT

@Patch(
    name = "Bypass Integrity & Update Dialog - CapCut (Experimental)",
    description = "⚠️ [Experimental / Untested] Bypasses CapCut tamper detection, security advisory notice, and force update popups.",
    compatiblePackages = [
        CompatiblePackage(COMPATIBILITY_CAPCUT),
        CompatiblePackage(COMPATIBILITY_CAPCUT_ALT)
    ]
)
@Suppress("unused")
object CapCutBypassIntegrityPatch : BytecodePatch() {

    override fun execute() {
        // 1. Neutralize VersionUpdateService popup & force update check
        runCatching {
            val updateServiceClass = classDefBy("Lcom/vega/main/update/VersionUpdateService;")
            updateServiceClass.methods.forEach { method ->
                // Method returning boolean (checks if update/popup is needed) -> return false
                if (method.returnType == "Z") {
                    val regCount = maxOf(4, method.parameters.size + 2)
                    method.toMutable().replaceInstructions("""
                        const/4 v0, 0x0
                        return v0
                    """, regCount)
                }
                // Show dialog methods -> return-void
                if (method.name.startsWith("show") || method.name == "c" || method.name == "d" || method.name == "e") {
                    if (method.returnType == "V") {
                        method.toMutable().replaceInstructions("return-void", 1)
                    }
                }
            }
        }

        // 2. Neutralize UpdatePopupDialogBuilder if present
        runCatching {
            val dialogBuilder = classDefBy("Lcom/vega/main/update/UpdatePopupDialogBuilder;")
            dialogBuilder.methods.filter { it.returnType == "V" && !it.name.startsWith("<") }.forEach {
                it.toMutable().replaceInstructions("return-void", 1)
            }
        }

        // 3. Bypass SignUtils signature verification
        runCatching {
            val signUtils = classDefBy("Lcom/bytedance/common/utility/SignUtils;")
            signUtils.methods.forEach { method ->
                if (method.name == "checkSignature" && method.returnType == "Z") {
                    val regCount = maxOf(4, method.parameters.size + 2)
                    method.toMutable().replaceInstructions("""
                        const/4 v0, 0x1
                        return v0
                    """, regCount)
                }
            }
        }

        // 4. Intercept getInstallerPackageName to return official Play Store package name
        runCatching {
            classes.forEach { classDef ->
                classDef.methods.forEach { method ->
                    if (method.returnType == "Ljava/lang/String;" && 
                        (method.name.contains("Installer", ignoreCase = true) || 
                         method.name.contains("InstallSource", ignoreCase = true))) {
                        val regCount = maxOf(4, method.parameters.size + 2)
                        method.toMutable().replaceInstructions("""
                            const-string v0, "com.android.vending"
                            return-object v0
                        """, regCount)
                    }
                }
            }
        }
    }
}

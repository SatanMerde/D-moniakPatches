package com.dmoniak.patches.googledrive

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.AccessFlags
import com.dmoniak.patches.hungryshark.util.findMutableMethodOf
import com.dmoniak.patches.shared.Constants.COMPATIBILITY_GOOGLE_DRIVE
import java.util.logging.Logger

@Suppress("unused")
val googleDriveAmoledDeclutterPatch = bytecodePatch(
    name = "AMOLED Dark Theme & Declutter - Google Drive (Experimental)",
    description = "⚠️ [En cours de développement / Non testé] Injects pure OLED pitch black (#000000) into Google Drive file list, folder browser, and detail views, and hides Google One storage upsell banners and upgrade prompts.",
) {
    compatibleWith(COMPATIBILITY_GOOGLE_DRIVE)

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        executeGoogleDriveAmoledDeclutterLogic(logger)
    }
}

fun BytecodePatchContext.executeGoogleDriveAmoledDeclutterLogic(logger: Logger) {
    logger.info("Executing AMOLED Dark Theme & Declutter patch for Google Drive...")
    var hookedPoints = 0

    classDefForEach { classDef ->
        val tl = classDef.type.lowercase()
        if (tl.contains("androidx") || tl.contains("android/support")) return@classDefForEach

        val mutableClass by lazy { mutableClassDefBy(classDef) }

        for (method in classDef.methods.toList()) {
            if (method.implementation == null) continue
            val isStatic = AccessFlags.STATIC.isSet(method.accessFlags)
            val mName = method.name.lowercase()
            val retType = method.returnType

            // Hook dark background colors -> pure OLED pitch black (#000000)
            if (!isStatic && (
                mName == "getdrivebackgroundcolor" ||
                mName == "getfilelistbackgroundcolor" ||
                mName == "getdarkmodebackground" ||
                mName == "getnavigationbarcolor"
            ) && retType == "I") {
                try {
                    val mutableMethod = mutableClass.findMutableMethodOf(method)
                    mutableMethod.addInstructions(
                        0,
                        """
                        const/high16 v0, -0x1000000
                        return v0
                        """.trimIndent()
                    )
                    hookedPoints++
                    logger.info("[Drive AMOLED] Injected pure black in: ${classDef.type}->${method.name}")
                } catch (e: Exception) {
                    logger.warning("[Drive AMOLED] Failed to hook ${method.name}: ${e.message}")
                }
            }

            // Suppress Google One / storage upsell banners
            if (!isStatic && (
                mName == "shouldshowstorageupgradebanner" ||
                mName == "isstoragequotabannervisible" ||
                mName == "shouldpromptgoogleoneupgrade" ||
                mName == "isoneupselldialogshown"
            ) && retType == "Z") {
                try {
                    val mutableMethod = mutableClass.findMutableMethodOf(method)
                    mutableMethod.addInstructions(
                        0,
                        """
                        const/4 v0, 0x0
                        return v0
                        """.trimIndent()
                    )
                    hookedPoints++
                    logger.info("[Drive Declutter] Suppressed upsell banner in: ${classDef.type}->${method.name}")
                } catch (e: Exception) {
                    logger.warning("[Drive Declutter] Failed to hook ${method.name}: ${e.message}")
                }
            }
        }
    }
    logger.info("[Google Drive AMOLED & Declutter] Total hooks applied: $hookedPoints")
}

package com.dmoniak.patches.bitwarden

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.AccessFlags
import com.dmoniak.patches.hungryshark.util.findMutableMethodOf
import com.dmoniak.patches.shared.Constants.COMPATIBILITY_BITWARDEN
import java.util.logging.Logger

@Suppress("unused")
val bitwardenUnlockPremiumTotpPatch = bytecodePatch(
    name = "Unlock Premium & Integrated TOTP 2FA - Bitwarden (Experimental)",
    description = "⚠️ [En cours de développement / Non testé] Unlocks Bitwarden Premium features: generates integrated 2FA TOTP authentication codes directly inside vault items, unlocks vault health reports, and enables priority attachment management.",
) {
    compatibleWith(COMPATIBILITY_BITWARDEN)

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        executeBitwardenUnlockPremiumTotpLogic(logger)
    }
}

fun BytecodePatchContext.executeBitwardenUnlockPremiumTotpLogic(logger: Logger) {
    logger.info("Executing Unlock Premium & Integrated TOTP patch for Bitwarden...")
    var hookedPoints = 0

    classDefForEach { classDef ->
        val tl = classDef.type.lowercase()
        if (tl.contains("androidx") || tl.contains("android/support") || tl.contains("com/google")) return@classDefForEach

        val mutableClass by lazy { mutableClassDefBy(classDef) }

        for (method in classDef.methods.toList()) {
            if (method.implementation == null) continue
            val isStatic = AccessFlags.STATIC.isSet(method.accessFlags)
            val mName = method.name.lowercase()
            val retType = method.returnType

            // Premium status & TOTP authenticator feature access
            if (!isStatic && (
                mName == "haspremium" ||
                mName == "ispremium" ||
                mName == "haspremiumaccess" ||
                mName == "canusetotp" ||
                mName == "istotpenabled" ||
                mName == "canaccessvaulthealthreports" ||
                mName == "canaccessreports" ||
                mName == "hasemergencyaccess" ||
                mName == "ispremiumfromorganization"
            ) && retType == "Z") {
                try {
                    val mutableMethod = mutableClass.findMutableMethodOf(method)
                    mutableMethod.addInstructions(
                        0,
                        """
                        const/4 v0, 0x1
                        return v0
                        """.trimIndent()
                    )
                    hookedPoints++
                    logger.info("[Bitwarden Premium/TOTP] Forced Premium/TOTP access in: ${classDef.type}->${method.name}")
                } catch (e: Exception) {
                    logger.warning("[Bitwarden Premium/TOTP] Failed to hook ${method.name}: ${e.message}")
                }
            }
        }
    }
    logger.info("[Bitwarden Premium/TOTP] Total Premium hooks applied: $hookedPoints")
}

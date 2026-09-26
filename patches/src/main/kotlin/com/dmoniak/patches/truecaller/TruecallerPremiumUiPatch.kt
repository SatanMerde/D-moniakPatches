package com.dmoniak.patches.truecaller

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.AccessFlags
import com.dmoniak.patches.hungryshark.util.findMutableMethodOf
import com.dmoniak.patches.shared.Constants.COMPATIBILITY_TRUECALLER
import java.util.logging.Logger

@Suppress("unused")
val truecallerPremiumUiPatch = bytecodePatch(
    name = "Unlock Premium & Gold Features - Truecaller (Experimental)",
    description = "⚠️ [En cours de développement / Non testé] Enables Truecaller Premium and Gold caller ID themes, advanced spam blocking filters, and who-viewed-my-profile indicator UI.",
) {
    compatibleWith(COMPATIBILITY_TRUECALLER)

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        executeTruecallerPremiumUiLogic(logger)
    }
}

fun BytecodePatchContext.executeTruecallerPremiumUiLogic(logger: Logger) {
    logger.info("Executing Unlock Premium & Gold Features patch for Truecaller...")
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

            // 1. Enable Premium and Gold status flags
            if (!isStatic && (
                mName == "ispremium" ||
                mName == "isgold" ||
                mName == "ispremiumuser" ||
                mName == "hasgoldbadge" ||
                mName == "isadvancedspamblockingunlocked" ||
                mName == "canviewprofileviews" ||
                mName == "haspremiumtier"
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
                    logger.info("[Truecaller Premium] Enforced Premium/Gold flag: ${classDef.type}->${method.name}")
                } catch (e: Exception) {
                    logger.warning("[Truecaller Premium] Failed to hook ${method.name}: ${e.message}")
                }
            }
        }
    }
    logger.info("[Truecaller Premium] Total premium caller ID hooks applied: $hookedPoints")
}

package com.dmoniak.patches.movix

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.AccessFlags
import com.dmoniak.patches.hungryshark.util.findMutableMethodOf
import com.dmoniak.patches.shared.Constants.COMPATIBILITY_MOVIX
import java.util.logging.Logger

@Suppress("unused")
val movixUnlockPremiumSubscriptionPatch = bytecodePatch(
    name = "Unlock Premium Subscription - Movix (Experimental)",
    description = "⚠️ [En cours de développement / Non testé] Bypasses Movix VIP/Premium subscription checks to unlock all premium and exclusive content, series, and movie libraries without an active paid subscription.",
) {
    compatibleWith(COMPATIBILITY_MOVIX)

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        executeMovixUnlockPremiumLogic(logger)
    }
}

fun BytecodePatchContext.executeMovixUnlockPremiumLogic(logger: Logger) {
    logger.info("Executing Unlock Premium Subscription patch for Movix...")
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

            if (!isStatic && (
                mName == "ispremiumsubscriber" ||
                mName == "isvipuser" ||
                mName == "haspremiumaccess" ||
                mName == "canwatchpremiumcontent" ||
                mName == "issubscriptionactive" ||
                mName == "haspaidsubscription"
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
                    logger.info("[Movix Premium] Unlocked subscription in: ${classDef.type}->${method.name}")
                } catch (e: Exception) {
                    logger.warning("[Movix Premium] Failed to hook ${method.name}: ${e.message}")
                }
            }
        }
    }
    logger.info("[Movix Unlock Premium] Total hooks applied: $hookedPoints")
}

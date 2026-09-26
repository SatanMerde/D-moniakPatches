package com.dmoniak.patches.peak

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.AccessFlags
import com.dmoniak.patches.hungryshark.util.findMutableMethodOf
import com.dmoniak.patches.shared.Constants.COMPATIBILITY_PEAK
import java.util.logging.Logger

@Suppress("unused")
val peakBlockAdsPatch = bytecodePatch(
    name = "Block Ads & Promo Screens - Peak Brain Training (Experimental)",
    description = "⚠️ [En cours de développement / Non testé] Strips promotional subscription prompts, rating alerts, and video ads in Peak Brain Training.",
) {
    compatibleWith(COMPATIBILITY_PEAK)

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        executePeakBlockAdsLogic(logger)
    }
}

fun BytecodePatchContext.executePeakBlockAdsLogic(logger: Logger) {
    logger.info("Executing Block Ads patch for Peak...")
    var hookedPoints = 0

    classDefForEach { classDef ->
        val tl = classDef.type.lowercase()
        if (tl.contains("androidx") || tl.contains("android/support") || tl.contains("com/google/android/gms")) return@classDefForEach

        val mutableClass by lazy { mutableClassDefBy(classDef) }

        for (method in classDef.methods.toList()) {
            if (method.implementation == null) continue
            val isStatic = AccessFlags.STATIC.isSet(method.accessFlags)
            val mName = method.name.lowercase()
            val retType = method.returnType

            if (!isStatic && (
                mName == "isadready" ||
                mName == "shouldshowad" ||
                mName == "isinterstitialready" ||
                mName == "shouldshowupgradeprompt" ||
                mName == "shouldshowpaywall"
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
                    logger.info("[Peak Promo] Neutralized promo check in: ${classDef.type}->${method.name}")
                } catch (e: Exception) {
                    logger.warning("[Peak Promo] Failed to hook ${method.name}: ${e.message}")
                }
            }

            if (!isStatic && (
                mName == "showupgradescreen" ||
                mName == "showpaywall" ||
                mName == "showinterstitial" ||
                mName == "showad"
            ) && retType == "V") {
                try {
                    val mutableMethod = mutableClass.findMutableMethodOf(method)
                    mutableMethod.addInstructions(
                        0,
                        """
                        return-void
                        """.trimIndent()
                    )
                    hookedPoints++
                    logger.info("[Peak Promo] Neutralized promo show in: ${classDef.type}->${method.name}")
                } catch (e: Exception) {
                    logger.warning("[Peak Promo] Failed to hook ${method.name}: ${e.message}")
                }
            }
        }
    }
    logger.info("[Peak Promo] Total promo hooks applied: $hookedPoints")
}

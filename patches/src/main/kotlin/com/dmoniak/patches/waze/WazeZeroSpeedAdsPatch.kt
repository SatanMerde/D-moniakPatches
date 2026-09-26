package com.dmoniak.patches.waze

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.AccessFlags
import com.dmoniak.patches.hungryshark.util.findMutableMethodOf
import com.dmoniak.patches.shared.Constants.COMPATIBILITY_WAZE
import java.util.logging.Logger

@Suppress("unused")
val wazeZeroSpeedAdsPatch = bytecodePatch(
    name = "Block Zero-Speed Ads & Sponsored Pins - Waze (Experimental)",
    description = "⚠️ [En cours de développement / Non testé] Prevents full-screen commercial banner popups from appearing when the vehicle is stopped at traffic lights or in congestion, and hides sponsored venue pins from the navigation map.",
) {
    compatibleWith(COMPATIBILITY_WAZE)

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        executeWazeZeroSpeedAdsLogic(logger)
    }
}

fun BytecodePatchContext.executeWazeZeroSpeedAdsLogic(logger: Logger) {
    logger.info("Executing Block Zero-Speed Ads patch for Waze...")
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

            // 1. Zero-speed commercial ad eligibility & display check
            if (!isStatic && (
                mName == "iszerospeedadeligible" ||
                mName == "shouldshowzerospeedad" ||
                mName == "iszerospeedbanneractive" ||
                mName == "canpopzerospeedad" ||
                mName == "issponsoredpin" ||
                mName == "ispromotedlocation" ||
                mName == "hasvenuepromotion" ||
                mName == "shouldshowsponsoredlocation"
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
                    logger.info("[Waze Ads] Disabled zero-speed ad/sponsored pin: ${classDef.type}->${method.name}")
                } catch (e: Exception) {
                    logger.warning("[Waze Ads] Failed to hook ${method.name}: ${e.message}")
                }
            }

            // 2. Void ad-display popup launchers
            if (!isStatic && (
                mName == "showzerospeedad" ||
                mName == "displayzerospeedbanner" ||
                mName == "popcommercialoverlay"
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
                    logger.info("[Waze Ads] Neutralized zero-speed ad launcher: ${classDef.type}->${method.name}")
                } catch (e: Exception) {
                    logger.warning("[Waze Ads] Failed to hook ${method.name}: ${e.message}")
                }
            }
        }
    }
    logger.info("[Waze Ads] Total zero-speed ad prevention hooks applied: $hookedPoints")
}

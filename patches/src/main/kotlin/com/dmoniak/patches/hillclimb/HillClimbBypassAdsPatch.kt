package com.dmoniak.patches.hillclimb

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.AccessFlags
import com.dmoniak.patches.hungryshark.util.findMutableMethodOf
import com.dmoniak.patches.shared.Constants.COMPATIBILITY_HILL_CLIMB
import java.util.logging.Logger

@Suppress("unused")
val hillClimbBypassAdsPatch = bytecodePatch(
    name = "Bypass Rewarded Ads - Hill Climb Racing (Experimental)",
    description = "⚠️ [En cours de développement / Non testé] Bypasses rewarded video ads in Hill Climb Racing for free post-race coin doublers, instant driver revives, and free tuning crates.",
) {
    compatibleWith(COMPATIBILITY_HILL_CLIMB)

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        executeHillClimbBypassAdsLogic(logger)
    }
}

fun BytecodePatchContext.executeHillClimbBypassAdsLogic(logger: Logger) {
    logger.info("Executing Bypass Rewarded Ads patch for Hill Climb Racing...")
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

            // 1. Force rewarded video ad completion flags
            if (!isStatic && (
                mName == "isrewardedadready" ||
                mName == "isrewardedvideoavailable" ||
                mName == "hasusercompletedrewardedad" ||
                mName == "isvideorewardgranted"
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
                    logger.info("[Hill Climb Ads] Granted rewarded ad event: ${classDef.type}->${method.name}")
                } catch (e: Exception) {
                    logger.warning("[Hill Climb Ads] Failed to hook ${method.name}: ${e.message}")
                }
            }

            // 2. Disable interstitial ad triggers
            if (!isStatic && (
                mName == "shouldshowinterstitial" ||
                mName == "canplayinterstitialad"
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
                    logger.info("[Hill Climb Ads] Blocked interstitial: ${classDef.type}->${method.name}")
                } catch (e: Exception) {
                    logger.warning("[Hill Climb Ads] Failed to hook ${method.name}: ${e.message}")
                }
            }
        }
    }
    logger.info("[Hill Climb Ads] Total ad bypass hooks applied: $hookedPoints")
}

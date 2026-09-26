package com.dmoniak.patches.altosadventure

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.AccessFlags
import com.dmoniak.patches.hungryshark.util.findMutableMethodOf
import com.dmoniak.patches.shared.Constants.COMPATIBILITY_ALTOS_ADVENTURE
import java.util.logging.Logger

@Suppress("unused")
val altosAdventureBypassAdsPatch = bytecodePatch(
    name = "Bypass Rewarded Ads - Alto's Adventure (Experimental)",
    description = "⚠️ [En cours de développement / Non testé] Bypasses video ads in Alto's Adventure for free crash revives, free coin doublers at run end, and removes intrusive interstitial popups.",
) {
    compatibleWith(COMPATIBILITY_ALTOS_ADVENTURE)

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        executeAltosAdventureBypassAdsLogic(logger)
    }
}

fun BytecodePatchContext.executeAltosAdventureBypassAdsLogic(logger: Logger) {
    logger.info("Executing Bypass Rewarded Ads patch for Alto's Adventure...")
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

            // 1. Force rewarded video ad completion flags for revives and coin doubling
            if (!isStatic && (
                mName == "isrewardedadready" ||
                mName == "isvideorewardavailable" ||
                mName == "haswatchedrevivead" ||
                mName == "canrevivewithad" ||
                mName == "candoublecoinswithad"
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
                    logger.info("[Alto Ads] Granted rewarded ad event: ${classDef.type}->${method.name}")
                } catch (e: Exception) {
                    logger.warning("[Alto Ads] Failed to hook ${method.name}: ${e.message}")
                }
            }

            // 2. Block interstitial / crash ad popups
            if (!isStatic && (
                mName == "shouldshowinterstitial" ||
                mName == "canplayinterstitial" ||
                mName == "isinterstitialready"
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
                    logger.info("[Alto Ads] Blocked interstitial: ${classDef.type}->${method.name}")
                } catch (e: Exception) {
                    logger.warning("[Alto Ads] Failed to hook ${method.name}: ${e.message}")
                }
            }
        }
    }
    logger.info("[Alto Ads] Total ad bypass hooks applied: $hookedPoints")
}

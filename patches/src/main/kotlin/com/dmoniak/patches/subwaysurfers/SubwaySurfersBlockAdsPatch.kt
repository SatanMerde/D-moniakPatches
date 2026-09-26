package com.dmoniak.patches.subwaysurfers

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.AccessFlags
import com.dmoniak.patches.hungryshark.util.findMutableMethodOf
import com.dmoniak.patches.shared.Constants.COMPATIBILITY_SUBWAY_SURFERS
import java.util.logging.Logger

@Suppress("unused")
val subwaySurfersBlockAdsPatch = bytecodePatch(
    name = "Block Ads & Promotional Popups - Subway Surfers (Experimental)",
    description = "⚠️ [En cours de développement / Non testé] Strips interstitial video ads, post-run banners, forced video revives, and promotional popup screens in Subway Surfers.",
) {
    compatibleWith(COMPATIBILITY_SUBWAY_SURFERS)

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        executeSubwaySurfersBlockAdsLogic(logger)
    }
}

fun BytecodePatchContext.executeSubwaySurfersBlockAdsLogic(logger: Logger) {
    logger.info("Executing Block Ads & Promotional Popups patch for Subway Surfers...")
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

            // 1. Neutralize ad availability and should-show getters
            if (!isStatic && (
                mName == "isinterstitialloaded" ||
                mName == "isadready" ||
                mName == "hasvideoad" ||
                mName == "shouldshowinterstitial" ||
                mName == "shouldshowpostrunad" ||
                mName == "isbanneradloaded"
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
                    logger.info("[SubwaySurfers Ads] Neutralized ad check in: ${classDef.type}->${method.name}")
                } catch (e: Exception) {
                    logger.warning("[SubwaySurfers Ads] Failed to hook ${method.name}: ${e.message}")
                }
            }

            // 2. Neutralize ad presentation / show methods
            if (!isStatic && (
                mName == "showinterstitialad" ||
                mName == "showpostrunad" ||
                mName == "displaybanner" ||
                mName == "showforcedvideo" ||
                mName == "requestinterstitial"
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
                    logger.info("[SubwaySurfers Ads] Neutralized ad presentation in: ${classDef.type}->${method.name}")
                } catch (e: Exception) {
                    logger.warning("[SubwaySurfers Ads] Failed to hook ${method.name}: ${e.message}")
                }
            }
        }
    }
    logger.info("[SubwaySurfers Ads] Total ad-blocking hooks applied: $hookedPoints")
}

package com.dmoniak.patches.pvz

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.AccessFlags
import com.dmoniak.patches.hungryshark.util.findMutableMethodOf
import com.dmoniak.patches.shared.Constants.COMPATIBILITY_PLANTS_VS_ZOMBIES
import java.util.logging.Logger

@Suppress("unused")
val pvzBlockAdsDeclutterPatch = bytecodePatch(
    name = "Block Interstitials & EA Promo Ads - Plants vs. Zombies (Experimental)",
    description = "⚠️ [En cours de développement / Non testé] Strips interstitial video ads between levels, bottom banners, and EA promotional popups across Plants vs. Zombies.",
) {
    compatibleWith(COMPATIBILITY_PLANTS_VS_ZOMBIES)

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        executePvZBlockAdsLogic(logger)
    }
}

fun BytecodePatchContext.executePvZBlockAdsLogic(logger: Logger) {
    logger.info("Executing Block Interstitials & EA Promo Ads patch for Plants vs. Zombies...")
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
                mName == "isinterstitialready" ||
                mName == "shouldshowad" ||
                mName == "isadavailable" ||
                mName == "hasvideoad"
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
                    logger.info("[PvZ Ads] Blocked ad check in: ${classDef.type}->${method.name}")
                } catch (e: Exception) {
                    logger.warning("[PvZ Ads] Failed to hook ${method.name}: ${e.message}")
                }
            }

            if (!isStatic && (
                mName == "showinterstitial" ||
                mName == "showvideoad" ||
                mName == "displaybanner"
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
                    logger.info("[PvZ Ads] Neutralized ad display in: ${classDef.type}->${method.name}")
                } catch (e: Exception) {
                    logger.warning("[PvZ Ads] Failed to hook ${method.name}: ${e.message}")
                }
            }
        }
    }
    logger.info("[PvZ Ads] Total ad-blocking hooks applied: $hookedPoints")
}

package com.dmoniak.patches.angrybirds

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.AccessFlags
import com.dmoniak.patches.hungryshark.util.findMutableMethodOf
import com.dmoniak.patches.shared.Constants.COMPATIBILITY_ANGRY_BIRDS
import java.util.logging.Logger

@Suppress("unused")
val angryBirdsBlockAdsPatch = bytecodePatch(
    name = "Block Ads & Video Promos - Angry Birds Classic (Experimental)",
    description = "⚠️ [En cours de développement / Non testé] Strips interstitial ads, pause-screen banners, and promotional popups in Angry Birds Classic.",
) {
    compatibleWith(COMPATIBILITY_ANGRY_BIRDS)

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        executeAngryBirdsBlockAdsLogic(logger)
    }
}

fun BytecodePatchContext.executeAngryBirdsBlockAdsLogic(logger: Logger) {
    logger.info("Executing Block Ads patch for Angry Birds Classic...")
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
                mName == "hasvideoad" ||
                mName == "isinterstitialready" ||
                mName == "isbannerloaded"
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
                    logger.info("[AngryBirds Ads] Neutralized ad check in: ${classDef.type}->${method.name}")
                } catch (e: Exception) {
                    logger.warning("[AngryBirds Ads] Failed to hook ${method.name}: ${e.message}")
                }
            }

            if (!isStatic && (
                mName == "showinterstitial" ||
                mName == "showvideoad" ||
                mName == "showfullscreenad" ||
                mName == "showbanner"
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
                    logger.info("[AngryBirds Ads] Neutralized ad display in: ${classDef.type}->${method.name}")
                } catch (e: Exception) {
                    logger.warning("[AngryBirds Ads] Failed to hook ${method.name}: ${e.message}")
                }
            }
        }
    }
    logger.info("[AngryBirds Ads] Total ad-blocking hooks applied: $hookedPoints")
}

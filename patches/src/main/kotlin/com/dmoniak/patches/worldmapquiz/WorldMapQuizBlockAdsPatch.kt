package com.dmoniak.patches.worldmapquiz

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.AccessFlags
import com.dmoniak.patches.hungryshark.util.findMutableMethodOf
import com.dmoniak.patches.shared.Constants.COMPATIBILITY_WORLD_MAP_QUIZ
import java.util.logging.Logger

@Suppress("unused")
val worldMapQuizBlockAdsPatch = bytecodePatch(
    name = "Block Ads & Video Interruptions - World Map Quiz (Experimental)",
    description = "⚠️ [En cours de développement / Non testé] Strips interstitial ads between rounds, banner ads, and video prompts in World Map Quiz.",
) {
    compatibleWith(COMPATIBILITY_WORLD_MAP_QUIZ)

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        executeWorldMapQuizBlockAdsLogic(logger)
    }
}

fun BytecodePatchContext.executeWorldMapQuizBlockAdsLogic(logger: Logger) {
    logger.info("Executing Block Ads patch for World Map Quiz...")
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
                    logger.info("[WorldMapQuiz Ads] Neutralized ad check in: ${classDef.type}->${method.name}")
                } catch (e: Exception) {
                    logger.warning("[WorldMapQuiz Ads] Failed to hook ${method.name}: ${e.message}")
                }
            }

            if (!isStatic && (
                mName == "showinterstitial" ||
                mName == "showad" ||
                mName == "showvideo" ||
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
                    logger.info("[WorldMapQuiz Ads] Neutralized ad show in: ${classDef.type}->${method.name}")
                } catch (e: Exception) {
                    logger.warning("[WorldMapQuiz Ads] Failed to hook ${method.name}: ${e.message}")
                }
            }
        }
    }
    logger.info("[WorldMapQuiz Ads] Total ad-blocking hooks applied: $hookedPoints")
}

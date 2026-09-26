package com.dmoniak.patches.doodlejump

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.AccessFlags
import com.dmoniak.patches.hungryshark.util.findMutableMethodOf
import com.dmoniak.patches.shared.Constants.COMPATIBILITY_DOODLE_JUMP
import java.util.logging.Logger

@Suppress("unused")
val doodleJumpBlockAdsPatch = bytecodePatch(
    name = "Block Ads & Game-Over Popups - Doodle Jump (Experimental)",
    description = "⚠️ [En cours de développement / Non testé] Strips post-game interstitial ads, bottom banner advertisements, and sponsored promotions in Doodle Jump.",
) {
    compatibleWith(COMPATIBILITY_DOODLE_JUMP)

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        executeDoodleJumpBlockAdsLogic(logger)
    }
}

fun BytecodePatchContext.executeDoodleJumpBlockAdsLogic(logger: Logger) {
    logger.info("Executing Block Ads patch for Doodle Jump...")
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
                    logger.info("[DoodleJump Ads] Neutralized ad check in: ${classDef.type}->${method.name}")
                } catch (e: Exception) {
                    logger.warning("[DoodleJump Ads] Failed to hook ${method.name}: ${e.message}")
                }
            }

            if (!isStatic && (
                mName == "showinterstitial" ||
                mName == "showad" ||
                mName == "showbanner" ||
                mName == "showfullscreenad"
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
                    logger.info("[DoodleJump Ads] Neutralized ad show in: ${classDef.type}->${method.name}")
                } catch (e: Exception) {
                    logger.warning("[DoodleJump Ads] Failed to hook ${method.name}: ${e.message}")
                }
            }
        }
    }
    logger.info("[DoodleJump Ads] Total ad-blocking hooks applied: $hookedPoints")
}

package com.dmoniak.patches.crossyroad

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.AccessFlags
import com.dmoniak.patches.hungryshark.util.findMutableMethodOf
import com.dmoniak.patches.shared.Constants.COMPATIBILITY_CROSSY_ROAD
import java.util.logging.Logger

@Suppress("unused")
val crossyRoadBlockAdsPatch = bytecodePatch(
    name = "Block Ads & Video Promos - Crossy Road (Experimental)",
    description = "⚠️ [En cours de développement / Non testé] Strips video advertisements, post-death revival popups, and banner ads in Crossy Road.",
) {
    compatibleWith(COMPATIBILITY_CROSSY_ROAD)

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        executeCrossyRoadBlockAdsLogic(logger)
    }
}

fun BytecodePatchContext.executeCrossyRoadBlockAdsLogic(logger: Logger) {
    logger.info("Executing Block Ads patch for Crossy Road...")
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
                mName == "isrewardedadready"
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
                    logger.info("[CrossyRoad Ads] Neutralized ad check in: ${classDef.type}->${method.name}")
                } catch (e: Exception) {
                    logger.warning("[CrossyRoad Ads] Failed to hook ${method.name}: ${e.message}")
                }
            }

            if (!isStatic && (
                mName == "showinterstitial" ||
                mName == "showvideo" ||
                mName == "showrewardedvideo" ||
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
                    logger.info("[CrossyRoad Ads] Neutralized ad display in: ${classDef.type}->${method.name}")
                } catch (e: Exception) {
                    logger.warning("[CrossyRoad Ads] Failed to hook ${method.name}: ${e.message}")
                }
            }
        }
    }
    logger.info("[CrossyRoad Ads] Total ad-blocking hooks applied: $hookedPoints")
}

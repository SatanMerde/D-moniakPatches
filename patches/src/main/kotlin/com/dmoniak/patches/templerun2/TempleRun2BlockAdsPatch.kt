package com.dmoniak.patches.templerun2

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.AccessFlags
import com.dmoniak.patches.hungryshark.util.findMutableMethodOf
import com.dmoniak.patches.shared.Constants.COMPATIBILITY_TEMPLE_RUN_2
import java.util.logging.Logger

@Suppress("unused")
val templeRun2BlockAdsPatch = bytecodePatch(
    name = "Block Ads & Death Interstitials - Temple Run 2 (Experimental)",
    description = "⚠️ [En cours de développement / Non testé] Eliminates post-death full-screen ads, video revive popups, and banner ads in Temple Run 2.",
) {
    compatibleWith(COMPATIBILITY_TEMPLE_RUN_2)

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        executeTempleRun2BlockAdsLogic(logger)
    }
}

fun BytecodePatchContext.executeTempleRun2BlockAdsLogic(logger: Logger) {
    logger.info("Executing Block Ads & Death Interstitials patch for Temple Run 2...")
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
                mName == "shouldshowpostdeathad" ||
                mName == "isvideoready" ||
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
                    logger.info("[TempleRun2 Ads] Neutralized ad check in: ${classDef.type}->${method.name}")
                } catch (e: Exception) {
                    logger.warning("[TempleRun2 Ads] Failed to hook ${method.name}: ${e.message}")
                }
            }

            if (!isStatic && (
                mName == "showinterstitial" ||
                mName == "showdeathad" ||
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
                    logger.info("[TempleRun2 Ads] Neutralized ad presentation in: ${classDef.type}->${method.name}")
                } catch (e: Exception) {
                    logger.warning("[TempleRun2 Ads] Failed to hook ${method.name}: ${e.message}")
                }
            }
        }
    }
    logger.info("[TempleRun2 Ads] Total ad-blocking hooks applied: $hookedPoints")
}

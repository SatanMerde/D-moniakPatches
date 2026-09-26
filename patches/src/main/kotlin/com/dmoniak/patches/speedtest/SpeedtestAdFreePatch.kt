package com.dmoniak.patches.speedtest

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.AccessFlags
import com.dmoniak.patches.hungryshark.util.findMutableMethodOf
import com.dmoniak.patches.shared.Constants.COMPATIBILITY_SPEEDTEST
import java.util.logging.Logger

@Suppress("unused")
val speedtestAdFreePatch = bytecodePatch(
    name = "Ad-Free Speedtest - Speedtest by Ookla (Experimental)",
    description = "⚠️ [En cours de développement / Non testé] Removes banner ads, video survey ads, and post-speedtest sponsored surveys and promotional cards from Speedtest by Ookla.",
) {
    compatibleWith(COMPATIBILITY_SPEEDTEST)

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        executeSpeedtestAdFreeLogic(logger)
    }
}

fun BytecodePatchContext.executeSpeedtestAdFreeLogic(logger: Logger) {
    logger.info("Executing Ad-Free patch for Speedtest by Ookla...")
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

            // 1. Hook hasAds / isAdEnabled / isAdFree status
            if (!isStatic && (
                mName == "isadfree" ||
                mName == "hasremovedads" ||
                mName == "isadfreepurchased"
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
                    logger.info("[Speedtest Ads] Enforced ad-free status in: ${classDef.type}->${method.name}")
                } catch (e: Exception) {
                    logger.warning("[Speedtest Ads] Failed to hook ${method.name}: ${e.message}")
                }
            }

            // 2. Disable ad view loaders and banner requests
            if (!isStatic && (
                mName == "shouldshowads" ||
                mName == "hasads" ||
                mName == "isadsvisible" ||
                mName == "canloadad"
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
                    logger.info("[Speedtest Ads] Disabled ad display in: ${classDef.type}->${method.name}")
                } catch (e: Exception) {
                    logger.warning("[Speedtest Ads] Failed to hook ${method.name}: ${e.message}")
                }
            }
        }
    }
    logger.info("[Speedtest Ads] Total ad-free hooks applied: $hookedPoints")
}

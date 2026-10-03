package com.dmoniak.patches.undercover

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.AccessFlags
import com.dmoniak.patches.hungryshark.util.findMutableMethodOf
import com.dmoniak.patches.shared.Constants.COMPATIBILITY_UNDERCOVER
import java.util.logging.Logger

@Suppress("unused")
val undercoverBlockAdsPatch = bytecodePatch(
    name = "Block Ads & Commercials - Undercover (Experimental)",
    description = "⚠️ [En cours de développement / Non testé] Strips interstitial video ads between rounds, banner ads in lobby and voting screens, and rewarded video gates by neutralizing ad SDK calls.",
) {
    compatibleWith(COMPATIBILITY_UNDERCOVER)

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        executeUndercoverBlockAdsLogic(logger)
    }
}

fun BytecodePatchContext.executeUndercoverBlockAdsLogic(logger: Logger) {
    logger.info("Executing Block Ads & Commercials patch for Undercover...")
    var hookedCount = 0

    classDefForEach { classDef ->
        val tl = classDef.type.lowercase()
        if (tl.contains("androidx") || tl.contains("android/support") || tl.contains("google")) return@classDefForEach

        val mutableClass by lazy { mutableClassDefBy(classDef) }

        for (method in classDef.methods.toList()) {
            if (method.implementation == null) continue
            val isStatic = AccessFlags.STATIC.isSet(method.accessFlags)
            val mName = method.name.lowercase()
            val retType = method.returnType

            // Neutralize boolean checks for ads (isAdLoaded, shouldShowInterstitial, etc.)
            if (!isStatic && (
                mName == "isadloaded" ||
                mName == "isinterstitialready" ||
                mName == "shouldshowad" ||
                mName == "canshowinterstitial" ||
                mName == "isrewardedvideoready" ||
                mName == "hasinterstitialad"
            ) && retType == "Z") {
                mutableClass.findMutableMethodOf(method)?.let { mutableMethod ->
                    mutableMethod.addInstructions(
                        0,
                        """
                        const/4 v0, 0x0
                        return v0
                        """.trimIndent()
                    )
                    hookedCount++
                }
            }

            // Neutralize ad display triggers (showInterstitial, displayAd, etc.)
            if (!isStatic && (
                mName == "showinterstitial" ||
                mName == "showinterstitialad" ||
                mName == "displayad" ||
                mName == "showbanner" ||
                mName == "showbannerad"
            ) && retType == "V") {
                mutableClass.findMutableMethodOf(method)?.let { mutableMethod ->
                    mutableMethod.addInstructions(
                        0,
                        """
                        return-void
                        """.trimIndent()
                    )
                    hookedCount++
                }
            }
        }
    }

    logger.info("Hooked $hookedCount ad call points in Undercover.")
}

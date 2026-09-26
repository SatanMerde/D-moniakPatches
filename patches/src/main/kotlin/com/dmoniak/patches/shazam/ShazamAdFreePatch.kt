package com.dmoniak.patches.shazam

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.AccessFlags
import com.dmoniak.patches.hungryshark.util.findMutableMethodOf
import com.dmoniak.patches.shared.Constants.COMPATIBILITY_SHAZAM
import java.util.logging.Logger

@Suppress("unused")
val shazamAdFreePatch = bytecodePatch(
    name = "Ad-Free & Clean UI - Shazam (Experimental)",
    description = "⚠️ [En cours de développement / Non testé] Removes banner ads, sponsored artist recommendations, and Apple Music promotional upsell popups in Shazam.",
) {
    compatibleWith(COMPATIBILITY_SHAZAM)

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        executeShazamAdFreeLogic(logger)
    }
}

fun BytecodePatchContext.executeShazamAdFreeLogic(logger: Logger) {
    logger.info("Executing Ad-Free & Clean UI patch for Shazam...")
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

            // 1. Disable ads & promotional card visibility
            if (!isStatic && (
                mName == "isadvisible" ||
                mName == "shouldshowad" ||
                mName == "ispromotedtrack" ||
                mName == "isapplemusicupsellvisible" ||
                mName == "hassponsoredcontent" ||
                mName == "shouldshowupsellbanner"
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
                    logger.info("[Shazam AdFree] Disabled ad/promotional item: ${classDef.type}->${method.name}")
                } catch (e: Exception) {
                    logger.warning("[Shazam AdFree] Failed to hook ${method.name}: ${e.message}")
                }
            }
        }
    }
    logger.info("[Shazam AdFree] Total ad-free hooks applied: $hookedPoints")
}

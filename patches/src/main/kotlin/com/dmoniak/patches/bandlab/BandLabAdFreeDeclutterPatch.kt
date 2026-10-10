package com.dmoniak.patches.bandlab

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.AccessFlags
import com.dmoniak.patches.hungryshark.util.findMutableMethodOf
import com.dmoniak.patches.shared.Constants.COMPATIBILITY_BANDLAB
import java.util.logging.Logger

@Suppress("unused")
val bandLabAdFreeDeclutterPatch = bytecodePatch(
    name = "Block Ads & Declutter Feed - BandLab",
    description = "Removes in-app promotional banners, upgrade prompts, sponsored artist cards, and interstitial ads across the BandLab home feed and studio screens.",
) {
    compatibleWith(COMPATIBILITY_BANDLAB)

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        executeBandLabAdFreeDeclutterLogic(logger)
    }
}

fun BytecodePatchContext.executeBandLabAdFreeDeclutterLogic(logger: Logger) {
    logger.info("Executing Block Ads & Declutter Feed patch for BandLab...")
    var hookedMethods = 0

    classDefForEach { classDef ->
        val type = classDef.type
        val tl = type.lowercase()

        // Skip framework internals
        if (
            tl.startsWith("landroid/") ||
            tl.startsWith("lkotlin/") ||
            tl.startsWith("ljava/")
        ) return@classDefForEach

        val mutableClass by lazy { mutableClassDefBy(classDef) }

        // 1. Hook ad visibility and promo banner checks
        for (method in classDef.methods.toList()) {
            if (method.implementation == null) continue
            val isStatic = AccessFlags.STATIC.isSet(method.accessFlags)
            val mName = method.name.lowercase()
            val retType = method.returnType

            if (!isStatic && (
                mName == "isadvisible" ||
                mName == "shouldshowad" ||
                mName == "shouldshowpromo" ||
                mName == "ispromotionalcard" ||
                mName == "issponsoredpost" ||
                mName == "shoulddisplayupgradebanner"
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
                    hookedMethods++
                    logger.info("[BandLab AdFree] Disabled promo/ad check in: ${classDef.type}->${method.name}")
                } catch (e: Exception) {
                    logger.warning("[BandLab AdFree] Failed to hook ${method.name}: ${e.message}")
                }
            }
        }
    }

    logger.info("[BandLab AdFree] Total hooks applied: $hookedMethods")
}

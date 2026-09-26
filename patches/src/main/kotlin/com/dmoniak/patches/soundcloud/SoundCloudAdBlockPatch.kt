package com.dmoniak.patches.soundcloud

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.AccessFlags
import com.dmoniak.patches.hungryshark.util.findMutableMethodOf
import com.dmoniak.patches.shared.Constants.COMPATIBILITY_SOUNDCLOUD
import java.util.logging.Logger

@Suppress("unused")
val soundCloudAdBlockPatch = bytecodePatch(
    name = "Block Audio & Stream Ads - SoundCloud (Experimental)",
    description = "⚠️ [En cours de développement / Non testé] Bypasses interstitial audio ads between music tracks and removes sponsored promoted tracks from the SoundCloud audio stream.",
) {
    compatibleWith(COMPATIBILITY_SOUNDCLOUD)

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        executeSoundCloudAdBlockLogic(logger)
    }
}

fun BytecodePatchContext.executeSoundCloudAdBlockLogic(logger: Logger) {
    logger.info("Executing Block Audio & Stream Ads patch for SoundCloud...")
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

            // 1. Audio ad & promoted track verification
            if (!isStatic && (
                mName == "isaudioadplaying" ||
                mName == "shouldplayaudioad" ||
                mName == "isadtrack" ||
                mName == "ispromotedtrack" ||
                mName == "issponsoredtrack" ||
                mName == "hasinterstitialad" ||
                mName == "isadeventpending"
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
                    logger.info("[SoundCloud Ads] Blocked audio ad / promoted track: ${classDef.type}->${method.name}")
                } catch (e: Exception) {
                    logger.warning("[SoundCloud Ads] Failed to hook ${method.name}: ${e.message}")
                }
            }

            // 2. Void ad playback execution hook
            if (!isStatic && (
                mName == "playaudioad" ||
                mName == "triggerinterstitialad" ||
                mName == "loadmidrollad"
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
                    logger.info("[SoundCloud Ads] Neutralized audio ad trigger: ${classDef.type}->${method.name}")
                } catch (e: Exception) {
                    logger.warning("[SoundCloud Ads] Failed to hook ${method.name}: ${e.message}")
                }
            }
        }
    }
    logger.info("[SoundCloud Ads] Total audio ad prevention hooks applied: $hookedPoints")
}

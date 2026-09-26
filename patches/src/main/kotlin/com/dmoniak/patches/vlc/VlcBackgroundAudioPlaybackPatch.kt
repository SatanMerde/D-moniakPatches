package com.dmoniak.patches.vlc

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.AccessFlags
import com.dmoniak.patches.hungryshark.util.findMutableMethodOf
import com.dmoniak.patches.shared.Constants.COMPATIBILITY_VLC
import java.util.logging.Logger

@Suppress("unused")
val vlcBackgroundAudioPlaybackPatch = bytecodePatch(
    name = "Force Background Audio Playback - VLC (Experimental)",
    description = "⚠️ [En cours de développement / Non testé] Forces audio playback to stay active in the background when exiting the video player, minimizing the app, or locking the device.",
) {
    compatibleWith(COMPATIBILITY_VLC)

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        executeVlcBackgroundAudioPlaybackLogic(logger)
    }
}

fun BytecodePatchContext.executeVlcBackgroundAudioPlaybackLogic(logger: Logger) {
    logger.info("Executing Force Background Audio Playback patch for VLC...")
    var hookedPoints = 0

    classDefForEach { classDef ->
        val tl = classDef.type.lowercase()
        if (tl.contains("androidx") || tl.contains("android/support")) return@classDefForEach

        val mutableClass by lazy { mutableClassDefBy(classDef) }

        for (method in classDef.methods.toList()) {
            if (method.implementation == null) continue
            val isStatic = AccessFlags.STATIC.isSet(method.accessFlags)
            val mName = method.name.lowercase()
            val retType = method.returnType

            // Force background audio playback
            if (!isStatic && (
                mName == "isbackgroundplaybackallowed" ||
                mName == "canplayinbackground" ||
                mName == "shouldswitchestoaudioinbackground" ||
                mName == "isplayonaudiomodeenabled"
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
                    logger.info("[VLC Background] Forced background audio mode in: ${classDef.type}->${method.name}")
                } catch (e: Exception) {
                    logger.warning("[VLC Background] Failed to hook ${method.name}: ${e.message}")
                }
            }

            // Suppress stop on background
            if (!isStatic && (
                mName == "stoponbackground" ||
                mName == "suspendplaybackonexit" ||
                mName == "releaseplaybacksession"
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
                    logger.info("[VLC Background] Neutralized stop method in: ${classDef.type}->${method.name}")
                } catch (e: Exception) {
                    logger.warning("[VLC Background] Failed to hook stop method: ${e.message}")
                }
            }
        }
    }

    logger.info("Force Background Audio Playback for VLC executed: $hookedPoints points hooked.")
}

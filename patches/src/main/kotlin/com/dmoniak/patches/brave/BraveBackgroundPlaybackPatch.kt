package com.dmoniak.patches.brave

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.AccessFlags
import com.dmoniak.patches.hungryshark.util.findMutableMethodOf
import com.dmoniak.patches.shared.Constants.COMPATIBILITY_BRAVE
import java.util.logging.Logger

@Suppress("unused")
val braveBackgroundPlaybackPatch = bytecodePatch(
    name = "Background Playback - Brave (Experimental)",
    description = "⚠️ [En cours de développement / Non testé] Enables background audio and video playback when switching apps or locking the screen in Brave Browser.",
) {
    compatibleWith(COMPATIBILITY_BRAVE)

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        executeBraveBackgroundPlaybackLogic(logger)
    }
}

fun BytecodePatchContext.executeBraveBackgroundPlaybackLogic(logger: Logger) {
    logger.info("Executing Background Playback patch for Brave...")
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

            // Hook background media playback checks in Chromium / Brave audio focus
            if (!isStatic && (
                mName == "isbackgroundplaybackenabled" ||
                mName == "canplayinbackground" ||
                mName == "isplaywhenhiddenallowed" ||
                mName == "allowbackgroundaudio" ||
                mName == "isbackgroundaudioplaybackenabled"
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
                    logger.info("[Brave Playback] Enabled background playback in: ${classDef.type}->${method.name}")
                } catch (e: Exception) {
                    logger.warning("[Brave Playback] Failed to hook ${method.name}: ${e.message}")
                }
            }

            // Bypass pause on lock / hidden tab
            if (!isStatic && (
                mName == "pauseonbackground" ||
                mName == "suspendmediawhenhidden" ||
                mName == "stopplaybackonhidden"
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
                    logger.info("[Brave Playback] Neutralized pause-on-background in: ${classDef.type}->${method.name}")
                } catch (e: Exception) {
                    logger.warning("[Brave Playback] Failed to hook pause method: ${e.message}")
                }
            }
        }
    }

    logger.info("Background Playback for Brave executed successfully: $hookedPoints points hooked.")
}

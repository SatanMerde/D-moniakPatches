package com.dmoniak.patches.spotify

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.AccessFlags
import com.dmoniak.patches.hungryshark.util.findMutableMethodOf
import com.dmoniak.patches.shared.Constants.COMPATIBILITY_SPOTIFY
import java.util.logging.Logger

@Suppress("unused")
val spotifyAccentColorPatch = bytecodePatch(
    name = "Spicetify Custom Accent Color - Spotify (Experimental)",
    description = "⚠️ [En cours de développement / Non testé] Replaces the default Spotify green (#1DB954) with custom theme accents (Purple, Cyan, Crimson, Gold) resilient across weekly updates.",
) {
    compatibleWith(COMPATIBILITY_SPOTIFY)

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        executeSpotifyAccentColorLogic(logger)
    }
}

fun BytecodePatchContext.executeSpotifyAccentColorLogic(logger: Logger) {
    logger.info("Executing Spicetify Custom Accent Color patch for Spotify...")
    var accentCount = 0

    // Custom Spicetify Accent: Electric Violet / Cyberpunk Neon Purple (#8A2BE2 = 0xFF8A2BE2)
    // Dalvik signed 32-bit int: -7722014

    classDefForEach { classDef ->
        val tl = classDef.type.lowercase()
        if (tl.contains("androidx") || tl.contains("android/support") || tl.contains("com/google")) return@classDefForEach

        val mutableClass by lazy { mutableClassDefBy(classDef) }

        for (method in classDef.methods.toList()) {
            if (method.implementation == null) continue
            val mName = method.name.lowercase()
            val retType = method.returnType
            val pTypes = method.parameterTypes

            // 1. Hook methods with explicit semantic name (unobfuscated builds)
            val nameMatch = (mName.contains("accentcolor") ||
                 mName.contains("brandcolor") ||
                 mName.contains("highlightcolor") ||
                 mName.contains("primarybrandcolor") ||
                 mName.contains("spotifygreen")) &&
                retType == "I" && pTypes.isEmpty()

            // 2. Obfuscation-resistant hook: inspect methods returning int that load Spotify green color constants
            // #1DB954 = -14829228 (0xFF1DB954) or 1948004 (0x1DB954)
            // #1ED760 = -14756000 (0xFF1ED760) or 2021216 (0x1ED760)
            var literalMatch = false
            if (retType == "I" && pTypes.isEmpty()) {
                val impl = method.implementation
                if (impl != null) {
                    for (insn in impl.instructions) {
                        if (insn is com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction) {
                            val lit = insn.narrowLiteral
                            if (lit == -14829228 || lit == -14756000 || lit == 1948004 || lit == 2021216) {
                                literalMatch = true
                                break
                            }
                        }
                    }
                }
            }

            if (nameMatch || literalMatch) {
                try {
                    val mutableMethod = mutableClass.findMutableMethodOf(method)
                    mutableMethod.addInstructions(
                        0,
                        """
                        const v0, -0x75d41e
                        return v0
                        """.trimIndent() // #8A2BE2 (Electric Purple)
                    )
                    accentCount++
                    logger.info("[Spotify Accent] Hooked brand color: ${classDef.type}->${method.name}")
                } catch (e: Exception) {
                    logger.warning("[Spotify Accent] Failed to hook ${method.name}: ${e.message}")
                }
            }
        }
    }
    logger.info("[Spotify Accent] Total patched accent color points: $accentCount")
}

package com.dmoniak.patches.spotify

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction21c
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction31c
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction31i
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction51l
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.WideLiteralInstruction
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableStringReference
import com.dmoniak.patches.hungryshark.util.findMutableMethodOf
import com.dmoniak.patches.shared.Constants.COMPATIBILITY_SPOTIFY
import java.util.logging.Logger

@Suppress("unused")
val spotifyAccentColorPatch = bytecodePatch(
    name = "Spicetify Custom Accent Color - Spotify (Experimental)",
    description = "⚠️ [En cours de développement / Non testé] Replaces Spotify brand green (#1DB954 / #1ED760) with custom Cyberpunk Electric Purple (#8A2BE2) across Jetpack Compose Encore design system (64-bit literals), obfuscated Dalvik bytecode, string tables, and color models.",
) {
    compatibleWith(COMPATIBILITY_SPOTIFY)

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        executeSpotifyAccentColorLogic(logger)
    }
}

fun BytecodePatchContext.executeSpotifyAccentColorLogic(logger: Logger) {
    logger.info("Executing Spicetify Custom Accent Color patch for Spotify...")
    var wideLiteralsReplaced = 0
    var narrowLiteralsReplaced = 0
    var stringsReplaced = 0
    var methodsHooked = 0

    // Electric Purple / Neon Violet (#8A2BE2)
    val purpleHex = "#8A2BE2"

    classDefForEach { classDef ->
        val tl = classDef.type.lowercase()
        if (tl.startsWith("landroid/") || tl.startsWith("lkotlin/") || tl.startsWith("ljava/")) return@classDefForEach

        val mutableClass by lazy { mutableClassDefBy(classDef) }

        for (method in classDef.methods.toList()) {
            val impl = method.implementation ?: continue
            val isStatic = AccessFlags.STATIC.isSet(method.accessFlags)
            val mName = method.name.lowercase()
            val retType = method.returnType

            val instructions = impl.instructions.toList()
            for ((index, instruction) in instructions.withIndex()) {
                // 1. Jetpack Compose Encore 64-bit color constants (const-wide)
                if (instruction is WideLiteralInstruction && instruction is OneRegisterInstruction) {
                    val lit = instruction.wideLiteral
                    val newWideVal = when (lit) {
                        0x00000000ff1ed760L, 0x00000000ff1db954L -> 0x00000000ff8a2be2L
                        -63479633854580224L, -58843912170668032L -> -33165275882618880L
                        else -> null
                    }
                    if (newWideVal != null) {
                        try {
                            val mutableMethod = mutableClass.findMutableMethodOf(method)
                            val newInsn = BuilderInstruction51l(Opcode.CONST_WIDE, instruction.registerA, newWideVal)
                            mutableMethod.replaceInstruction(index, newInsn)
                            wideLiteralsReplaced++
                            logger.fine("[Spotify Accent] Replaced 64-bit green Compose literal in ${classDef.type}->${method.name}")
                        } catch (e: Exception) {
                            logger.fine("[Spotify Accent] Skip wide literal replace: ${e.message}")
                        }
                    }
                }

                // 2. 32-bit Narrow literals (legacy Android Views, Canvas, ARGB ints)
                if (instruction is NarrowLiteralInstruction && instruction is OneRegisterInstruction) {
                    val lit = instruction.narrowLiteral
                    val newIntVal = when (lit) {
                        -14756000, -14829228 -> -7722014 // 0xFF8A2BE2
                        2021216, 1948004 -> 9055202 // 0x8A2BE2
                        else -> null
                    }
                    if (newIntVal != null) {
                        try {
                            val mutableMethod = mutableClass.findMutableMethodOf(method)
                            val newInsn = BuilderInstruction31i(Opcode.CONST, instruction.registerA, newIntVal)
                            mutableMethod.replaceInstruction(index, newInsn)
                            narrowLiteralsReplaced++
                            logger.fine("[Spotify Accent] Replaced 32-bit green literal in ${classDef.type}->${method.name}")
                        } catch (e: Exception) {
                            logger.fine("[Spotify Accent] Skip narrow literal replace: ${e.message}")
                        }
                    }
                }

                // 3. String hex constants (#1DB954, #1ED760)
                if (instruction.opcode == Opcode.CONST_STRING || instruction.opcode == Opcode.CONST_STRING_JUMBO) {
                    val strRef = (instruction as? ReferenceInstruction)?.reference as? StringReference ?: continue
                    val s = strRef.string.lowercase()
                    if (s == "#1db954" || s == "#1ed760" || s == "1db954" || s == "1ed760") {
                        val reg = (instruction as? OneRegisterInstruction)?.registerA ?: continue
                        val newInsn = if (instruction.opcode == Opcode.CONST_STRING_JUMBO) {
                            BuilderInstruction31c(Opcode.CONST_STRING_JUMBO, reg, ImmutableStringReference(purpleHex))
                        } else {
                            BuilderInstruction21c(Opcode.CONST_STRING, reg, ImmutableStringReference(purpleHex))
                        }
                        try {
                            val mutableMethod = mutableClass.findMutableMethodOf(method)
                            mutableMethod.replaceInstruction(index, newInsn)
                            stringsReplaced++
                            logger.fine("[Spotify Accent] Replaced green string in ${classDef.type}->${method.name}")
                        } catch (e: Exception) {
                            logger.fine("[Spotify Accent] Skip string replace: ${e.message}")
                        }
                    }
                }
            }

            // 4. Hook semantic color getter methods returning ARGB int or Compose Color Long
            val semanticMatch = mName.contains("accentcolor") ||
                    mName.contains("brandcolor") ||
                    mName.contains("primarybrandcolor") ||
                    mName.contains("spotifygreen")

            if (semanticMatch && method.parameterTypes.isEmpty()) {
                if (retType == "I") {
                    try {
                        val mutableMethod = mutableClass.findMutableMethodOf(method)
                        mutableMethod.addInstructions(
                            0,
                            """
                            const v0, -0x75d41e
                            return v0
                            """.trimIndent() // #8A2BE2
                        )
                        methodsHooked++
                        logger.info("[Spotify Accent] Hooked brand color method in ${classDef.type}->${method.name}")
                    } catch (e: Exception) {
                        logger.fine("[Spotify Accent] Failed to hook ${method.name}: ${e.message}")
                    }
                } else if (retType == "J") {
                    try {
                        val mutableMethod = mutableClass.findMutableMethodOf(method)
                        mutableMethod.addInstructions(
                            0,
                            """
                            const-wide v0, 0x00000000ff8a2be2L
                            return-wide v0
                            """.trimIndent()
                        )
                        methodsHooked++
                        logger.info("[Spotify Accent] Hooked Compose brand color method in ${classDef.type}->${method.name}")
                    } catch (e: Exception) {
                        logger.fine("[Spotify Accent] Failed to hook Compose ${method.name}: ${e.message}")
                    }
                }
            }
        }
    }

    logger.info("[Spotify Accent] Finished: $wideLiteralsReplaced 64-bit Compose literals replaced, $narrowLiteralsReplaced 32-bit literals replaced, $stringsReplaced green hex strings redirected, $methodsHooked color methods hooked.")
}

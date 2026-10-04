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
val spotifyAmoledThemePatch = bytecodePatch(
    name = "Spicetify AMOLED Black Theme - Spotify (Experimental)",
    description = "⚠️ [En cours de développement / Non testé] Implements an OLED True Black (#000000) theme for Spotify Mobile, replacing dark-grey backgrounds across Jetpack Compose Encore design system (64-bit literals), obfuscated Dalvik bytecode, and string tables for maximum contrast and battery savings.",
) {
    compatibleWith(COMPATIBILITY_SPOTIFY)

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        executeSpotifyAmoledThemeLogic(logger)
    }
}

fun BytecodePatchContext.executeSpotifyAmoledThemeLogic(logger: Logger) {
    logger.info("Executing Spicetify AMOLED Black Theme patch for Spotify...")
    var wideLiteralsReplaced = 0
    var narrowLiteralsReplaced = 0
    var stringsReplaced = 0
    var methodsHooked = 0

    val blackHex = "#000000"

    classDefForEach { classDef ->
        val type = classDef.type
        val tl = type.lowercase()
        if (tl.startsWith("landroid/") || tl.startsWith("lkotlin/") || tl.startsWith("ljava/")) return@classDefForEach

        val mutableClass by lazy { mutableClassDefBy(classDef) }

        for (method in classDef.methods.toList()) {
            val impl = method.implementation ?: continue
            val isStatic = AccessFlags.STATIC.isSet(method.accessFlags)
            val mName = method.name
            val mNameLower = mName.lowercase()
            val retType = method.returnType

            // 1. Scan and replace dark grey literals across instructions
            val instructions = impl.instructions.toList()
            for ((index, instruction) in instructions.withIndex()) {
                // A. Jetpack Compose 64-bit color constants (const-wide)
                if (instruction is WideLiteralInstruction && instruction is OneRegisterInstruction) {
                    val lit = instruction.wideLiteral
                    val newWideVal = when (lit) {
                        0x00000000ff121212L,
                        0x00000000ff181818L,
                        0x00000000ff282828L,
                        0x00000000ff242424L,
                        0x00000000ff191414L -> 0x00000000ff000000L
                        // Shifted representations (high 32 bits)
                        -67250682882752512L, // 0xff12121200000000L
                        -65561833500213248L, // 0xff18181800000000L
                        -62747084529319936L, // 0xff28282800000000L
                        -63872983790239744L, // 0xff24242400000000L
                        -65280358489948160L  // 0xff19141400000000L
                        -> -72057594037927936L // 0xff00000000000000L
                        else -> null
                    }
                    if (newWideVal != null) {
                        try {
                            val mutableMethod = mutableClass.findMutableMethodOf(method)
                            val newInsn = BuilderInstruction51l(Opcode.CONST_WIDE, instruction.registerA, newWideVal)
                            mutableMethod.replaceInstruction(index, newInsn)
                            wideLiteralsReplaced++
                            logger.fine("[Spotify AMOLED] Replaced 64-bit dark Compose literal in ${type}->${mName}")
                        } catch (e: Exception) {
                            logger.fine("[Spotify AMOLED] Skip wide literal replace: ${e.message}")
                        }
                    }
                }

                // B. 32-bit Narrow literals (legacy Android Views, Canvas, Drawables)
                if (instruction is NarrowLiteralInstruction && instruction is OneRegisterInstruction) {
                    val lit = instruction.narrowLiteral
                    val newIntVal = when (lit) {
                        -15592942, -15200232, -15133676, -14408668, -14145496 -> -16777216 // 0xFF000000
                        1184274, 1579032, 1643540, 2368548, 2631720 -> 0 // 24-bit 0x000000
                        else -> null
                    }
                    if (newIntVal != null) {
                        try {
                            val mutableMethod = mutableClass.findMutableMethodOf(method)
                            val newInsn = BuilderInstruction31i(Opcode.CONST, instruction.registerA, newIntVal)
                            mutableMethod.replaceInstruction(index, newInsn)
                            narrowLiteralsReplaced++
                            logger.fine("[Spotify AMOLED] Replaced 32-bit dark literal in ${type}->${mName}")
                        } catch (e: Exception) {
                            logger.fine("[Spotify AMOLED] Skip narrow literal replace: ${e.message}")
                        }
                    }
                }

                // C. Dark grey background hex strings
                if (instruction.opcode == Opcode.CONST_STRING || instruction.opcode == Opcode.CONST_STRING_JUMBO) {
                    val strRef = (instruction as? ReferenceInstruction)?.reference as? StringReference ?: continue
                    val s = strRef.string.lowercase()
                    if (s == "#121212" || s == "#181818" || s == "#191414" || s == "#242424" || s == "#282828" ||
                        s == "121212" || s == "181818" || s == "191414" || s == "242424" || s == "282828"
                    ) {
                        val reg = (instruction as? OneRegisterInstruction)?.registerA ?: continue
                        val newInsn = if (instruction.opcode == Opcode.CONST_STRING_JUMBO) {
                            BuilderInstruction31c(Opcode.CONST_STRING_JUMBO, reg, ImmutableStringReference(blackHex))
                        } else {
                            BuilderInstruction21c(Opcode.CONST_STRING, reg, ImmutableStringReference(blackHex))
                        }
                        try {
                            val mutableMethod = mutableClass.findMutableMethodOf(method)
                            mutableMethod.replaceInstruction(index, newInsn)
                            stringsReplaced++
                            logger.fine("[Spotify AMOLED] Replaced dark grey string in: ${type}->${mName}")
                        } catch (e: Exception) {
                            logger.fine("[Spotify AMOLED] Skip string replace: ${e.message}")
                        }
                    }
                }
            }

            // 2. Hook background color getter methods
            val semanticMatch = mNameLower.contains("backgroundcolor") ||
                    mNameLower.contains("surfacecolor") ||
                    mNameLower.contains("darkbackground") ||
                    mNameLower.contains("elevatedcolor")

            if (semanticMatch && method.parameterTypes.isEmpty()) {
                if (retType == "I") {
                    try {
                        val mutableMethod = mutableClass.findMutableMethodOf(method)
                        mutableMethod.addInstructions(
                            0,
                            """
                            const/high16 v0, -0x1000000
                            return v0
                            """.trimIndent() // Pure Black
                        )
                        methodsHooked++
                        logger.info("[Spotify AMOLED] Hooked background color method in: ${type}->${mName}")
                    } catch (e: Exception) {
                        logger.fine("[Spotify AMOLED] Failed to hook ${mName}: ${e.message}")
                    }
                } else if (retType == "J") {
                    try {
                        val mutableMethod = mutableClass.findMutableMethodOf(method)
                        mutableMethod.addInstructions(
                            0,
                            """
                            const-wide v0, 0x00000000ff000000L
                            return-wide v0
                            """.trimIndent()
                        )
                        methodsHooked++
                        logger.info("[Spotify AMOLED] Hooked Compose background color method in: ${type}->${mName}")
                    } catch (e: Exception) {
                        logger.fine("[Spotify AMOLED] Failed to hook Compose ${mName}: ${e.message}")
                    }
                }
            }

            // 3. Force dark theme
            if (!isStatic && (
                mNameLower == "isdarktheme" ||
                mNameLower == "isdarkmode" ||
                mNameLower == "isnightmodeactive"
            ) && retType == "Z" && method.parameterTypes.isEmpty()) {
                try {
                    val mutableMethod = mutableClass.findMutableMethodOf(method)
                    mutableMethod.addInstructions(
                        0,
                        """
                        const/4 v0, 0x1
                        return v0
                        """.trimIndent()
                    )
                    methodsHooked++
                    logger.info("[Spotify AMOLED] Enforced dark theme: ${type}->${mName}")
                } catch (e: Exception) {
                    logger.fine("[Spotify AMOLED] Failed to hook ${mName}: ${e.message}")
                }
            }
        }
    }

    logger.info("[Spotify AMOLED] Finished: $wideLiteralsReplaced 64-bit Compose literals replaced, $narrowLiteralsReplaced 32-bit literals replaced, $stringsReplaced grey strings redirected, $methodsHooked color/theme methods hooked.")
}

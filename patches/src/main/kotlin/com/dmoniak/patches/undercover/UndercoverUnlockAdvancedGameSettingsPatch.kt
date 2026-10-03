package com.dmoniak.patches.undercover

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import com.dmoniak.patches.hungryshark.util.findMutableMethodOf
import com.dmoniak.patches.shared.Constants.COMPATIBILITY_UNDERCOVER
import java.util.logging.Logger

@Suppress("unused")
val undercoverUnlockAdvancedGameSettingsPatch = bytecodePatch(
    name = "Unlock Advanced Game Settings - Undercover (Experimental)",
    description = "⚠️ [En cours de développement / Non testé] Unlocks advanced game configuration (custom player counts, exact role distribution sliders for Civilians, Undercover agents, and Mr. White, custom discussion timers, and voting rules).",
) {
    compatibleWith(COMPATIBILITY_UNDERCOVER)

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        executeUndercoverUnlockSettingsLogic(logger)
    }
}

fun BytecodePatchContext.executeUndercoverUnlockSettingsLogic(logger: Logger) {
    logger.info("Executing Unlock Advanced Game Settings patch for Undercover...")
    var hookedPoints = 0

    classDefForEach { classDef ->
        val type = classDef.type
        val tl = type.lowercase()
        if (tl.contains("androidx") || tl.contains("android/support") || tl.contains("google")) return@classDefForEach

        val mutableClass by lazy { mutableClassDefBy(classDef) }

        // 1. Hook InGamePlayersAndRolesActivity methods
        if (type.contains("InGamePlayersAndRolesActivity") || type.contains("GameSetActivity")) {
            for (method in classDef.methods.toList()) {
                val retType = method.returnType
                if (retType == "Z") {
                    try {
                        val mm = mutableClass.findMutableMethodOf(method)
                        mm?.addInstructions(
                            0,
                            """
                            const/4 v0, 0x1
                            return v0
                            """.trimIndent()
                        )
                        hookedPoints++
                    } catch (e: Exception) {
                        logger.fine("Failed to hook $type.${method.name}: ${e.message}")
                    }
                } else if (retType == "I" && method.name !in listOf("hashCode", "describeContents")) {
                    try {
                        val mm = mutableClass.findMutableMethodOf(method)
                        mm?.addInstructions(
                            0,
                            """
                            const/16 v0, 0x32
                            return v0
                            """.trimIndent()
                        )
                        hookedPoints++
                    } catch (e: Exception) {
                        logger.fine("Failed to hook $type.${method.name}: ${e.message}")
                    }
                }
            }
        }

        // 2. Hook any method referencing special roles flags
        for (method in classDef.methods.toList()) {
            val impl = method.implementation ?: continue
            val retType = method.returnType

            var referencesRoleFlag = false
            for (insn in impl.instructions) {
                if (insn is ReferenceInstruction && insn.reference is StringReference) {
                    val s = (insn.reference as StringReference).string
                    if (
                        s == "is_using_falafel_vendor" ||
                        s == "mr_white_can_start" ||
                        s == "online_create_game_premium_word_count" ||
                        s == "SETTING_MR_WHITE_CAN_START" ||
                        s == "SETTING_USE_FALAFEL_VENDOR" ||
                        s == "SETTING_USE_LOVERS"
                    ) {
                        referencesRoleFlag = true
                        break
                    }
                }
            }

            if (referencesRoleFlag) {
                if (retType == "Z") {
                    try {
                        val mm = mutableClass.findMutableMethodOf(method)
                        mm?.addInstructions(
                            0,
                            """
                            const/4 v0, 0x1
                            return v0
                            """.trimIndent()
                        )
                        hookedPoints++
                    } catch (e: Exception) {
                        logger.fine("Failed to hook $type.${method.name}: ${e.message}")
                    }
                } else if (retType == "I") {
                    try {
                        val mm = mutableClass.findMutableMethodOf(method)
                        mm?.addInstructions(
                            0,
                            """
                            const/16 v0, 0x32
                            return v0
                            """.trimIndent()
                        )
                        hookedPoints++
                    } catch (e: Exception) {
                        logger.fine("Failed to hook $type.${method.name}: ${e.message}")
                    }
                }
            }
        }
    }

    logger.info("Hooked $hookedPoints advanced game configuration points in Undercover.")
}

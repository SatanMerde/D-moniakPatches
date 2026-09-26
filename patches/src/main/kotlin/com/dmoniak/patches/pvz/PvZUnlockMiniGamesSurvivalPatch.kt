package com.dmoniak.patches.pvz

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.AccessFlags
import com.dmoniak.patches.hungryshark.util.findMutableMethodOf
import com.dmoniak.patches.shared.Constants.COMPATIBILITY_PLANTS_VS_ZOMBIES
import java.util.logging.Logger

@Suppress("unused")
val pvzUnlockMiniGamesSurvivalPatch = bytecodePatch(
    name = "Unlock Mini-Games, Puzzles & Survival - Plants vs. Zombies (Experimental)",
    description = "⚠️ [En cours de développement / Non testé] Instantly unlocks Mini-Games, I, Zombie, Vasebreaker puzzles, Survival mode, and Zen Garden in Plants vs. Zombies without completing Adventure mode.",
) {
    compatibleWith(COMPATIBILITY_PLANTS_VS_ZOMBIES)

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        executePvZUnlockMiniGamesSurvivalLogic(logger)
    }
}

fun BytecodePatchContext.executePvZUnlockMiniGamesSurvivalLogic(logger: Logger) {
    logger.info("Executing Unlock Mini-Games & Survival patch for Plants vs. Zombies...")
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

            if (!isStatic && (
                mName == "isminigamesunlocked" ||
                mName == "ispuzzleunlocked" ||
                mName == "issurvivalunlocked" ||
                mName == "iszengardenunlocked" ||
                mName == "isadventurecompleted" ||
                mName == "canplayminigame"
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
                    logger.info("[PvZ Unlocks] Unlocked game mode in: ${classDef.type}->${method.name}")
                } catch (e: Exception) {
                    logger.warning("[PvZ Unlocks] Failed to hook ${method.name}: ${e.message}")
                }
            }
        }
    }
    logger.info("[PvZ Unlocks] Total game mode unlock hooks applied: $hookedPoints")
}

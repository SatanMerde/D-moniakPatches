package com.dmoniak.patches.fruitninja

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.AccessFlags
import com.dmoniak.patches.hungryshark.util.findMutableMethodOf
import com.dmoniak.patches.shared.Constants.COMPATIBILITY_FRUIT_NINJA
import java.util.logging.Logger

@Suppress("unused")
val fruitNinjaUnlockBladesDojosPatch = bytecodePatch(
    name = "Unlock All Blades & Dojos - Fruit Ninja (Experimental)",
    description = "⚠️ [En cours de développement / Non testé] Unlocks all swords, mythical blades, custom dojos, and power-up accessories in Fruit Ninja.",
) {
    compatibleWith(COMPATIBILITY_FRUIT_NINJA)

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        executeFruitNinjaUnlockBladesDojosLogic(logger)
    }
}

fun BytecodePatchContext.executeFruitNinjaUnlockBladesDojosLogic(logger: Logger) {
    logger.info("Executing Unlock All Blades & Dojos patch for Fruit Ninja...")
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

            // 1. Blade & Dojo unlock status
            if (!isStatic && (
                mName == "isbladeunlocked" ||
                mName == "isdojounlocked" ||
                mName == "isitemunlocked" ||
                mName == "hasblade" ||
                mName == "hasdojo" ||
                mName == "isunlocked" ||
                mName == "canselectblade" ||
                mName == "canselectdojo"
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
                    logger.info("[FruitNinja Unlocks] Unlocked blade/dojo in: ${classDef.type}->${method.name}")
                } catch (e: Exception) {
                    logger.warning("[FruitNinja Unlocks] Failed to hook ${method.name}: ${e.message}")
                }
            }

            // 2. Starfruit quantity booster (for easy purchase of any remaining consumable)
            if (!isStatic && (
                mName == "getstarfruit" ||
                mName == "getstarfruitcount" ||
                mName == "getcarambolas"
            ) && retType == "I") {
                try {
                    val mutableMethod = mutableClass.findMutableMethodOf(method)
                    mutableMethod.addInstructions(
                        0,
                        """
                        const v0, 0x000f423f
                        return v0
                        """.trimIndent()
                    )
                    hookedPoints++
                    logger.info("[FruitNinja Unlocks] Boosted starfruit in: ${classDef.type}->${method.name}")
                } catch (e: Exception) {
                    logger.warning("[FruitNinja Unlocks] Failed to hook ${method.name}: ${e.message}")
                }
            }
        }
    }
    logger.info("[FruitNinja Unlocks] Total unlock hooks applied: $hookedPoints")
}

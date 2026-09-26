package com.dmoniak.patches.dantheman

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.AccessFlags
import com.dmoniak.patches.hungryshark.util.findMutableMethodOf
import com.dmoniak.patches.shared.Constants.COMPATIBILITY_DAN_THE_MAN
import java.util.logging.Logger

@Suppress("unused")
val danTheManUnlimitedCoinsUpgradesPatch = bytecodePatch(
    name = "Unlock All Characters & Upgrades - Dan The Man (Experimental)",
    description = "⚠️ [En cours de développement / Non testé] Unlocks all heroes (Dan, Josie, Barry Steakfries), combat abilities, costumes, and boosts gold coins in Dan The Man.",
) {
    compatibleWith(COMPATIBILITY_DAN_THE_MAN)

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        executeDanTheManUnlimitedCoinsUpgradesLogic(logger)
    }
}

fun BytecodePatchContext.executeDanTheManUnlimitedCoinsUpgradesLogic(logger: Logger) {
    logger.info("Executing Unlock All Characters & Upgrades patch for Dan The Man...")
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

            // 1. Character, costume & upgrade unlock flags
            if (!isStatic && (
                mName == "ischaracterunlocked" ||
                mName == "ischaracterowned" ||
                mName == "isitemunlocked" ||
                mName == "isabilityunlocked" ||
                mName == "isupgradeunlocked" ||
                mName == "hascharacter" ||
                mName == "canselectcharacter"
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
                    logger.info("[DanTheMan Unlocks] Unlocked character/upgrade check in: ${classDef.type}->${method.name}")
                } catch (e: Exception) {
                    logger.warning("[DanTheMan Unlocks] Failed to hook ${method.name}: ${e.message}")
                }
            }

            // 2. High coins getter
            if (!isStatic && (
                mName == "getcoins" ||
                mName == "getgoldcoins" ||
                mName == "getcoincount"
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
                    logger.info("[DanTheMan Unlocks] Boosted coin balance in: ${classDef.type}->${method.name}")
                } catch (e: Exception) {
                    logger.warning("[DanTheMan Unlocks] Failed to hook ${method.name}: ${e.message}")
                }
            }
        }
    }
    logger.info("[DanTheMan Unlocks] Total upgrade hooks applied: $hookedPoints")
}

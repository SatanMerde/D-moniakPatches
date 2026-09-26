package com.dmoniak.patches.crossyroad

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.AccessFlags
import com.dmoniak.patches.hungryshark.util.findMutableMethodOf
import com.dmoniak.patches.shared.Constants.COMPATIBILITY_CROSSY_ROAD
import java.util.logging.Logger

@Suppress("unused")
val crossyRoadUnlockCharactersPatch = bytecodePatch(
    name = "Unlock All Characters & Figurines - Crossy Road (Experimental)",
    description = "⚠️ [En cours de développement / Non testé] Unlocks all characters, mystery mascots, secret figurines, and provides free prize machine tokens in Crossy Road.",
) {
    compatibleWith(COMPATIBILITY_CROSSY_ROAD)

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        executeCrossyRoadUnlockCharactersLogic(logger)
    }
}

fun BytecodePatchContext.executeCrossyRoadUnlockCharactersLogic(logger: Logger) {
    logger.info("Executing Unlock All Characters patch for Crossy Road...")
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

            // 1. Character unlock flags
            if (!isStatic && (
                mName == "ischaracterunlocked" ||
                mName == "ischaracterowned" ||
                mName == "hascharacter" ||
                mName == "isfigurineunlocked" ||
                mName == "isitemowned" ||
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
                    logger.info("[CrossyRoad Unlocks] Unlocked character check in: ${classDef.type}->${method.name}")
                } catch (e: Exception) {
                    logger.warning("[CrossyRoad Unlocks] Failed to hook ${method.name}: ${e.message}")
                }
            }

            // 2. Free prize machine coins
            if (!isStatic && (
                mName == "getcoins" ||
                mName == "getcoincount" ||
                mName == "getcurrentcoins"
            ) && retType == "I") {
                try {
                    val mutableMethod = mutableClass.findMutableMethodOf(method)
                    mutableMethod.addInstructions(
                        0,
                        """
                        const v0, 0x0001869f
                        return v0
                        """.trimIndent()
                    )
                    hookedPoints++
                    logger.info("[CrossyRoad Unlocks] Forced coin balance in: ${classDef.type}->${method.name}")
                } catch (e: Exception) {
                    logger.warning("[CrossyRoad Unlocks] Failed to hook ${method.name}: ${e.message}")
                }
            }
        }
    }
    logger.info("[CrossyRoad Unlocks] Total unlock hooks applied: $hookedPoints")
}

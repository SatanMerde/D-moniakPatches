package com.dmoniak.patches.subwaysurfers

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.AccessFlags
import com.dmoniak.patches.hungryshark.util.findMutableMethodOf
import com.dmoniak.patches.shared.Constants.COMPATIBILITY_SUBWAY_SURFERS
import java.util.logging.Logger

@Suppress("unused")
val subwaySurfersUnlockCharactersBoardsPatch = bytecodePatch(
    name = "Unlock Characters & Hoverboards - Subway Surfers (Experimental)",
    description = "⚠️ [En cours de développement / Non testé] Bypasses character and hoverboard ownership verification to unlock all runners, outfits, and boards in Subway Surfers.",
) {
    compatibleWith(COMPATIBILITY_SUBWAY_SURFERS)

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        executeSubwaySurfersUnlockCharactersBoardsLogic(logger)
    }
}

fun BytecodePatchContext.executeSubwaySurfersUnlockCharactersBoardsLogic(logger: Logger) {
    logger.info("Executing Unlock Characters & Hoverboards patch for Subway Surfers...")
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

            // 1. Force character, board, outfit ownership checks to true
            if (!isStatic && (
                mName == "ischaracterunlocked" ||
                mName == "isboardunlocked" ||
                mName == "isoutfitunlocked" ||
                mName == "hascharacter" ||
                mName == "hasboard" ||
                mName == "isitemowned" ||
                mName == "canselectcharacter" ||
                mName == "canselectboard"
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
                    logger.info("[SubwaySurfers Unlocks] Unlocked item in: ${classDef.type}->${method.name}")
                } catch (e: Exception) {
                    logger.warning("[SubwaySurfers Unlocks] Failed to hook ${method.name}: ${e.message}")
                }
            }
        }
    }
    logger.info("[SubwaySurfers Unlocks] Total unlock hooks applied: $hookedPoints")
}

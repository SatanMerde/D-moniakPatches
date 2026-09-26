package com.dmoniak.patches.doodlejump

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.AccessFlags
import com.dmoniak.patches.hungryshark.util.findMutableMethodOf
import com.dmoniak.patches.shared.Constants.COMPATIBILITY_DOODLE_JUMP
import java.util.logging.Logger

@Suppress("unused")
val doodleJumpUnlockThemesOutfitsPatch = bytecodePatch(
    name = "Unlock All Themes & Outfits - Doodle Jump (Experimental)",
    description = "⚠️ [En cours de développement / Non testé] Unlocks all legendary world themes (Ninja, Space, Underwater, Jungle, Halloween, Christmas) and outfits in Doodle Jump.",
) {
    compatibleWith(COMPATIBILITY_DOODLE_JUMP)

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        executeDoodleJumpUnlockThemesOutfitsLogic(logger)
    }
}

fun BytecodePatchContext.executeDoodleJumpUnlockThemesOutfitsLogic(logger: Logger) {
    logger.info("Executing Unlock All Themes & Outfits patch for Doodle Jump...")
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

            // Theme & outfit unlocks
            if (!isStatic && (
                mName == "isthemeunlocked" ||
                mName == "isoutfitunlocked" ||
                mName == "iscostumeunlocked" ||
                mName == "isitemowned" ||
                mName == "hastheme" ||
                mName == "canselecttheme"
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
                    logger.info("[DoodleJump Unlocks] Unlocked theme/outfit in: ${classDef.type}->${method.name}")
                } catch (e: Exception) {
                    logger.warning("[DoodleJump Unlocks] Failed to hook ${method.name}: ${e.message}")
                }
            }
        }
    }
    logger.info("[DoodleJump Unlocks] Total unlock hooks applied: $hookedPoints")
}

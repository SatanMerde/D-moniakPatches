package com.dmoniak.patches.worldmapquiz

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.AccessFlags
import com.dmoniak.patches.hungryshark.util.findMutableMethodOf
import com.dmoniak.patches.shared.Constants.COMPATIBILITY_WORLD_MAP_QUIZ
import java.util.logging.Logger

@Suppress("unused")
val worldMapQuizUnlockPremiumPatch = bytecodePatch(
    name = "Unlock Premium & Unlimited Hints - World Map Quiz (Experimental)",
    description = "⚠️ [En cours de développement / Non testé] Unlocks the Premium edition, all continental maps, flag & capital quiz modes, and provides unlimited hint tokens in World Map Quiz.",
) {
    compatibleWith(COMPATIBILITY_WORLD_MAP_QUIZ)

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        executeWorldMapQuizUnlockPremiumLogic(logger)
    }
}

fun BytecodePatchContext.executeWorldMapQuizUnlockPremiumLogic(logger: Logger) {
    logger.info("Executing Unlock Premium patch for World Map Quiz...")
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

            // 1. Premium & mode unlock flags
            if (!isStatic && (
                mName == "ispremium" ||
                mName == "ispro" ||
                mName == "hasremovedads" ||
                mName == "isunlocked" ||
                mName == "ispackunlocked" ||
                mName == "ismodetypeunlocked" ||
                mName == "canplaymode"
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
                    logger.info("[WorldMapQuiz Premium] Forced premium/unlock flag in: ${classDef.type}->${method.name}")
                } catch (e: Exception) {
                    logger.warning("[WorldMapQuiz Premium] Failed to hook ${method.name}: ${e.message}")
                }
            }

            // 2. Unlimited hints count
            if (!isStatic && (
                mName == "gethintcount" ||
                mName == "gethints" ||
                mName == "getremaininghints"
            ) && retType == "I") {
                try {
                    val mutableMethod = mutableClass.findMutableMethodOf(method)
                    mutableMethod.addInstructions(
                        0,
                        """
                        const/16 v0, 0x3e7
                        return v0
                        """.trimIndent()
                    )
                    hookedPoints++
                    logger.info("[WorldMapQuiz Premium] Forced hints count in: ${classDef.type}->${method.name}")
                } catch (e: Exception) {
                    logger.warning("[WorldMapQuiz Premium] Failed to hook ${method.name}: ${e.message}")
                }
            }
        }
    }
    logger.info("[WorldMapQuiz Premium] Total premium hooks applied: $hookedPoints")
}

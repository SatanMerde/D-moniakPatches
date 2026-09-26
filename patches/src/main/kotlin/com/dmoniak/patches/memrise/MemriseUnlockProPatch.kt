package com.dmoniak.patches.memrise

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.AccessFlags
import com.dmoniak.patches.hungryshark.util.findMutableMethodOf
import com.dmoniak.patches.shared.Constants.COMPATIBILITY_MEMRISE
import java.util.logging.Logger

@Suppress("unused")
val memriseUnlockProPatch = bytecodePatch(
    name = "Unlock Memrise Pro - Memrise (Experimental)",
    description = "⚠️ [En cours de développement / Non testé] Unlocks Memrise Pro: all language courses, Learn with Locals native speaker clips, grammar bot, difficult words reviews, and offline downloads.",
) {
    compatibleWith(COMPATIBILITY_MEMRISE)

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        executeMemriseUnlockProLogic(logger)
    }
}

fun BytecodePatchContext.executeMemriseUnlockProLogic(logger: Logger) {
    logger.info("Executing Unlock Pro patch for Memrise...")
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

            // Pro & Subscription flags
            if (!isStatic && (
                mName == "ispro" ||
                mName == "isprouser" ||
                mName == "isprosubscriber" ||
                mName == "ispremium" ||
                mName == "hassubscription" ||
                mName == "islearningmodeunlocked" ||
                mName == "canaccesslearnwithlocals" ||
                mName == "canaccessdifficultwords" ||
                mName == "isofflineaccessenabled"
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
                    logger.info("[Memrise Pro] Forced Pro subscription in: ${classDef.type}->${method.name}")
                } catch (e: Exception) {
                    logger.warning("[Memrise Pro] Failed to hook ${method.name}: ${e.message}")
                }
            }
        }
    }
    logger.info("[Memrise Pro] Total Pro hooks applied: $hookedPoints")
}

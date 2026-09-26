package com.dmoniak.patches.peak

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.AccessFlags
import com.dmoniak.patches.hungryshark.util.findMutableMethodOf
import com.dmoniak.patches.shared.Constants.COMPATIBILITY_PEAK
import java.util.logging.Logger

@Suppress("unused")
val peakUnlockProPatch = bytecodePatch(
    name = "Unlock Peak Pro - Peak Brain Training (Experimental)",
    description = "⚠️ [En cours de développement / Non testé] Unlocks Peak Pro: all 45+ cognitive brain games, unlimited daily workouts, advanced brain analytics, and personalized coach training modules.",
) {
    compatibleWith(COMPATIBILITY_PEAK)

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        executePeakUnlockProLogic(logger)
    }
}

fun BytecodePatchContext.executePeakUnlockProLogic(logger: Logger) {
    logger.info("Executing Unlock Pro patch for Peak...")
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
                mName == "isgameunlocked" ||
                mName == "canplaygame" ||
                mName == "canaccessinsights" ||
                mName == "canplayworkout"
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
                    logger.info("[Peak Pro] Forced Pro subscription in: ${classDef.type}->${method.name}")
                } catch (e: Exception) {
                    logger.warning("[Peak Pro] Failed to hook ${method.name}: ${e.message}")
                }
            }
        }
    }
    logger.info("[Peak Pro] Total Pro hooks applied: $hookedPoints")
}

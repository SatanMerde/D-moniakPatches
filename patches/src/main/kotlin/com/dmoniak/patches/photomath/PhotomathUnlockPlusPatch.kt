package com.dmoniak.patches.photomath

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.AccessFlags
import com.dmoniak.patches.hungryshark.util.findMutableMethodOf
import com.dmoniak.patches.shared.Constants.COMPATIBILITY_PHOTOMATH
import java.util.logging.Logger

@Suppress("unused")
val photomathUnlockPlusPatch = bytecodePatch(
    name = "Unlock Photomath Plus - Photomath (Experimental)",
    description = "⚠️ [En cours de développement / Non testé] Unlocks Photomath Plus features: deep step-by-step mathematical explanations, animated calculation walkthroughs, and textbook solutions.",
) {
    compatibleWith(COMPATIBILITY_PHOTOMATH)

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        executePhotomathUnlockPlusLogic(logger)
    }
}

fun BytecodePatchContext.executePhotomathUnlockPlusLogic(logger: Logger) {
    logger.info("Executing Unlock Plus patch for Photomath...")
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

            // Plus & Subscription status flags
            if (!isStatic && (
                mName == "isplus" ||
                mName == "isplususer" ||
                mName == "isplussubscriber" ||
                mName == "isplusunlocked" ||
                mName == "hassubscription" ||
                mName == "ispro" ||
                mName == "canviewexplanation" ||
                mName == "canviewanimatedtutorial" ||
                mName == "canviewtextbook" ||
                mName == "hasunlimitedaccess"
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
                    logger.info("[Photomath Plus] Forced Plus subscription check in: ${classDef.type}->${method.name}")
                } catch (e: Exception) {
                    logger.warning("[Photomath Plus] Failed to hook ${method.name}: ${e.message}")
                }
            }
        }
    }
    logger.info("[Photomath Plus] Total Plus hooks applied: $hookedPoints")
}

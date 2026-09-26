package com.dmoniak.patches.deepl

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.AccessFlags
import com.dmoniak.patches.hungryshark.util.findMutableMethodOf
import com.dmoniak.patches.shared.Constants.COMPATIBILITY_DEEPL
import java.util.logging.Logger

@Suppress("unused")
val deepLUnlockProPatch = bytecodePatch(
    name = "Unlock Pro & Formality - DeepL (Experimental)",
    description = "⚠️ [En cours de développement / Non testé] Unlocks DeepL Pro client features: tone/formality customization (formal/informal), extended character limits, and dictionary features.",
) {
    compatibleWith(COMPATIBILITY_DEEPL)

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        executeDeepLUnlockProLogic(logger)
    }
}

fun BytecodePatchContext.executeDeepLUnlockProLogic(logger: Logger) {
    logger.info("Executing Unlock Pro patch for DeepL...")
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

            // Pro & Formality flags
            if (!isStatic && (
                mName == "ispro" ||
                mName == "isprouser" ||
                mName == "isprosubscriber" ||
                mName == "ispremium" ||
                mName == "hassubscription" ||
                mName == "canuseformality" ||
                mName == "isformalityenabled" ||
                mName == "hasunlimitedcharacters"
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
                    logger.info("[DeepL Pro] Forced Pro feature in: ${classDef.type}->${method.name}")
                } catch (e: Exception) {
                    logger.warning("[DeepL Pro] Failed to hook ${method.name}: ${e.message}")
                }
            }
        }
    }
    logger.info("[DeepL Pro] Total Pro hooks applied: $hookedPoints")
}

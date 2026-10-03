package com.dmoniak.patches.undercover

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.AccessFlags
import com.dmoniak.patches.hungryshark.util.findMutableMethodOf
import com.dmoniak.patches.shared.Constants.COMPATIBILITY_UNDERCOVER
import java.util.logging.Logger

@Suppress("unused")
val undercoverUnlockAllWordPacksPatch = bytecodePatch(
    name = "Unlock All Word Packs & Premium - Undercover (Experimental)",
    description = "⚠️ [En cours de développement / Non testé] Hooks in-app purchase verification and license state to unlock all premium word packs (Adult 18+, Pop Culture, Geek, Cinema, Science & History) and remove all paywalls.",
) {
    compatibleWith(COMPATIBILITY_UNDERCOVER)

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        executeUndercoverUnlockWordPacksLogic(logger)
    }
}

fun BytecodePatchContext.executeUndercoverUnlockWordPacksLogic(logger: Logger) {
    logger.info("Executing Unlock All Word Packs & Premium patch for Undercover...")
    var hookedPoints = 0

    classDefForEach { classDef ->
        val tl = classDef.type.lowercase()
        if (tl.contains("androidx") || tl.contains("android/support") || tl.contains("google")) return@classDefForEach

        val mutableClass by lazy { mutableClassDefBy(classDef) }

        for (method in classDef.methods.toList()) {
            if (method.implementation == null) continue
            val isStatic = AccessFlags.STATIC.isSet(method.accessFlags)
            val mName = method.name.lowercase()
            val retType = method.returnType

            // Hook premium purchase & word pack unlock checkers
            if (!isStatic && (
                mName == "iswordpackunlocked" ||
                mName == "ispackpurchased" ||
                mName == "ispackunlocked" ||
                mName == "haspurchasedpack" ||
                mName == "ispremium" ||
                mName == "ispremiumuser" ||
                mName == "isvip" ||
                mName == "hasfullversion" ||
                mName == "isadfree" ||
                mName == "hasunlockedallpacks"
            ) && retType == "Z") {
                mutableClass.findMutableMethodOf(method)?.let { mutableMethod ->
                    mutableMethod.addInstructions(
                        0,
                        """
                        const/4 v0, 0x1
                        return v0
                        """.trimIndent()
                    )
                    hookedPoints++
                }
            }
        }
    }

    logger.info("Hooked $hookedPoints word pack and premium checkpoints in Undercover.")
}

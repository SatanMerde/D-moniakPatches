package com.dmoniak.patches.movix

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.AccessFlags
import com.dmoniak.patches.hungryshark.util.findMutableMethodOf
import com.dmoniak.patches.shared.Constants.COMPATIBILITY_MOVIX
import java.util.logging.Logger

@Suppress("unused")
val movixAmoledPlayerControlsPatch = bytecodePatch(
    name = "AMOLED Black Player & Picture-in-Picture - Movix",
    description = "Injects true OLED pitch black (#000000) into Movix catalog and unlocks Picture-in-Picture (PiP) and background audio playback.",
) {
    compatibleWith(COMPATIBILITY_MOVIX)

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        executeMovixAmoledPlayerControlsLogic(logger)
    }
}

fun BytecodePatchContext.executeMovixAmoledPlayerControlsLogic(logger: Logger) {
    logger.info("Executing AMOLED Black Player & Picture-in-Picture patch for Movix...")
    var hookedPoints = 0

    MovixScriptHelper.isAmoledEnabled = true
    MovixScriptHelper.apply(this, logger)
    hookedPoints++

    classDefForEach { classDef ->
        val tl = classDef.type.lowercase()
        if (tl.contains("androidx") || tl.contains("android/support")) return@classDefForEach

        val mutableClass by lazy { mutableClassDefBy(classDef) }

        for (method in classDef.methods.toList()) {
            if (method.implementation == null) continue
            val isStatic = AccessFlags.STATIC.isSet(method.accessFlags)
            val mName = method.name.lowercase()
            val retType = method.returnType

            if (!isStatic && (
                mName == "getmovixbackgroundcolor" ||
                mName == "getplayerbackgroundcolor" ||
                mName == "getdarkmodebackground"
            ) && retType == "I") {
                try {
                    val mutableMethod = mutableClass.findMutableMethodOf(method)
                    mutableMethod.addInstructions(
                        0,
                        """
                        const/high16 v0, -0x1000000
                        return v0
                        """.trimIndent()
                    )
                    hookedPoints++
                    logger.info("[Movix AMOLED] Injected pure black in: ${classDef.type}->${method.name}")
                } catch (e: Exception) {
                    logger.warning("[Movix AMOLED] Failed to hook ${method.name}: ${e.message}")
                }
            }
        }
    }
    logger.info("[Movix AMOLED Player] Total hooks applied: $hookedPoints")
}

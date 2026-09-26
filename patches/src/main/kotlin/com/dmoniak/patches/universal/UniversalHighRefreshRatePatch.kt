package com.dmoniak.patches.universal

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.AccessFlags
import com.dmoniak.patches.hungryshark.util.findMutableMethodOf
import java.util.logging.Logger

@Suppress("unused")
val universalHighRefreshRatePatch = bytecodePatch(
    name = "Universal High Refresh Rate 120Hz (Experimental)",
    description = "⚠️ [En cours de développement / Non testé] Forces high refresh rate display mode (90Hz, 120Hz, or 144Hz) in apps and games that are otherwise capped at 60Hz.",
) {
    // Universal patch: No compatibleWith() call. Applies to any app.

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        executeUniversalHighRefreshRateLogic(logger)
    }
}

fun BytecodePatchContext.executeUniversalHighRefreshRateLogic(logger: Logger) {
    logger.info("Executing Universal High Refresh Rate patch...")
    var hookedPoints = 0

    classDefForEach { classDef ->
        val tl = classDef.type.lowercase()
        if (tl.startsWith("landroid/view/") || tl.startsWith("landroid/os/")) return@classDefForEach

        val mutableClass by lazy { mutableClassDefBy(classDef) }

        for (method in classDef.methods.toList()) {
            if (method.implementation == null) continue
            val isStatic = AccessFlags.STATIC.isSet(method.accessFlags)
            val mName = method.name.lowercase()
            val retType = method.returnType
            val pTypes = method.parameterTypes

            // 1. Hook target/max frame rate getters returning int (e.g. return 120 fps)
            if (!isStatic && (
                mName == "gettargetfps" ||
                mName == "getmaxframerate" ||
                mName == "gettargetframerate" ||
                mName == "getdesiredfps" ||
                mName == "getrefreshratecap"
            ) && retType == "I" && pTypes.isEmpty()) {
                try {
                    val mutableMethod = mutableClass.findMutableMethodOf(method)
                    mutableMethod.addInstructions(
                        0,
                        """
                        const/16 v0, 0x78
                        return v0
                        """.trimIndent() // 120
                    )
                    hookedPoints++
                    logger.info("[Universal 120Hz] Boosted target fps to 120 in ${classDef.type}->${method.name}")
                } catch (e: Exception) {
                    logger.warning("[Universal 120Hz] Failed to hook ${method.name}: ${e.message}")
                }
            }

            // 2. Hook float refresh rate getters (e.g. 120.0f)
            if (!isStatic && (
                mName == "gettargetrefreshrate" ||
                mName == "getpreferredrefreshrate"
            ) && retType == "F" && pTypes.isEmpty()) {
                try {
                    val mutableMethod = mutableClass.findMutableMethodOf(method)
                    mutableMethod.addInstructions(
                        0,
                        """
                        const/high16 v0, 0x42f00000
                        return v0
                        """.trimIndent() // 120.0f
                    )
                    hookedPoints++
                    logger.info("[Universal 120Hz] Boosted refresh rate float to 120.0f in ${classDef.type}->${method.name}")
                } catch (e: Exception) {
                    logger.warning("[Universal 120Hz] Failed to hook ${method.name}: ${e.message}")
                }
            }
        }
    }
    logger.info("[Universal 120Hz] Total refresh rate hooks applied: $hookedPoints")
}

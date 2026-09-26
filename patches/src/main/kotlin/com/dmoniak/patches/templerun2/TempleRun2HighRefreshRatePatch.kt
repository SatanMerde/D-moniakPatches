package com.dmoniak.patches.templerun2

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.AccessFlags
import com.dmoniak.patches.hungryshark.util.findMutableMethodOf
import com.dmoniak.patches.shared.Constants.COMPATIBILITY_TEMPLE_RUN_2
import java.util.logging.Logger

@Suppress("unused")
val templeRun2HighRefreshRatePatch = bytecodePatch(
    name = "120 FPS High Refresh Rate - Temple Run 2 (Experimental)",
    description = "⚠️ [En cours de développement / Non testé] Forces 120Hz high framerate rendering in Temple Run 2 for smooth corner turns and swipe sensitivity.",
) {
    compatibleWith(COMPATIBILITY_TEMPLE_RUN_2)

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        executeTempleRun2HighRefreshRateLogic(logger)
    }
}

fun BytecodePatchContext.executeTempleRun2HighRefreshRateLogic(logger: Logger) {
    logger.info("Executing 120 FPS High Refresh Rate patch for Temple Run 2...")
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

            if (!isStatic && (
                mName == "gettargetframerate" ||
                mName == "gettargetfps" ||
                mName == "getmaxfps"
            ) && retType == "I") {
                try {
                    val mutableMethod = mutableClass.findMutableMethodOf(method)
                    mutableMethod.addInstructions(
                        0,
                        """
                        const/16 v0, 0x78
                        return v0
                        """.trimIndent()
                    )
                    hookedPoints++
                    logger.info("[TempleRun2 FPS] Forced 120 FPS in: ${classDef.type}->${method.name}")
                } catch (e: Exception) {
                    logger.warning("[TempleRun2 FPS] Failed to hook ${method.name}: ${e.message}")
                }
            }
        }
    }
    logger.info("[TempleRun2 FPS] Total FPS hooks applied: $hookedPoints")
}

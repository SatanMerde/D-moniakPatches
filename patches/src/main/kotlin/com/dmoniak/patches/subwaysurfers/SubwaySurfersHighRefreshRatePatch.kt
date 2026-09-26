package com.dmoniak.patches.subwaysurfers

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.AccessFlags
import com.dmoniak.patches.hungryshark.util.findMutableMethodOf
import com.dmoniak.patches.shared.Constants.COMPATIBILITY_SUBWAY_SURFERS
import java.util.logging.Logger

@Suppress("unused")
val subwaySurfersHighRefreshRatePatch = bytecodePatch(
    name = "120 FPS High Refresh Rate & Low Latency - Subway Surfers (Experimental)",
    description = "⚠️ [En cours de développement / Non testé] Forces 120Hz/90Hz high refresh rate target framerate in Subway Surfers, eliminating input lag and frame pacing stutters.",
) {
    compatibleWith(COMPATIBILITY_SUBWAY_SURFERS)

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        executeSubwaySurfersHighRefreshRateLogic(logger)
    }
}

fun BytecodePatchContext.executeSubwaySurfersHighRefreshRateLogic(logger: Logger) {
    logger.info("Executing 120 FPS High Refresh Rate patch for Subway Surfers...")
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

            // 1. Force target frame rate getters to 120 FPS
            if (!isStatic && (
                mName == "gettargetframerate" ||
                mName == "gettargetfps" ||
                mName == "getmaxframerate" ||
                mName == "getrefreshrate"
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
                    logger.info("[SubwaySurfers FPS] Forced 120 FPS in: ${classDef.type}->${method.name}")
                } catch (e: Exception) {
                    logger.warning("[SubwaySurfers FPS] Failed to hook ${method.name}: ${e.message}")
                }
            }

            // 2. High refresh rate boolean flags
            if (!isStatic && (
                mName == "ishighrefreshratesupported" ||
                mName == "is120fpsenabled" ||
                mName == "canusehighrefreshrate"
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
                    logger.info("[SubwaySurfers FPS] Enabled high refresh flag in: ${classDef.type}->${method.name}")
                } catch (e: Exception) {
                    logger.warning("[SubwaySurfers FPS] Failed to hook ${method.name}: ${e.message}")
                }
            }
        }
    }
    logger.info("[SubwaySurfers FPS] Total high refresh rate hooks applied: $hookedPoints")
}

package com.dmoniak.patches.windy

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.AccessFlags
import com.dmoniak.patches.hungryshark.util.findMutableMethodOf
import com.dmoniak.patches.shared.Constants.COMPATIBILITY_WINDY
import java.util.logging.Logger

@Suppress("unused")
val windyUnlockPremiumPatch = bytecodePatch(
    name = "Unlock Windy Premium - Windy.com (Experimental)",
    description = "⚠️ [En cours de développement / Non testé] Unlocks Windy Premium features: 1-hour forecast step resolution, 16-day extended forecast, high-res satellite radar archive, route planner, and unlimited alerts.",
) {
    compatibleWith(COMPATIBILITY_WINDY)

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        executeWindyUnlockPremiumLogic(logger)
    }
}

fun BytecodePatchContext.executeWindyUnlockPremiumLogic(logger: Logger) {
    logger.info("Executing Unlock Windy Premium patch for Windy.com...")
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

            // Premium status boolean flags
            if (!isStatic && (
                mName == "ispremium" ||
                mName == "ispro" ||
                mName == "ispremiumuser" ||
                mName == "hassubscription" ||
                mName == "ispremiumactive" ||
                mName == "canaccess1hforecast" ||
                mName == "canaccess16dayforecast" ||
                mName == "canusehighressatellite"
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
                    logger.info("[Windy Premium] Forced Premium status in: ${classDef.type}->${method.name}")
                } catch (e: Exception) {
                    logger.warning("[Windy Premium] Failed to hook ${method.name}: ${e.message}")
                }
            }
        }
    }
    logger.info("[Windy Premium] Total Premium hooks applied: $hookedPoints")
}

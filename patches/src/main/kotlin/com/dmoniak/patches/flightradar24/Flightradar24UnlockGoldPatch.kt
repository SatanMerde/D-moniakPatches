package com.dmoniak.patches.flightradar24

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.AccessFlags
import com.dmoniak.patches.hungryshark.util.findMutableMethodOf
import com.dmoniak.patches.shared.Constants.COMPATIBILITY_FLIGHTRADAR24
import java.util.logging.Logger

@Suppress("unused")
val flightradar24UnlockGoldPatch = bytecodePatch(
    name = "Unlock Silver & Gold Features - Flightradar24 (Experimental)",
    description = "⚠️ [En cours de développement / Non testé] Unlocks Flightradar24 Gold & Silver features: extended flight history, unlimited 3D cockpit views, aeronautical charts, oceanic tracks, and weather radar overlays.",
) {
    compatibleWith(COMPATIBILITY_FLIGHTRADAR24)

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        executeFlightradar24UnlockGoldLogic(logger)
    }
}

fun BytecodePatchContext.executeFlightradar24UnlockGoldLogic(logger: Logger) {
    logger.info("Executing Unlock Gold features patch for Flightradar24...")
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

            // Gold / Silver / Subscription boolean flags
            if (!isStatic && (
                mName == "isgold" ||
                mName == "issilver" ||
                mName == "ispremium" ||
                mName == "isbusiness" ||
                mName == "hassubscription" ||
                mName == "isfeatureunlocked" ||
                mName == "canuse3dview" ||
                mName == "canuseaeronauticalcharts" ||
                mName == "canviewextendedhistory"
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
                    logger.info("[Flightradar24 Gold] Forced membership status in: ${classDef.type}->${method.name}")
                } catch (e: Exception) {
                    logger.warning("[Flightradar24 Gold] Failed to hook ${method.name}: ${e.message}")
                }
            }
        }
    }
    logger.info("[Flightradar24 Gold] Total membership hooks applied: $hookedPoints")
}

package com.dmoniak.patches.alltrails

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.AccessFlags
import com.dmoniak.patches.hungryshark.util.findMutableMethodOf
import com.dmoniak.patches.shared.Constants.COMPATIBILITY_ALLTRAILS
import java.util.logging.Logger

@Suppress("unused")
val allTrailsUnlockPlusPatch = bytecodePatch(
    name = "Unlock AllTrails+ & Offline Maps - AllTrails (Experimental)",
    description = "⚠️ [En cours de développement / Non testé] Unlocks AllTrails+ (Pro) features: offline topo map downloads, wrong-turn navigation alerts, real-time 3D trail previews, and satellite heatmaps.",
) {
    compatibleWith(COMPATIBILITY_ALLTRAILS)

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        executeAllTrailsUnlockPlusLogic(logger)
    }
}

fun BytecodePatchContext.executeAllTrailsUnlockPlusLogic(logger: Logger) {
    logger.info("Executing Unlock AllTrails+ patch for AllTrails...")
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

            // AllTrails+ / Pro boolean flags
            if (!isStatic && (
                mName == "ispro" ||
                mName == "isprouser" ||
                mName == "isplus" ||
                mName == "isplususer" ||
                mName == "ispremium" ||
                mName == "hassubscription" ||
                mName == "canuseofflinemaps" ||
                mName == "candownloadmap" ||
                mName == "canusewrongturnalerts"
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
                    logger.info("[AllTrails Plus] Forced Plus status in: ${classDef.type}->${method.name}")
                } catch (e: Exception) {
                    logger.warning("[AllTrails Plus] Failed to hook ${method.name}: ${e.message}")
                }
            }
        }
    }
    logger.info("[AllTrails Plus] Total AllTrails+ hooks applied: $hookedPoints")
}

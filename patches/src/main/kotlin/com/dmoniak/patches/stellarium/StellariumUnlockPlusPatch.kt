package com.dmoniak.patches.stellarium

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.AccessFlags
import com.dmoniak.patches.hungryshark.util.findMutableMethodOf
import com.dmoniak.patches.shared.Constants.COMPATIBILITY_STELLARIUM
import com.dmoniak.patches.shared.Constants.COMPATIBILITY_STELLARIUM_ALT
import java.util.logging.Logger

@Suppress("unused")
val stellariumUnlockPlusPatch = bytecodePatch(
    name = "Unlock Plus & Gaia Star Catalog - Stellarium Mobile (Experimental)",
    description = "⚠️ [En cours de développement / Non testé] Unlocks Stellarium Plus subscription features: Gaia DR3 star catalog (1.8+ billion stars), full deep-sky objects (DSO), high-res planetary textures, satellite tracking, and telescope control.",
) {
    compatibleWith(COMPATIBILITY_STELLARIUM, COMPATIBILITY_STELLARIUM_ALT)

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        executeStellariumUnlockPlusLogic(logger)
    }
}

fun BytecodePatchContext.executeStellariumUnlockPlusLogic(logger: Logger) {
    logger.info("Executing Unlock Plus patch for Stellarium Mobile...")
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

            // Plus & Premium boolean getters
            if (!isStatic && (
                mName == "isplus" ||
                mName == "isplusunlocked" ||
                mName == "isplususer" ||
                mName == "isplussubscribed" ||
                mName == "hassubscription" ||
                mName == "ispremium" ||
                mName == "hasfullcatalog" ||
                mName == "istelescopecontrolunlocked" ||
                mName == "isfeatureunlocked" ||
                mName == "ispro" ||
                mName == "canaccessgaiacatalog"
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
                    logger.info("[Stellarium Plus] Forced Plus status in: ${classDef.type}->${method.name}")
                } catch (e: Exception) {
                    logger.warning("[Stellarium Plus] Failed to hook ${method.name}: ${e.message}")
                }
            }
        }
    }
    logger.info("[Stellarium Plus] Total unlock hooks applied: $hookedPoints")
}

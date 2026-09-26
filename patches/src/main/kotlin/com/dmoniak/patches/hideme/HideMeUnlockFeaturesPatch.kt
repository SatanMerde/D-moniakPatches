package com.dmoniak.patches.hideme

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.AccessFlags
import com.dmoniak.patches.hungryshark.util.findMutableMethodOf
import com.dmoniak.patches.shared.Constants.COMPATIBILITY_HIDEME
import java.util.logging.Logger

@Suppress("unused")
val hideMeUnlockFeaturesPatch = bytecodePatch(
    name = "Unlock Client Features & Dark Mode - hide.me VPN (Experimental)",
    description = "⚠️ [En cours de développement / Non testé] Unlocks client-side features, AMOLED dark theme, and DNS leak test utilities in hide.me VPN.",
) {
    compatibleWith(COMPATIBILITY_HIDEME)

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        executeHideMeUnlockFeaturesLogic(logger)
    }
}

fun BytecodePatchContext.executeHideMeUnlockFeaturesLogic(logger: Logger) {
    logger.info("Executing Unlock Client Features patch for hide.me VPN...")
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

            // Premium & feature status flags
            if (!isStatic && (
                mName == "ispremium" ||
                mName == "ispro" ||
                mName == "hassubscription" ||
                mName == "isfeatureunlocked" ||
                mName == "canusedarkmode" ||
                mName == "canusednsleakprotection"
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
                    logger.info("[hide.me Features] Forced feature flag in: ${classDef.type}->${method.name}")
                } catch (e: Exception) {
                    logger.warning("[hide.me Features] Failed to hook ${method.name}: ${e.message}")
                }
            }
        }
    }
    logger.info("[hide.me Features] Total feature hooks applied: $hookedPoints")
}

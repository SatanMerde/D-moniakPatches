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
val stellariumBlockAdsDeclutterPatch = bytecodePatch(
    name = "Block Ads & Promo Popups - Stellarium Mobile (Experimental)",
    description = "⚠️ [En cours de développement / Non testé] Strips promotional upgrade dialogues, rating prompts, and banner notices in Stellarium Mobile.",
) {
    compatibleWith(COMPATIBILITY_STELLARIUM, COMPATIBILITY_STELLARIUM_ALT)

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        executeStellariumBlockAdsDeclutterLogic(logger)
    }
}

fun BytecodePatchContext.executeStellariumBlockAdsDeclutterLogic(logger: Logger) {
    logger.info("Executing Block Ads & Promo patch for Stellarium Mobile...")
    var hookedPoints = 0

    classDefForEach { classDef ->
        val tl = classDef.type.lowercase()
        if (tl.contains("androidx") || tl.contains("android/support") || tl.contains("com/google/android/gms")) return@classDefForEach

        val mutableClass by lazy { mutableClassDefBy(classDef) }

        for (method in classDef.methods.toList()) {
            if (method.implementation == null) continue
            val isStatic = AccessFlags.STATIC.isSet(method.accessFlags)
            val mName = method.name.lowercase()
            val retType = method.returnType

            if (!isStatic && (
                mName == "isadready" ||
                mName == "shouldshowad" ||
                mName == "isinterstitialready" ||
                mName == "shouldshowupgradeprompt" ||
                mName == "shouldpromptrate"
            ) && retType == "Z") {
                try {
                    val mutableMethod = mutableClass.findMutableMethodOf(method)
                    mutableMethod.addInstructions(
                        0,
                        """
                        const/4 v0, 0x0
                        return v0
                        """.trimIndent()
                    )
                    hookedPoints++
                    logger.info("[Stellarium Declutter] Neutralized prompt check in: ${classDef.type}->${method.name}")
                } catch (e: Exception) {
                    logger.warning("[Stellarium Declutter] Failed to hook ${method.name}: ${e.message}")
                }
            }

            if (!isStatic && (
                mName == "showad" ||
                mName == "showinterstitial" ||
                mName == "showupgradeprompt" ||
                mName == "showpaywall" ||
                mName == "showrateprompt"
            ) && retType == "V") {
                try {
                    val mutableMethod = mutableClass.findMutableMethodOf(method)
                    mutableMethod.addInstructions(
                        0,
                        """
                        return-void
                        """.trimIndent()
                    )
                    hookedPoints++
                    logger.info("[Stellarium Declutter] Neutralized prompt show in: ${classDef.type}->${method.name}")
                } catch (e: Exception) {
                    logger.warning("[Stellarium Declutter] Failed to hook ${method.name}: ${e.message}")
                }
            }
        }
    }
    logger.info("[Stellarium Declutter] Total declutter hooks applied: $hookedPoints")
}

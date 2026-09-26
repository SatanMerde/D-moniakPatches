package com.dmoniak.patches.alltrails

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.AccessFlags
import com.dmoniak.patches.hungryshark.util.findMutableMethodOf
import com.dmoniak.patches.shared.Constants.COMPATIBILITY_ALLTRAILS
import java.util.logging.Logger

@Suppress("unused")
val allTrailsBlockAdsPatch = bytecodePatch(
    name = "Block Ads & Promo Screens - AllTrails (Experimental)",
    description = "⚠️ [En cours de développement / Non testé] Strips promotional upgrade screens, rating dialogs, and banner advertisements in AllTrails.",
) {
    compatibleWith(COMPATIBILITY_ALLTRAILS)

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        executeAllTrailsBlockAdsLogic(logger)
    }
}

fun BytecodePatchContext.executeAllTrailsBlockAdsLogic(logger: Logger) {
    logger.info("Executing Block Ads patch for AllTrails...")
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
                mName == "shouldpromotepro"
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
                    logger.info("[AllTrails Ads] Neutralized check in: ${classDef.type}->${method.name}")
                } catch (e: Exception) {
                    logger.warning("[AllTrails Ads] Failed to hook ${method.name}: ${e.message}")
                }
            }

            if (!isStatic && (
                mName == "showupgradescreen" ||
                mName == "showpaywall" ||
                mName == "showinterstitial" ||
                mName == "showad"
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
                    logger.info("[AllTrails Ads] Neutralized promo show in: ${classDef.type}->${method.name}")
                } catch (e: Exception) {
                    logger.warning("[AllTrails Ads] Failed to hook ${method.name}: ${e.message}")
                }
            }
        }
    }
    logger.info("[AllTrails Ads] Total promo hooks applied: $hookedPoints")
}

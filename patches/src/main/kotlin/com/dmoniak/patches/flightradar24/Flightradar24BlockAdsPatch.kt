package com.dmoniak.patches.flightradar24

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.AccessFlags
import com.dmoniak.patches.hungryshark.util.findMutableMethodOf
import com.dmoniak.patches.shared.Constants.COMPATIBILITY_FLIGHTRADAR24
import java.util.logging.Logger

@Suppress("unused")
val flightradar24BlockAdsPatch = bytecodePatch(
    name = "Block Ads & Promo Screens - Flightradar24 (Experimental)",
    description = "⚠️ [En cours de développement / Non testé] Strips promotional upgrade dialogs, map banner advertisements, and sponsored popups in Flightradar24.",
) {
    compatibleWith(COMPATIBILITY_FLIGHTRADAR24)

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        executeFlightradar24BlockAdsLogic(logger)
    }
}

fun BytecodePatchContext.executeFlightradar24BlockAdsLogic(logger: Logger) {
    logger.info("Executing Block Ads patch for Flightradar24...")
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
                mName == "isbannerloaded"
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
                    logger.info("[Flightradar24 Ads] Neutralized ad check in: ${classDef.type}->${method.name}")
                } catch (e: Exception) {
                    logger.warning("[Flightradar24 Ads] Failed to hook ${method.name}: ${e.message}")
                }
            }

            if (!isStatic && (
                mName == "showinterstitial" ||
                mName == "showad" ||
                mName == "showbanner" ||
                mName == "showupgradescreen" ||
                mName == "showpaywall"
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
                    logger.info("[Flightradar24 Ads] Neutralized ad/promo show in: ${classDef.type}->${method.name}")
                } catch (e: Exception) {
                    logger.warning("[Flightradar24 Ads] Failed to hook ${method.name}: ${e.message}")
                }
            }
        }
    }
    logger.info("[Flightradar24 Ads] Total ad hooks applied: $hookedPoints")
}

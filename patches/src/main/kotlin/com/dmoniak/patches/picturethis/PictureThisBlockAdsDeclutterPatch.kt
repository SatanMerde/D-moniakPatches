package com.dmoniak.patches.picturethis

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.AccessFlags
import com.dmoniak.patches.hungryshark.util.findMutableMethodOf
import com.dmoniak.patches.shared.Constants.COMPATIBILITY_PICTURETHIS
import java.util.logging.Logger

@Suppress("unused")
val pictureThisBlockAdsDeclutterPatch = bytecodePatch(
    name = "Block Ads & Paywall Prompts - PictureThis (Experimental)",
    description = "⚠️ [En cours de développement / Non testé] Strips startup subscription paywalls, rating prompts, and banner ads in PictureThis.",
) {
    compatibleWith(COMPATIBILITY_PICTURETHIS)

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        executePictureThisBlockAdsDeclutterLogic(logger)
    }
}

fun BytecodePatchContext.executePictureThisBlockAdsDeclutterLogic(logger: Logger) {
    logger.info("Executing Block Ads & Paywall patch for PictureThis...")
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
                mName == "shouldshowpaywall" ||
                mName == "shouldpromptrating"
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
                    logger.info("[PictureThis Ads] Neutralized check in: ${classDef.type}->${method.name}")
                } catch (e: Exception) {
                    logger.warning("[PictureThis Ads] Failed to hook ${method.name}: ${e.message}")
                }
            }

            if (!isStatic && (
                mName == "showpaywall" ||
                mName == "showsubscriptiondialog" ||
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
                    logger.info("[PictureThis Ads] Neutralized paywall in: ${classDef.type}->${method.name}")
                } catch (e: Exception) {
                    logger.warning("[PictureThis Ads] Failed to hook ${method.name}: ${e.message}")
                }
            }
        }
    }
    logger.info("[PictureThis Ads] Total declutter hooks applied: $hookedPoints")
}

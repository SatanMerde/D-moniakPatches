package com.dmoniak.patches.deepl

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.AccessFlags
import com.dmoniak.patches.hungryshark.util.findMutableMethodOf
import com.dmoniak.patches.shared.Constants.COMPATIBILITY_DEEPL
import java.util.logging.Logger

@Suppress("unused")
val deepLBlockAdsDeclutterPatch = bytecodePatch(
    name = "Block Ads & Promo Screens - DeepL (Experimental)",
    description = "⚠️ [En cours de développement / Non testé] Strips promotional upgrade screens, rating dialogs, and subscription banners in DeepL.",
) {
    compatibleWith(COMPATIBILITY_DEEPL)

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        executeDeepLBlockAdsDeclutterLogic(logger)
    }
}

fun BytecodePatchContext.executeDeepLBlockAdsDeclutterLogic(logger: Logger) {
    logger.info("Executing Block Ads patch for DeepL...")
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
                mName == "shouldshowupgradeprompt" ||
                mName == "shouldpromotepro" ||
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
                    logger.info("[DeepL Ads] Neutralized check in: ${classDef.type}->${method.name}")
                } catch (e: Exception) {
                    logger.warning("[DeepL Ads] Failed to hook ${method.name}: ${e.message}")
                }
            }

            if (!isStatic && (
                mName == "showupgradescreen" ||
                mName == "showpaywall" ||
                mName == "showinterstitial" ||
                mName == "showad" ||
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
                    logger.info("[DeepL Ads] Neutralized promo show in: ${classDef.type}->${method.name}")
                } catch (e: Exception) {
                    logger.warning("[DeepL Ads] Failed to hook ${method.name}: ${e.message}")
                }
            }
        }
    }
    logger.info("[DeepL Ads] Total declutter hooks applied: $hookedPoints")
}

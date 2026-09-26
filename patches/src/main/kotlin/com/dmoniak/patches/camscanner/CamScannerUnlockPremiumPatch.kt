package com.dmoniak.patches.camscanner

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.AccessFlags
import com.dmoniak.patches.hungryshark.util.findMutableMethodOf
import com.dmoniak.patches.shared.Constants.COMPATIBILITY_CAMSCANNER
import java.util.logging.Logger

@Suppress("unused")
val camScannerUnlockPremiumPatch = bytecodePatch(
    name = "Unlock Premium & Remove Watermarks - CamScanner (Experimental)",
    description = "⚠️ [En cours de développement / Non testé] Unlocks CamScanner Premium features: removes 'Scanned with CamScanner' watermark on PDFs, enables HD scan quality, unlimited OCR text extraction, and electronic signatures.",
) {
    compatibleWith(COMPATIBILITY_CAMSCANNER)

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        executeCamScannerUnlockPremiumLogic(logger)
    }
}

fun BytecodePatchContext.executeCamScannerUnlockPremiumLogic(logger: Logger) {
    logger.info("Executing Unlock Premium patch for CamScanner...")
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

            // 1. VIP / Premium status flags -> true
            if (!isStatic && (
                mName == "isvip" ||
                mName == "isvipuser" ||
                mName == "ispremium" ||
                mName == "ispro" ||
                mName == "ispremiumuser" ||
                mName == "hassubscription" ||
                mName == "canexportwithoutwatermark" ||
                mName == "canusehd" ||
                mName == "canuseocr"
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
                    logger.info("[CamScanner Premium] Forced VIP check in: ${classDef.type}->${method.name}")
                } catch (e: Exception) {
                    logger.warning("[CamScanner Premium] Failed to hook ${method.name}: ${e.message}")
                }
            }

            // 2. Watermark flags -> false
            if (!isStatic && (
                mName == "haswatermark" ||
                mName == "shouldshowwatermark" ||
                mName == "iswatermarkenabled" ||
                mName == "needwatermark"
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
                    logger.info("[CamScanner Watermark] Stripped watermark flag in: ${classDef.type}->${method.name}")
                } catch (e: Exception) {
                    logger.warning("[CamScanner Watermark] Failed to hook ${method.name}: ${e.message}")
                }
            }
        }
    }
    logger.info("[CamScanner Premium] Total premium & watermark hooks applied: $hookedPoints")
}

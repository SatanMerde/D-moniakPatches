package com.dmoniak.patches.capcut

import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.AccessFlags
import com.dmoniak.patches.shared.AdBlockHelper.executeComprehensiveAdBlock
import com.dmoniak.patches.shared.Constants.COMPATIBILITY_CAPCUT
import com.dmoniak.patches.shared.Constants.COMPATIBILITY_CAPCUT_ALT
import com.dmoniak.patches.shared.replaceMethod
import java.util.logging.Logger

@Suppress("unused")
val capCutRemoveWatermarkAdsPatch = bytecodePatch(
    name = "Disable Ending Watermark & Ad Prompts - CapCut (Experimental)",
    description = "⚠️ [Experimental / Untested] Disables default ending clip watermark insertion, strips in-app advertisements, upgrade subscription banners, and paywall popups in CapCut.",
) {
    compatibleWith(COMPATIBILITY_CAPCUT, COMPATIBILITY_CAPCUT_ALT)

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        executeCapCutRemoveWatermarkAdsLogic(logger)
    }
}

fun BytecodePatchContext.executeCapCutRemoveWatermarkAdsLogic(logger: Logger) {
    logger.info("Executing Disable Ending Watermark & Ad Prompts patch for CapCut...")
    var hookedPoints = executeComprehensiveAdBlock(logger, "CapCut")

    classDefForEach { classDef ->
        val tl = classDef.type.lowercase()
        if (tl.startsWith("lkotlin/") || tl.startsWith("landroidx/") || tl.startsWith("landroid/")) return@classDefForEach

        val mutableClass by lazy { mutableClassDefBy(classDef) }

        for (method in classDef.methods.toList()) {
            if (method.implementation == null) continue
            val isStatic = AccessFlags.STATIC.isSet(method.accessFlags)
            val mName = method.name.lowercase()
            val retType = method.returnType

            // 1. Ending watermark / logo clip checkers -> false
            if (retType == "Z" && (
                mName == "isdefaultendingenabled" || mName == "shouldaddendingwatermark" ||
                mName == "isendingwatermarkon" || mName == "hasendingwatermark" ||
                mName == "iswatermarkenabled"
            )) {
                try {
                    mutableClass.replaceMethod(
                        method,
                        registerCount = 2,
                        smaliCode = """
                        const/4 v0, 0x0
                        return v0
                        """.trimIndent()
                    )
                    hookedPoints++
                    logger.fine("[CapCut Watermark] Neutralized ending watermark getter: ${classDef.type}->${method.name}")
                } catch (e: Exception) {
                    logger.warning("[CapCut Watermark] Failed to hook ${classDef.type}->${method.name}: ${e.message}")
                }
            }

            // 2. Paywall / Promo readiness and display checkers -> false
            if (retType == "Z" && (
                mName == "shouldshowpaywall" || mName == "shouldshowupgradedialog" ||
                mName == "isadready" || mName == "isadloaded" || mName == "shouldshowad"
            )) {
                try {
                    mutableClass.replaceMethod(
                        method,
                        registerCount = 2,
                        smaliCode = """
                        const/4 v0, 0x0
                        return v0
                        """.trimIndent()
                    )
                    hookedPoints++
                    logger.fine("[CapCut Ads] Hooked promo/ad check: ${classDef.type}->${method.name}")
                } catch (e: Exception) {
                    logger.warning("[CapCut Ads] Failed to hook ${classDef.type}->${method.name}: ${e.message}")
                }
            }

            // 3. Paywall / Ad show methods returning void
            if (retType == "V" && (
                mName == "showpaywall" || mName == "showupgradeprompt" ||
                mName == "showvipdialog" || mName == "showinterstitialad"
            )) {
                try {
                    mutableClass.replaceMethod(
                        method,
                        registerCount = if (isStatic) 1 else 2,
                        smaliCode = """
                        return-void
                        """.trimIndent()
                    )
                    hookedPoints++
                    logger.fine("[CapCut Ads] Neutralized paywall show method: ${classDef.type}->${method.name}")
                } catch (e: Exception) {
                    logger.warning("[CapCut Ads] Failed to neutralize ${classDef.type}->${method.name}: ${e.message}")
                }
            }
        }
    }

    logger.info("[CapCut Ads/Watermark] Total hooks applied: $hookedPoints")
}

package com.dmoniak.patches.movix

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.AccessFlags
import com.dmoniak.patches.hungryshark.util.findMutableMethodOf
import com.dmoniak.patches.shared.Constants.COMPATIBILITY_MOVIX
import java.util.logging.Logger

@Suppress("unused")
val movixBlockAdsDeclutterPatch = bytecodePatch(
    name = "Block Video Ads & Interstitials - Movix (Experimental)",
    description = "⚠️ [En cours de développement / Non testé] Strips video pre-roll and mid-roll ads, banner ads, and redirect popups across Movix movie and series player screens.",
) {
    compatibleWith(COMPATIBILITY_MOVIX)

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        executeMovixBlockAdsDeclutterLogic(logger)
    }
}

fun BytecodePatchContext.executeMovixBlockAdsDeclutterLogic(logger: Logger) {
    logger.info("Executing Block Video Ads & Interstitials patch for Movix...")
    var hookedPoints = 0

    classDefForEach { classDef ->
        val tl = classDef.type.lowercase()
        if (tl.contains("androidx") || tl.contains("android/support")) return@classDefForEach

        val mutableClass by lazy { mutableClassDefBy(classDef) }

        // 1. Hook RNCWebChromeClient: neutralize onCreateWindow (blocks all window.open popups, popunders and ad tabs)
        if (classDef.type.contains("RNCWebChromeClient")) {
            for (method in classDef.methods.toList()) {
                if (method.implementation == null) continue
                if (method.name == "onCreateWindow" && method.returnType == "Z") {
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
                        logger.info("[Movix Ads] Neutralized popup window creation in: ${classDef.type}->${method.name}")
                    } catch (e: Exception) {
                        logger.warning("[Movix Ads] Failed to hook onCreateWindow: ${e.message}")
                    }
                }
            }
        }

        // 2. Hook RNCWebViewManager / RNCWebViewManagerImpl: disable automatic window opening and multiple windows in WebSettings
        if (classDef.type.contains("RNCWebViewManager")) {
            for (method in classDef.methods.toList()) {
                if (method.implementation == null) continue
                val mName = method.name
                if ((mName == "setJavaScriptCanOpenWindowsAutomatically" || mName == "setSetSupportMultipleWindows") && method.returnType == "V") {
                    try {
                        val mutableMethod = mutableClass.findMutableMethodOf(method)
                        mutableMethod.addInstructions(
                            0,
                            """
                            return-void
                            """.trimIndent()
                        )
                        hookedPoints++
                        logger.info("[Movix Ads] Disabled window opening capability in: ${classDef.type}->${method.name}")
                    } catch (e: Exception) {
                        logger.warning("[Movix Ads] Failed to hook ${method.name}: ${e.message}")
                    }
                }
            }
        }

        // 3. Hook RNCWebViewClient: inject anti-ad stylesheet and window.open neutralizing script on page load
        if (classDef.type.contains("RNCWebViewClient")) {
            for (method in classDef.methods.toList()) {
                if (method.implementation == null) continue
                if (method.name == "onPageFinished" && method.returnType == "V" && method.parameterTypes.size == 2) {
                    try {
                        val mutableMethod = mutableClass.findMutableMethodOf(method)
                        mutableMethod.addInstructions(
                            0,
                            """
                            const-string v0, "javascript:(function(){ try { window.open=function(){return null;}; window.alert=function(){}; var s=document.createElement('style'); s.innerHTML='.ad-banner, .banner-ad, [class*=\"ad-\"], [id*=\"ad-\"], [class*=\"sponsor\"], [class*=\"popup\"], iframe[src*=\"ad\"], [id*=\"pop\"], .popunder, #popunder { display: none !important; opacity: 0 !important; pointer-events: none !important; visibility: hidden !important; width: 0 !important; height: 0 !important; }'; (document.head||document.documentElement).appendChild(s); } catch(e){} })();"
                            invoke-virtual {p1, v0}, Landroid/webkit/WebView;->loadUrl(Ljava/lang/String;)V
                            """.trimIndent()
                        )
                        hookedPoints++
                        logger.info("[Movix Ads] Injected anti-ad script in: ${classDef.type}->${method.name}")
                    } catch (e: Exception) {
                        logger.warning("[Movix Ads] Failed to hook onPageFinished: ${e.message}")
                    }
                }
            }
        }

        // 4. Universal ad/redirect flags for any auxiliary classes
        for (method in classDef.methods.toList()) {
            if (method.implementation == null) continue
            val isStatic = AccessFlags.STATIC.isSet(method.accessFlags)
            val mName = method.name.lowercase()
            val retType = method.returnType

            if (!isStatic && (
                mName == "shouldplayvideoad" ||
                mName == "isadvisible" ||
                mName == "shouldopenadredirect" ||
                mName == "isinterstitialadloaded" ||
                mName == "hasprerollad"
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
                    logger.info("[Movix Ads] Blocked ad/redirect flag in: ${classDef.type}->${method.name}")
                } catch (e: Exception) {
                    logger.warning("[Movix Ads] Failed to hook ${method.name}: ${e.message}")
                }
            }
        }
    }
    logger.info("[Movix Block Ads] Total hooks applied: $hookedPoints")
}

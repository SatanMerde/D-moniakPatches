package com.dmoniak.patches.movix

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.AccessFlags
import com.dmoniak.patches.hungryshark.util.findMutableMethodOf
import com.dmoniak.patches.shared.Constants.COMPATIBILITY_MOVIX
import java.util.logging.Logger

@Suppress("unused")
val movixBypassDownloadRestrictionsPatch = bytecodePatch(
    name = "Bypass Download Restrictions - Movix",
    description = "Bypasses offline download tier restrictions to allow downloading any movie or series episode for offline viewing without a Premium subscription, and removes download quality caps.",
) {
    compatibleWith(COMPATIBILITY_MOVIX)

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        executeMovixBypassDownloadRestrictionsLogic(logger)
    }
}

fun BytecodePatchContext.executeMovixBypassDownloadRestrictionsLogic(logger: Logger) {
    logger.info("Executing Bypass Download Restrictions patch for Movix...")
    var hookedPoints = 0

    val downloadScript = "(function(){ try { localStorage.setItem('offline_download_unlocked', 'true'); localStorage.setItem('allow_unlimited_downloads', 'true'); window.canDownload=true; } catch(e){} })();"

    classDefForEach { classDef ->
        val tl = classDef.type.lowercase()
        if (tl.contains("androidx") || tl.contains("android/support")) return@classDefForEach

        val mutableClass by lazy { mutableClassDefBy(classDef) }

        // 1. Hook RNCWebView (React Native core WebView)
        if (classDef.type.contains("RNCWebView") && !classDef.type.contains("Manager") && !classDef.type.contains("Client")) {
            for (method in classDef.methods.toList()) {
                if (method.implementation == null) continue
                if ((method.name == "callInjectedJavaScript" || method.name == "callInjectedJavaScriptBeforeContentLoaded") && method.returnType == "V" && method.parameterTypes.isEmpty()) {
                    try {
                        val mutableMethod = mutableClass.findMutableMethodOf(method)
                        mutableMethod.addInstructions(
                            0,
                            """
                            const-string v0, "$downloadScript"
                            invoke-virtual {p0, v0}, Lcom/reactnativecommunity/webview/RNCWebView;->evaluateJavascriptWithFallback(Ljava/lang/String;)V
                            """.trimIndent()
                        )
                        hookedPoints++
                        logger.info("[Movix Download] Injected download permissions in: ${classDef.type}->${method.name}")
                    } catch (e: Exception) {
                        logger.warning("[Movix Download] Failed to hook ${method.name}: ${e.message}")
                    }
                }
            }
        }

        // 2. Hook RNCWebViewManager to prepend download settings before page loads
        if (classDef.type.contains("RNCWebViewManager")) {
            for (method in classDef.methods.toList()) {
                if (method.implementation == null) continue
                val mName = method.name
                if (mName == "setInjectedJavaScriptBeforeContentLoaded" && method.parameterTypes.size == 2 && method.returnType == "V") {
                    try {
                        val mutableMethod = mutableClass.findMutableMethodOf(method)
                        mutableMethod.addInstructions(
                            0,
                            """
                            const-string v0, "$downloadScript;\n"
                            if-nez p2, :cond_skip_prepend_dl
                            invoke-virtual {v0, p2}, Ljava/lang/String;->concat(Ljava/lang/String;)Ljava/lang/String;
                            move-result-object p2
                            goto :cond_done_prepend_dl
                            :cond_skip_prepend_dl
                            move-object p2, v0
                            :cond_done_prepend_dl
                            """.trimIndent()
                        )
                        hookedPoints++
                        logger.info("[Movix Download] Prepended download permissions in: ${classDef.type}->${method.name}")
                    } catch (e: Exception) {
                        logger.warning("[Movix Download] Failed to hook setInjectedJavaScriptBeforeContentLoaded: ${e.message}")
                    }
                }
            }
        }

        for (method in classDef.methods.toList()) {
            if (method.implementation == null) continue
            val isStatic = AccessFlags.STATIC.isSet(method.accessFlags)
            val mName = method.name.lowercase()
            val retType = method.returnType

            if (!isStatic && (
                mName == "candownloadoffline" ||
                mName == "isofflinedownloadallowed" ||
                mName == "hasdownloadpermission" ||
                mName == "isdownloadsupportedfortier"
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
                    logger.info("[Movix Download] Unlocked download in: ${classDef.type}->${method.name}")
                } catch (e: Exception) {
                    logger.warning("[Movix Download] Failed to hook ${method.name}: ${e.message}")
                }
            }
        }
    }
    logger.info("[Movix Bypass Download] Total hooks applied: $hookedPoints")
}

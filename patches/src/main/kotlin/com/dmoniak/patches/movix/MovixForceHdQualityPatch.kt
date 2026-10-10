package com.dmoniak.patches.movix

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.AccessFlags
import com.dmoniak.patches.hungryshark.util.findMutableMethodOf
import com.dmoniak.patches.shared.Constants.COMPATIBILITY_MOVIX
import java.util.logging.Logger

@Suppress("unused")
val movixForceHdQualityPatch = bytecodePatch(
    name = "Force HD & 4K Quality Unlock - Movix",
    description = "Forces maximum available video quality (HD 1080p / 4K) on all content regardless of subscription tier or network quality detection thresholds.",
) {
    compatibleWith(COMPATIBILITY_MOVIX)

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        executeMovixForceHdQualityLogic(logger)
    }
}

fun BytecodePatchContext.executeMovixForceHdQualityLogic(logger: Logger) {
    logger.info("Executing Force HD & 4K Quality patch for Movix...")
    var hookedPoints = 0

    val hdScript = "(function(){ try { localStorage.setItem('preferred_quality', '1080p'); localStorage.setItem('stream_quality', 'max'); localStorage.setItem('auto_select_highest', 'true'); } catch(e){} })();"

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
                            const-string v0, "$hdScript"
                            invoke-virtual {p0, v0}, Lcom/reactnativecommunity/webview/RNCWebView;->evaluateJavascriptWithFallback(Ljava/lang/String;)V
                            """.trimIndent()
                        )
                        hookedPoints++
                        logger.info("[Movix HD] Injected quality preferences in: ${classDef.type}->${method.name}")
                    } catch (e: Exception) {
                        logger.warning("[Movix HD] Failed to hook ${method.name}: ${e.message}")
                    }
                }
            }
        }

        // 2. Hook RNCWebViewManager to prepend quality settings before page loads
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
                            const-string v0, "$hdScript;\n"
                            if-nez p2, :cond_skip_prepend_hd
                            invoke-virtual {v0, p2}, Ljava/lang/String;->concat(Ljava/lang/String;)Ljava/lang/String;
                            move-result-object p2
                            goto :cond_done_prepend_hd
                            :cond_skip_prepend_hd
                            move-object p2, v0
                            :cond_done_prepend_hd
                            """.trimIndent()
                        )
                        hookedPoints++
                        logger.info("[Movix HD] Prepended quality preferences in: ${classDef.type}->${method.name}")
                    } catch (e: Exception) {
                        logger.warning("[Movix HD] Failed to hook setInjectedJavaScriptBeforeContentLoaded: ${e.message}")
                    }
                }
            }
        }

        for (method in classDef.methods.toList()) {
            if (method.implementation == null) continue
            val isStatic = AccessFlags.STATIC.isSet(method.accessFlags)
            val mName = method.name.lowercase()
            val retType = method.returnType

            // Block quality cap enforcement
            if (!isStatic && (
                mName == "ishdqualityrestricted" ||
                mName == "should4kqualitybeblocked" ||
                mName == "isqualitylockedbytier"
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
                    logger.info("[Movix HD] Removed quality restriction in: ${classDef.type}->${method.name}")
                } catch (e: Exception) {
                    logger.warning("[Movix HD] Failed to hook ${method.name}: ${e.message}")
                }
            }

            // Unlock 4K / HD quality tier
            if (!isStatic && (
                mName == "canstream4k" ||
                mName == "canstreamhd" ||
                mName == "isultrahdstreamingallowed"
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
                    logger.info("[Movix HD] Unlocked HD/4K tier in: ${classDef.type}->${method.name}")
                } catch (e: Exception) {
                    logger.warning("[Movix HD] Failed to hook ${method.name}: ${e.message}")
                }
            }
        }
    }
    logger.info("[Movix Force HD Quality] Total hooks applied: $hookedPoints")
}

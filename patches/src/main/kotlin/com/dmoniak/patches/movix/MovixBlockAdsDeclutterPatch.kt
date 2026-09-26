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

    // Master script:
    // 1. Locks localStorage with VIP status + sets ad popup mode to 'auto' (which tells React AdFreePlayerAds to never render)
    // 2. Mocks /api/check-vip requests to prevent server from revoking VIP
    // 3. Neutralizes window.open and window.alert
    // 4. Dispatches storage, vipStatusChanged, and ad_popup_accepted events so the video player loads immediately
    // 5. Continually detects and auto-dismisses any 'Une pub et c'est parti' / Radix dialog elements
    // 6. Injects CSS suppression for ad frames and overlays
    val antiAdScript = "javascript:(function(){ try { " +
        "if (typeof Storage !== 'undefined') { try { " +
        "var g = Storage.prototype.getItem; " +
        "Storage.prototype.getItem = function(k) { " +
        "if (k === 'is_vip') return 'true'; " +
        "if (k === 'settings_ad_popup_mode') return 'auto'; " +
        "if (k === 'access_code') return 'VIP_LIFETIME_BYPASS'; " +
        "if (k === 'access_code_expires') return '2099-12-31T23:59:59.999Z'; " +
        "return g.apply(this, arguments); " +
        "}; " +
        "var s = Storage.prototype.setItem; " +
        "Storage.prototype.setItem = function(k, v) { " +
        "if (k === 'is_vip' && v === 'false') return s.call(this, k, 'true'); " +
        "if (k === 'settings_ad_popup_mode' && v !== 'auto') return s.call(this, k, 'auto'); " +
        "return s.apply(this, arguments); " +
        "}; " +
        "var r = Storage.prototype.removeItem; " +
        "Storage.prototype.removeItem = function(k) { " +
        "if (k === 'is_vip' || k === 'settings_ad_popup_mode' || k === 'access_code' || k === 'access_code_expires') return; " +
        "return r.apply(this, arguments); " +
        "}; " +
        "} catch(e) {} } " +
        "try { " +
        "localStorage.setItem('is_vip', 'true'); " +
        "localStorage.setItem('settings_ad_popup_mode', 'auto'); " +
        "localStorage.setItem('access_code', 'VIP_LIFETIME_BYPASS'); " +
        "localStorage.setItem('access_code_expires', '2099-12-31T23:59:59.999Z'); " +
        "} catch(e) {} " +
        "try { " +
        "if (typeof window !== 'undefined' && typeof window.fetch === 'function' && !window._movixFetchHooked) { " +
        "window._movixFetchHooked = true; " +
        "var origFetch = window.fetch; " +
        "window.fetch = function(url, opts) { " +
        "var u = String(url || ''); " +
        "if (u.indexOf('check-vip') !== -1) { " +
        "return Promise.resolve(new Response(JSON.stringify({ vip: true, expiresAt: '2099-12-31T23:59:59.999Z' }), { status: 200, headers: { 'Content-Type': 'application/json' } })); " +
        "} " +
        "return origFetch.apply(this, arguments); " +
        "}; " +
        "} } catch(e) {} " +
        "if (typeof window !== 'undefined') { " +
        "window.isVip = true; window.hasVipAccess = true; " +
        "window.open = function() { return null; }; window.alert = function() {}; " +
        "} " +
        "var fire = function() { try { " +
        "if (typeof window !== 'undefined') { " +
        "window.dispatchEvent(new Event('storage')); " +
        "window.dispatchEvent(new CustomEvent('vipStatusChanged', { detail: { vip: true } })); " +
        "window.dispatchEvent(new CustomEvent('ad_popup_accepted', { detail: { timestamp: Date.now() } })); " +
        "window.dispatchEvent(new CustomEvent('ad_popup_mode_changed', { detail: { mode: 'auto' } })); " +
        "} } catch(e) {} }; " +
        "fire(); " +
        "var kill = function() { try { " +
        "if (typeof document === 'undefined') return; " +
        "var btns = document.querySelectorAll('[data-ad-view-button]'); " +
        "for (var b = 0; b < btns.length; b++) { try { btns[b].click(); } catch(e) {} } " +
        "var targets = ['Une pub et c', 'Voir une publici', 'Passe VIP', 'garde Movix', 'Marre des pubs']; " +
        "var dialogs = document.querySelectorAll('[role=dialog], [data-radix-portal], .fixed.inset-0.z-50, div[style*=\\\"rgba(59,130,246\\\"], div[style*=\\\"rgba(59, 130, 246\\\"]'); " +
        "for (var d = 0; d < dialogs.length; d++) { " +
        "var el = dialogs[d]; var txt = el.textContent || ''; " +
        "for (var t = 0; t < targets.length; t++) { " +
        "if (txt.indexOf(targets[t]) !== -1) { " +
        "var btn = el.querySelector('button'); " +
        "if (btn) { try { btn.click(); } catch(e) {} } " +
        "fire(); " +
        "try { el.remove(); } catch(e) {} " +
        "break; " +
        "} } } " +
        "} catch(e) {} }; " +
        "kill(); " +
        "if (typeof window !== 'undefined' && !window._movixKillInterval) { " +
        "window._movixKillInterval = setInterval(kill, 100); " +
        "} " +
        "try { " +
        "if (typeof document !== 'undefined' && !document.getElementById('movix-ad-shield-style')) { " +
        "var st = document.createElement('style'); st.id = 'movix-ad-shield-style'; " +
        "st.innerHTML = 'div[style*=\\\"rgba(59,130,246\\\"], div[style*=\\\"rgba(59, 130, 246\\\"], .ad-banner, .banner-ad, [class*=\\\"ad-\\\"], [id*=\\\"ad-\\\"], [class*=\\\"sponsor\\\"], [class*=\\\"popup\\\"], iframe[src*=\\\"ad\\\"], [id*=\\\"pop\\\"], .popunder, #popunder { display: none !important; opacity: 0 !important; pointer-events: none !important; visibility: hidden !important; width: 0 !important; height: 0 !important; }'; " +
        "(document.head || document.documentElement).appendChild(st); " +
        "} } catch(e) {} " +
        "} catch(err) {} })();"

    classDefForEach { classDef ->
        val tl = classDef.type.lowercase()
        if (tl.contains("androidx") || tl.contains("android/support")) return@classDefForEach

        val mutableClass by lazy { mutableClassDefBy(classDef) }

        // 1. Hook RNCWebChromeClient:
        // - onCreateWindow: return false to prevent ANY popup/popunder window creation
        // - onProgressChanged: inject anti-ad script on every loading progress tick
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
                        logger.info("[Movix Ads] Blocked popup window creation in: ${classDef.type}->${method.name}")
                    } catch (e: Exception) {
                        logger.warning("[Movix Ads] Failed to hook onCreateWindow: ${e.message}")
                    }
                }
                if (method.name == "onProgressChanged" && method.returnType == "V" && method.parameterTypes.size == 2) {
                    try {
                        val mutableMethod = mutableClass.findMutableMethodOf(method)
                        mutableMethod.addInstructions(
                            0,
                            """
                            const-string v0, "$antiAdScript"
                            invoke-virtual {p1, v0}, Landroid/webkit/WebView;->loadUrl(Ljava/lang/String;)V
                            """.trimIndent()
                        )
                        hookedPoints++
                        logger.info("[Movix Ads] Injected anti-ad script onProgressChanged in: ${classDef.type}->${method.name}")
                    } catch (e: Exception) {
                        logger.warning("[Movix Ads] Failed to hook onProgressChanged: ${e.message}")
                    }
                }
            }
        }

        // 2. Hook RNCWebView (Core React Native WebView class):
        // - callInjectedJavaScript: inject master anti-ad script after page load
        // - callInjectedJavaScriptBeforeContentLoaded: inject early before HTML content renders
        if (classDef.type.contains("RNCWebView") && !classDef.type.contains("Manager") && !classDef.type.contains("Client")) {
            for (method in classDef.methods.toList()) {
                if (method.implementation == null) continue
                if (method.name == "callInjectedJavaScript" && method.returnType == "V" && method.parameterTypes.isEmpty()) {
                    try {
                        val mutableMethod = mutableClass.findMutableMethodOf(method)
                        mutableMethod.addInstructions(
                            0,
                            """
                            const-string v0, "$antiAdScript"
                            invoke-virtual {p0, v0}, Landroid/webkit/WebView;->loadUrl(Ljava/lang/String;)V
                            """.trimIndent()
                        )
                        hookedPoints++
                        logger.info("[Movix Ads] Injected anti-ad script in callInjectedJavaScript: ${classDef.type}->${method.name}")
                    } catch (e: Exception) {
                        logger.warning("[Movix Ads] Failed to hook callInjectedJavaScript: ${e.message}")
                    }
                }
                if (method.name == "callInjectedJavaScriptBeforeContentLoaded" && method.returnType == "V" && method.parameterTypes.isEmpty()) {
                    try {
                        val mutableMethod = mutableClass.findMutableMethodOf(method)
                        mutableMethod.addInstructions(
                            0,
                            """
                            const-string v0, "$antiAdScript"
                            invoke-virtual {p0, v0}, Landroid/webkit/WebView;->loadUrl(Ljava/lang/String;)V
                            """.trimIndent()
                        )
                        hookedPoints++
                        logger.info("[Movix Ads] Injected anti-ad script in callInjectedJavaScriptBeforeContentLoaded: ${classDef.type}->${method.name}")
                    } catch (e: Exception) {
                        logger.warning("[Movix Ads] Failed to hook callInjectedJavaScriptBeforeContentLoaded: ${e.message}")
                    }
                }
            }
        }

        // 3. Hook RNCWebViewClient:
        // - onPageFinished: inject late script to ensure completed DOM is cleaned
        if (classDef.type.contains("RNCWebViewClient")) {
            for (method in classDef.methods.toList()) {
                if (method.implementation == null) continue
                if (method.name == "onPageFinished" && method.returnType == "V" && method.parameterTypes.size == 2) {
                    try {
                        val mutableMethod = mutableClass.findMutableMethodOf(method)
                        mutableMethod.addInstructions(
                            0,
                            """
                            const-string v0, "$antiAdScript"
                            invoke-virtual {p1, v0}, Landroid/webkit/WebView;->loadUrl(Ljava/lang/String;)V
                            """.trimIndent()
                        )
                        hookedPoints++
                        logger.info("[Movix Ads] Injected anti-ad script onPageFinished in: ${classDef.type}->${method.name}")
                    } catch (e: Exception) {
                        logger.warning("[Movix Ads] Failed to hook onPageFinished: ${e.message}")
                    }
                }
            }
        }

        // 4. Hook RNCWebViewManager / RNCWebViewManagerImpl: disable automatic window opening and multiple windows in WebSettings
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

        // 5. Auxiliary ad/redirect flags
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

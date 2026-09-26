package com.dmoniak.patches.vpnlat

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.AccessFlags
import com.dmoniak.patches.hungryshark.util.findMutableMethodOf
import com.dmoniak.patches.shared.Constants.COMPATIBILITY_VPN_LAT
import java.util.logging.Logger

@Suppress("unused")
val vpnLatBlockAdsPatch = bytecodePatch(
    name = "Block Ads & Video Interruptions - VPN.lat (Experimental)",
    description = "⚠️ [En cours de développement / Non testé] Strips interstitial video ads before server connection, bottom banners, and promotional popups in VPN.lat.",
) {
    compatibleWith(COMPATIBILITY_VPN_LAT)

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        executeVpnLatBlockAdsLogic(logger)
    }
}

fun BytecodePatchContext.executeVpnLatBlockAdsLogic(logger: Logger) {
    logger.info("Executing Block Ads patch for VPN.lat...")
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
                mName == "isbannerloaded" ||
                mName == "hasvideoad"
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
                    logger.info("[VPN.lat Ads] Neutralized ad check in: ${classDef.type}->${method.name}")
                } catch (e: Exception) {
                    logger.warning("[VPN.lat Ads] Failed to hook ${method.name}: ${e.message}")
                }
            }

            if (!isStatic && (
                mName == "showinterstitial" ||
                mName == "showvideo" ||
                mName == "showad" ||
                mName == "showbanner"
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
                    logger.info("[VPN.lat Ads] Neutralized ad display in: ${classDef.type}->${method.name}")
                } catch (e: Exception) {
                    logger.warning("[VPN.lat Ads] Failed to hook ${method.name}: ${e.message}")
                }
            }
        }
    }
    logger.info("[VPN.lat Ads] Total ad-blocking hooks applied: $hookedPoints")
}

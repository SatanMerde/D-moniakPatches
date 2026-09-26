package com.dmoniak.patches.speedtest

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.AccessFlags
import com.dmoniak.patches.hungryshark.util.findMutableMethodOf
import com.dmoniak.patches.shared.Constants.COMPATIBILITY_SPEEDTEST
import java.util.logging.Logger

@Suppress("unused")
val speedtestPremiumVpnPatch = bytecodePatch(
    name = "Premium VPN & Unlimited Data - Speedtest by Ookla (Experimental)",
    description = "⚠️ [En cours de développement / Non testé] Unlocks unlimited Speedtest VPN bandwidth indicators, bypasses the monthly 2GB free quota limit, and enables full server location selection.",
) {
    compatibleWith(COMPATIBILITY_SPEEDTEST)

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        executeSpeedtestPremiumVpnLogic(logger)
    }
}

fun BytecodePatchContext.executeSpeedtestPremiumVpnLogic(logger: Logger) {
    logger.info("Executing Premium VPN patch for Speedtest by Ookla...")
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

            // 1. Hook VPN subscription & unlimited state getters
            if (!isStatic && (
                mName == "isvpnpremium" ||
                mName == "hasunlimitedvpn" ||
                mName == "isvpnsubscribed" ||
                mName == "ispremiumsubscriber"
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
                    logger.info("[Speedtest VPN] Enforced premium VPN status: ${classDef.type}->${method.name}")
                } catch (e: Exception) {
                    logger.warning("[Speedtest VPN] Failed to hook ${method.name}: ${e.message}")
                }
            }

            // 2. Disable VPN quota reached / monthly limit flags
            if (!isStatic && (
                mName == "hasreachedvpnlimit" ||
                mName == "isvpnquotalimited" ||
                mName == "isoverbandwidthlimit"
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
                    logger.info("[Speedtest VPN] Disabled VPN quota limit: ${classDef.type}->${method.name}")
                } catch (e: Exception) {
                    logger.warning("[Speedtest VPN] Failed to hook ${method.name}: ${e.message}")
                }
            }
        }
    }
    logger.info("[Speedtest VPN] Total premium VPN hooks applied: $hookedPoints")
}

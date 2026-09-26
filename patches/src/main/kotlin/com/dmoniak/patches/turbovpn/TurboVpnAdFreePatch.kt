package com.dmoniak.patches.turbovpn

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.AccessFlags
import com.dmoniak.patches.hungryshark.util.findMutableMethodOf
import com.dmoniak.patches.shared.Constants.COMPATIBILITY_TURBO_VPN
import java.util.logging.Logger

@Suppress("unused")
val turboVpnAdFreePatch = bytecodePatch(
    name = "Ad-Free Turbo VPN (Experimental)",
    description = "⚠️ [En cours de développement / Non testé] Removes full-screen video ads on connect and disconnect, banner ads at the bottom of the interface, and intrusive interstitial popups.",
) {
    compatibleWith(COMPATIBILITY_TURBO_VPN)

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        executeTurboVpnAdFreeLogic(logger)
    }
}

fun BytecodePatchContext.executeTurboVpnAdFreeLogic(logger: Logger) {
    logger.info("Executing Ad-Free patch for Turbo VPN...")
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

            // 1. Hook isAdFree / isVip / hasRemovedAds status to true
            if (!isStatic && (
                mName == "isadfree" ||
                mName == "hasremovedads" ||
                mName == "isvipuser"
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
                    logger.info("[Turbo VPN AdFree] Enforced ad-free status in: ${classDef.type}->${method.name}")
                } catch (e: Exception) {
                    logger.warning("[Turbo VPN AdFree] Failed to hook ${method.name}: ${e.message}")
                }
            }

            // 2. Block interstitial / connection ad triggers
            if (!isStatic && (
                mName == "shouldshowad" ||
                mName == "canloadad" ||
                mName == "shouldshowconnectad" ||
                mName == "shouldshowdisconnectad"
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
                    logger.info("[Turbo VPN AdFree] Disabled ad trigger in: ${classDef.type}->${method.name}")
                } catch (e: Exception) {
                    logger.warning("[Turbo VPN AdFree] Failed to hook ${method.name}: ${e.message}")
                }
            }
        }
    }
    logger.info("[Turbo VPN AdFree] Total ad-free hooks applied: $hookedPoints")
}

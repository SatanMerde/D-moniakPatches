package com.dmoniak.patches.protonvpn

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.AccessFlags
import com.dmoniak.patches.hungryshark.util.findMutableMethodOf
import com.dmoniak.patches.shared.Constants.COMPATIBILITY_PROTON_VPN
import java.util.logging.Logger

@Suppress("unused")
val protonVpnSpeedIndicatorsPatch = bytecodePatch(
    name = "Always Show Latency & Speed - Proton VPN (Experimental)",
    description = "⚠️ [En cours de développement / Non testé] Displays real-time ping latency (ms) and transmission speed indicators on all server cards in the connection list.",
) {
    compatibleWith(COMPATIBILITY_PROTON_VPN)

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        executeProtonVpnSpeedIndicatorsLogic(logger)
    }
}

fun BytecodePatchContext.executeProtonVpnSpeedIndicatorsLogic(logger: Logger) {
    logger.info("Executing Speed & Latency Indicators patch for Proton VPN...")
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

            // Hook latency / speed visibility getters
            if (!isStatic && (
                mName == "isspeedvisible" ||
                mName == "isloadvisible" ||
                mName == "islatencyvisible" ||
                mName == "shouldshowserverping"
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
                    logger.info("[Proton Speed] Enabled latency indicator in: ${classDef.type}->${method.name}")
                } catch (e: Exception) {
                    logger.warning("[Proton Speed] Failed to hook ${method.name}: ${e.message}")
                }
            }
        }
    }
    logger.info("[Proton Speed] Total speed/ping indicator hooks applied: $hookedPoints")
}

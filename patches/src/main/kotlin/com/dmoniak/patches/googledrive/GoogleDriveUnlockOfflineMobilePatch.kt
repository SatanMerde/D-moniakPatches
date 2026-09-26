package com.dmoniak.patches.googledrive

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.AccessFlags
import com.dmoniak.patches.hungryshark.util.findMutableMethodOf
import com.dmoniak.patches.shared.Constants.COMPATIBILITY_GOOGLE_DRIVE
import java.util.logging.Logger

@Suppress("unused")
val googleDriveUnlockOfflineMobilePatch = bytecodePatch(
    name = "Unlock Offline Mode & Sync on Mobile Data - Google Drive",
    description = "Forces Google Drive offline synchronization, file downloads, and backup transfers to operate freely over mobile cellular data by bypassing Wi-Fi-only network restrictions and metered connection blocks.",
) {
    compatibleWith(COMPATIBILITY_GOOGLE_DRIVE)

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        executeGoogleDriveUnlockOfflineMobileLogic(logger)
    }
}

fun BytecodePatchContext.executeGoogleDriveUnlockOfflineMobileLogic(logger: Logger) {
    logger.info("Executing Unlock Offline Mode & Sync on Mobile Data patch for Google Drive...")
    var hookedPoints = 0

    classDefForEach { classDef ->
        val type = classDef.type
        val tl = type.lowercase()
        if (tl.contains("androidx") || tl.contains("android/support")) return@classDefForEach

        val mutableClass by lazy { mutableClassDefBy(classDef) }

        for (method in classDef.methods.toList()) {
            if (method.implementation == null) continue
            val isStatic = AccessFlags.STATIC.isSet(method.accessFlags)
            val mName = method.name.lowercase()
            val retType = method.returnType

            // 1. Wi-Fi only restriction flags -> false (disabled)
            if (!isStatic && (
                mName == "iswifionlysyncenforced" ||
                mName == "iswifionly" ||
                mName == "shouldpauseforwifi" ||
                mName == "iswaitingforwifi" ||
                mName == "isnetworkmeteredforupload" ||
                mName == "requirewififoroffline" ||
                mName == "iscellularblocked"
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
                    logger.info("[Drive Offline] Disabled Wi-Fi restriction in: ${type}->${method.name}")
                } catch (e: Exception) {
                    logger.warning("[Drive Offline] Failed to hook ${method.name}: ${e.message}")
                }
            }

            // 2. Mobile data permitted flags -> true (enabled)
            if (!isStatic && (
                mName == "canusecellularnetwork" ||
                mName == "iscellularallowed" ||
                mName == "isofflinesyncpermittedonmobiledata" ||
                mName == "canperformdatasync"
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
                    logger.info("[Drive Offline] Enabled mobile data sync in: ${type}->${method.name}")
                } catch (e: Exception) {
                    logger.warning("[Drive Offline] Failed to hook ${method.name}: ${e.message}")
                }
            }
        }
    }
    logger.info("[Google Drive Offline Mobile] Total network hooks applied: $hookedPoints")
}

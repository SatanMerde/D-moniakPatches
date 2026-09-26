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
    name = "Unlock Offline Mode Without WiFi - Google Drive (Experimental)",
    description = "⚠️ [En cours de développement / Non testé] Forces Google Drive offline sync and file access to work on mobile data connections by bypassing the WiFi-only restriction enforced during offline content synchronization.",
) {
    compatibleWith(COMPATIBILITY_GOOGLE_DRIVE)

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        executeGoogleDriveUnlockOfflineMobileLogic(logger)
    }
}

fun BytecodePatchContext.executeGoogleDriveUnlockOfflineMobileLogic(logger: Logger) {
    logger.info("Executing Unlock Offline Mode Without WiFi patch for Google Drive...")
    var hookedPoints = 0

    classDefForEach { classDef ->
        val tl = classDef.type.lowercase()
        if (tl.contains("androidx") || tl.contains("android/support")) return@classDefForEach

        val mutableClass by lazy { mutableClassDefBy(classDef) }

        for (method in classDef.methods.toList()) {
            if (method.implementation == null) continue
            val isStatic = AccessFlags.STATIC.isSet(method.accessFlags)
            val mName = method.name.lowercase()
            val retType = method.returnType

            // Remove WiFi-only restriction for offline sync
            if (!isStatic && (
                mName == "iswifionlysyncenforced" ||
                mName == "shouldresyncovercelluardata" ||
                mName == "isofflineavailableonmobiledata" ||
                mName == "requirswififorofflinecontent"
            ) && retType == "Z") {
                try {
                    val mutableMethod = mutableClass.findMutableMethodOf(method)
                    val ret = if (mName == "iswifionlysyncenforced" || mName == "requirswififorofflinecontent") "0x0" else "0x1"
                    mutableMethod.addInstructions(
                        0,
                        """
                        const/4 v0, $ret
                        return v0
                        """.trimIndent()
                    )
                    hookedPoints++
                    logger.info("[Drive Offline] Patched in: ${classDef.type}->${method.name}")
                } catch (e: Exception) {
                    logger.warning("[Drive Offline] Failed to hook ${method.name}: ${e.message}")
                }
            }
        }
    }
    logger.info("[Google Drive Offline Mobile] Total hooks applied: $hookedPoints")
}

package com.dmoniak.patches.googledrive

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.AccessFlags
import com.dmoniak.patches.hungryshark.util.findMutableMethodOf
import com.dmoniak.patches.shared.Constants.COMPATIBILITY_GOOGLE_DRIVE
import java.util.logging.Logger

@Suppress("unused")
val googleDriveBypassStorageQuotaPatch = bytecodePatch(
    name = "Bypass Storage Quota Alerts - Google Drive (Experimental)",
    description = "⚠️ [En cours de développement / Non testé] Suppresses storage quota threshold alerts, 15 GB full warnings, and upload-blocking dialogs that prevent uploading new files when Google storage is at or near its limit.",
) {
    compatibleWith(COMPATIBILITY_GOOGLE_DRIVE)

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        executeGoogleDriveBypassStorageQuotaLogic(logger)
    }
}

fun BytecodePatchContext.executeGoogleDriveBypassStorageQuotaLogic(logger: Logger) {
    logger.info("Executing Bypass Storage Quota Alerts patch for Google Drive...")
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

            if (!isStatic && (
                mName == "isstoragefull" ||
                mName == "shouldshowstoragelimitwarning" ||
                mName == "isuploadblockedbystoragequota" ||
                mName == "isstoragequotaexceeded" ||
                mName == "shouldblockuploadduetostorage"
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
                    logger.info("[Drive Quota] Bypassed quota alert in: ${classDef.type}->${method.name}")
                } catch (e: Exception) {
                    logger.warning("[Drive Quota] Failed to hook ${method.name}: ${e.message}")
                }
            }
        }
    }
    logger.info("[Google Drive Bypass Quota] Total hooks applied: $hookedPoints")
}

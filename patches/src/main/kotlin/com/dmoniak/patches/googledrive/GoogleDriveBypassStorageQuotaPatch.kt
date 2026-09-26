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
    name = "Bypass Storage Quota Alerts & Upload Blocker - Google Drive",
    description = "Suppresses client-side storage quota warnings, 15 GB full alerts, and bypasses the client upload-blocking mechanism that disables upload buttons when storage is at or near its limit.",
) {
    compatibleWith(COMPATIBILITY_GOOGLE_DRIVE)

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        executeGoogleDriveBypassStorageQuotaLogic(logger)
    }
}

fun BytecodePatchContext.executeGoogleDriveBypassStorageQuotaLogic(logger: Logger) {
    logger.info("Executing Bypass Storage Quota Alerts & Upload Blocker patch for Google Drive...")
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

            // 1. Storage full & upload blocker boolean checks -> false (unblocked)
            if (!isStatic && (
                mName == "isstoragefull" ||
                mName == "shouldshowstoragelimitwarning" ||
                mName == "isuploadblockedbystoragequota" ||
                mName == "isstoragequotaexceeded" ||
                mName == "shouldblockuploadduetostorage" ||
                mName == "hasreachedhardquotalimit" ||
                mName == "isaccountoutofstorage"
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
                    logger.info("[Drive Quota] Bypassed client storage block in: ${type}->${method.name}")
                } catch (e: Exception) {
                    logger.warning("[Drive Quota] Failed to hook ${method.name}: ${e.message}")
                }
            }

            // 2. Client free storage calculation (report abundant free space on client) -> return 1 TB (0x10000000000L bytes)
            if (!isStatic && (
                mName == "getremainingquotabytes" ||
                mName == "getfreestoragebytes" ||
                mName == "getavailablestoragespace"
            ) && retType == "J") {
                try {
                    val mutableMethod = mutableClass.findMutableMethodOf(method)
                    mutableMethod.addInstructions(
                        0,
                        """
                        const-wide v0, 0x0000010000000000L
                        return-wide v0
                        """.trimIndent()
                    )
                    hookedPoints++
                    logger.info("[Drive Quota] Injected abundant client free storage in: ${type}->${method.name}")
                } catch (e: Exception) {
                    logger.warning("[Drive Quota] Failed to hook ${method.name}: ${e.message}")
                }
            }
        }
    }
    logger.info("[Google Drive Bypass Quota] Total quota hooks applied: $hookedPoints")
}

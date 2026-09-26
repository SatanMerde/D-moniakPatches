package com.dmoniak.patches.googledrive

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.AccessFlags
import com.dmoniak.patches.hungryshark.util.findMutableMethodOf
import com.dmoniak.patches.shared.Constants.COMPATIBILITY_GOOGLE_DRIVE
import java.util.logging.Logger

@Suppress("unused")
val googleDriveAllowScreenshotsPatch = bytecodePatch(
    name = "Allow Screenshots & Secure Share - Google Drive (Experimental)",
    description = "⚠️ [En cours de développement / Non testé] Removes Android FLAG_SECURE window restrictions in Google Drive to allow screenshots and screen recording of documents, spreadsheets, and presentation previews.",
) {
    compatibleWith(COMPATIBILITY_GOOGLE_DRIVE)

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        executeGoogleDriveAllowScreenshotsLogic(logger)
    }
}

fun BytecodePatchContext.executeGoogleDriveAllowScreenshotsLogic(logger: Logger) {
    logger.info("Executing Allow Screenshots & Secure Share patch for Google Drive...")
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

            // Block FLAG_SECURE application on document viewer windows
            if (!isStatic && (
                mName == "issecurewindowrequired" ||
                mName == "shouldapplyflagsecure" ||
                mName == "isscreencaptureblocked"
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
                    logger.info("[Drive Screenshots] Removed FLAG_SECURE in: ${classDef.type}->${method.name}")
                } catch (e: Exception) {
                    logger.warning("[Drive Screenshots] Failed to hook ${method.name}: ${e.message}")
                }
            }
        }
    }
    logger.info("[Google Drive Allow Screenshots] Total hooks applied: $hookedPoints")
}

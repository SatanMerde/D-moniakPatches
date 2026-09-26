package com.dmoniak.patches.googledrive

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.AccessFlags
import com.dmoniak.patches.hungryshark.util.findMutableMethodOf
import com.dmoniak.patches.shared.Constants.COMPATIBILITY_GOOGLE_DRIVE
import java.util.logging.Logger

@Suppress("unused")
val googleDriveUnlockPdfEditorSharingPatch = bytecodePatch(
    name = "Unlock PDF Editor & Advanced Sharing - Google Drive (Experimental)",
    description = "⚠️ [En cours de développement / Non testé] Unlocks inline PDF editing, annotation tools, advanced link-sharing permission levels, and extended expiry options for shared files without a Workspace subscription.",
) {
    compatibleWith(COMPATIBILITY_GOOGLE_DRIVE)

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        executeGoogleDriveUnlockPdfEditorSharingLogic(logger)
    }
}

fun BytecodePatchContext.executeGoogleDriveUnlockPdfEditorSharingLogic(logger: Logger) {
    logger.info("Executing Unlock PDF Editor & Advanced Sharing patch for Google Drive...")
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
                mName == "ispdfeditorenabled" ||
                mName == "canusepdfannotations" ||
                mName == "isadvancedsharingenabled" ||
                mName == "cansetsharingexpiry" ||
                mName == "hasworkspacesharingfeatures"
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
                    logger.info("[Drive PDF/Share] Unlocked feature in: ${classDef.type}->${method.name}")
                } catch (e: Exception) {
                    logger.warning("[Drive PDF/Share] Failed to hook ${method.name}: ${e.message}")
                }
            }
        }
    }
    logger.info("[Google Drive PDF Editor & Sharing] Total hooks applied: $hookedPoints")
}

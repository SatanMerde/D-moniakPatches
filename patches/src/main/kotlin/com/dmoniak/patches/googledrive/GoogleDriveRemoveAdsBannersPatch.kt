package com.dmoniak.patches.googledrive

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.AccessFlags
import com.dmoniak.patches.hungryshark.util.findMutableMethodOf
import com.dmoniak.patches.shared.Constants.COMPATIBILITY_GOOGLE_DRIVE
import java.util.logging.Logger

@Suppress("unused")
val googleDriveRemoveAdsBannersPatch = bytecodePatch(
    name = "Remove Ads & Promotional Banners - Google Drive (Experimental)",
    description = "⚠️ [En cours de développement / Non testé] Removes Google One upsell banners, Workspace promotional cards, storage upgrade prompts, and sponsored suggestions from the Drive home feed and search results.",
) {
    compatibleWith(COMPATIBILITY_GOOGLE_DRIVE)

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        executeGoogleDriveRemoveAdsBannersLogic(logger)
    }
}

fun BytecodePatchContext.executeGoogleDriveRemoveAdsBannersLogic(logger: Logger) {
    logger.info("Executing Remove Ads & Promotional Banners patch for Google Drive...")
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
                mName == "shouldshowgoogleonebanner" ||
                mName == "shouldshowworkspacepromo" ||
                mName == "issponsoredcontentvisible" ||
                mName == "shouldshowsuggestedupgradetile" ||
                mName == "isdrivepromotionbannervisible"
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
                    logger.info("[Drive Ads] Removed promo banner in: ${classDef.type}->${method.name}")
                } catch (e: Exception) {
                    logger.warning("[Drive Ads] Failed to hook ${method.name}: ${e.message}")
                }
            }
        }
    }
    logger.info("[Google Drive Remove Ads] Total hooks applied: $hookedPoints")
}

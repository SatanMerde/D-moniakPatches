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
    name = "Remove Ads & Google One Upsell Banners - Google Drive",
    description = "Removes Google One upsell banners, storage upgrade warning cards, Workspace promotional tiles, and sponsored suggestion chips across Google Drive.",
) {
    compatibleWith(COMPATIBILITY_GOOGLE_DRIVE)

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        executeGoogleDriveRemoveAdsBannersLogic(logger)
    }
}

fun BytecodePatchContext.executeGoogleDriveRemoveAdsBannersLogic(logger: Logger) {
    logger.info("Executing Remove Ads & Google One Banners patch for Google Drive...")
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

            // 1. Suppress promo, upsell, and storage banner visibility flags -> false
            if (!isStatic && (
                mName == "shouldshowgoogleonebanner" ||
                mName == "shouldshowstorageupgradebanner" ||
                mName == "isstoragequotabannervisible" ||
                mName == "shouldshowworkspacepromo" ||
                mName == "issponsoredcontentvisible" ||
                mName == "shouldpromptgoogleoneupgrade" ||
                mName == "isoneupselldialogshown" ||
                mName == "isdrivepromotionbannervisible" ||
                mName == "isstoragewarningactive"
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
                    logger.info("[Drive Ads] Suppressed upsell/promo banner in: ${type}->${method.name}")
                } catch (e: Exception) {
                    logger.warning("[Drive Ads] Failed to hook ${method.name}: ${e.message}")
                }
            }

            // 2. Mark account as already having Google One / active plan to permanently silence sales prompts -> true
            if (!isStatic && (
                mName == "hasgoogleone" ||
                mName == "isgoogleonesubscriber" ||
                mName == "haspaidstorageplan" ||
                mName == "ispremiumstorageactive"
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
                    logger.info("[Drive Ads] Forced Google One active status in: ${type}->${method.name}")
                } catch (e: Exception) {
                    logger.warning("[Drive Ads] Failed to hook ${method.name}: ${e.message}")
                }
            }
        }
    }
    logger.info("[Google Drive Remove Ads] Total upsell suppression hooks applied: $hookedPoints")
}

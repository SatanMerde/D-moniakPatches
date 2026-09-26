package com.dmoniak.patches.picturethis

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.AccessFlags
import com.dmoniak.patches.hungryshark.util.findMutableMethodOf
import com.dmoniak.patches.shared.Constants.COMPATIBILITY_PICTURETHIS
import java.util.logging.Logger

@Suppress("unused")
val pictureThisPremiumPatch = bytecodePatch(
    name = "Unlock Premium & Plant Disease Diagnosis - PictureThis (Experimental)",
    description = "⚠️ [En cours de développement / Non testé] Unlocks PictureThis Premium features: unlimited plant identifications, full disease diagnosis, botanist plant care guides, and bypasses startup paywall prompts.",
) {
    compatibleWith(COMPATIBILITY_PICTURETHIS)

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        executePictureThisPremiumLogic(logger)
    }
}

fun BytecodePatchContext.executePictureThisPremiumLogic(logger: Logger) {
    logger.info("Executing Unlock Premium patch for PictureThis...")
    var hookedPoints = 0

    classDefForEach { classDef ->
        val type = classDef.type
        val tl = type.lowercase()
        if (tl.contains("androidx") || tl.contains("android/support") || tl.contains("com/google")) return@classDefForEach

        val mutableClass by lazy { mutableClassDefBy(classDef) }

        for (method in classDef.methods.toList()) {
            if (method.implementation == null) continue
            val isStatic = AccessFlags.STATIC.isSet(method.accessFlags)
            val mName = method.name.lowercase()
            val retType = method.returnType

            // 1. Glority VipInfo / VIP status flags
            if (!isStatic && (
                mName == "isvip" ||
                mName == "ispremium" ||
                mName == "ispro" ||
                mName == "hassubscription" ||
                mName == "canidentifyplant" ||
                mName == "candetectdisease" ||
                mName == "hasunlimitedid"
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
                    logger.info("[PictureThis VIP] Forced VIP in: ${type}->${method.name}")
                } catch (e: Exception) {
                    logger.warning("[PictureThis VIP] Failed to hook ${method.name}: ${e.message}")
                }
            }

            // 2. VIP Type level (typically 1 or 2 for annual/lifetime pro)
            if (!isStatic && (
                mName == "getviptype" ||
                mName == "getvipstatus" ||
                mName == "getmembertype"
            ) && retType == "I") {
                try {
                    val mutableMethod = mutableClass.findMutableMethodOf(method)
                    mutableMethod.addInstructions(
                        0,
                        """
                        const/4 v0, 0x2
                        return v0
                        """.trimIndent()
                    )
                    hookedPoints++
                    logger.info("[PictureThis VIP] Forced VIP tier in: ${type}->${method.name}")
                } catch (e: Exception) {
                    logger.warning("[PictureThis VIP] Failed to hook ${method.name}: ${e.message}")
                }
            }

            // 3. Expiration timestamp (far future: 2099)
            if (!isStatic && (
                mName == "getexpiredate" ||
                mName == "getexpiretime" ||
                mName == "getendtime"
            ) && retType == "J") {
                try {
                    val mutableMethod = mutableClass.findMutableMethodOf(method)
                    mutableMethod.addInstructions(
                        0,
                        """
                        const-wide v0, 0x000003b9aca00000L
                        return-wide v0
                        """.trimIndent()
                    )
                    hookedPoints++
                    logger.info("[PictureThis VIP] Extended expiration in: ${type}->${method.name}")
                } catch (e: Exception) {
                    logger.warning("[PictureThis VIP] Failed to hook ${method.name}: ${e.message}")
                }
            }
        }
    }
    logger.info("[PictureThis VIP] Total VIP hooks applied: $hookedPoints")
}

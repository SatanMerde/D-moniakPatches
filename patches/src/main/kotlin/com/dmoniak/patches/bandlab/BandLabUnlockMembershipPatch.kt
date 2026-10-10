package com.dmoniak.patches.bandlab

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.AccessFlags
import com.dmoniak.patches.hungryshark.util.findMutableMethodOf
import com.dmoniak.patches.shared.Constants.COMPATIBILITY_BANDLAB
import java.util.logging.Logger

@Suppress("unused")
val bandLabUnlockMembershipPatch = bytecodePatch(
    name = "Unlock Creator Membership & Audio Tools - BandLab",
    description = "Hooks Google Play Billing Client and membership state models in BandLab to unlock client-side Creator Membership perks, audio presets, master presets, and removes membership upgrade paywalls.",
) {
    compatibleWith(COMPATIBILITY_BANDLAB)

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        executeBandLabUnlockMembershipLogic(logger)
    }
}

fun BytecodePatchContext.executeBandLabUnlockMembershipLogic(logger: Logger) {
    logger.info("Executing Unlock Creator Membership patch for BandLab...")
    var hookedMethods = 0

    classDefForEach { classDef ->
        val type = classDef.type
        val tl = type.lowercase()

        // Skip framework internals
        if (
            tl.startsWith("landroid/") ||
            tl.startsWith("lkotlin/") ||
            tl.startsWith("ljava/") ||
            tl.startsWith("lcom/google/android/material/")
        ) return@classDefForEach

        val mutableClass by lazy { mutableClassDefBy(classDef) }

        // 1. Hook Google Play BillingClient / in-app purchase validation
        if (type.contains("BillingClient") || type.contains("Billing") || type.contains("Purchase")) {
            for (method in classDef.methods.toList()) {
                if (method.implementation == null) continue
                val mName = method.name
                val retType = method.returnType

                // Purchase.getPurchaseState() -> 1 (PURCHASED)
                if (mName == "getPurchaseState" && retType == "I") {
                    try {
                        val mutableMethod = mutableClass.findMutableMethodOf(method)
                        mutableMethod.addInstructions(
                            0,
                            """
                            const/4 v0, 0x1
                            return v0
                            """.trimIndent()
                        )
                        hookedMethods++
                        logger.info("[BandLab Membership] Hooked Purchase.getPurchaseState() -> 1 (PURCHASED)")
                    } catch (e: Exception) {
                        logger.warning("[BandLab Membership] Failed to hook getPurchaseState: ${e.message}")
                    }
                }

                // Purchase.isAcknowledged() -> true
                if (mName == "isAcknowledged" && retType == "Z") {
                    try {
                        val mutableMethod = mutableClass.findMutableMethodOf(method)
                        mutableMethod.addInstructions(
                            0,
                            """
                            const/4 v0, 0x1
                            return v0
                            """.trimIndent()
                        )
                        hookedMethods++
                        logger.info("[BandLab Membership] Hooked Purchase.isAcknowledged() -> true")
                    } catch (e: Exception) {
                        logger.warning("[BandLab Membership] Failed to hook isAcknowledged: ${e.message}")
                    }
                }
            }
        }

        // 2. Hook BandLab Membership boolean state getters
        for (method in classDef.methods.toList()) {
            if (method.implementation == null) continue
            val isStatic = AccessFlags.STATIC.isSet(method.accessFlags)
            val mName = method.name.lowercase()
            val retType = method.returnType

            // Active membership / Pro status
            if (!isStatic && (
                mName == "ismembershipactive" ||
                mName == "hasactivemembership" ||
                mName == "ismember" ||
                mName == "issubscribed" ||
                mName == "hasmembership" ||
                mName == "iscreatorplus" ||
                mName == "isprosubscriber"
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
                    hookedMethods++
                    logger.info("[BandLab Membership] Unlocked active membership in: ${classDef.type}->${method.name}")
                } catch (e: Exception) {
                    logger.warning("[BandLab Membership] Failed to hook ${method.name}: ${e.message}")
                }
            }

            // Membership paywalls & restrictions
            if (!isStatic && (
                mName == "shouldshowmembershippaywall" ||
                mName == "ismembershiprequired" ||
                mName == "isfeaturelockedbymembership" ||
                mName == "ispresetrestricted"
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
                    hookedMethods++
                    logger.info("[BandLab Membership] Bypassed paywall restriction in: ${classDef.type}->${method.name}")
                } catch (e: Exception) {
                    logger.warning("[BandLab Membership] Failed to hook ${method.name}: ${e.message}")
                }
            }
        }
    }

    logger.info("[BandLab Membership] Total hooks applied: $hookedMethods")
}

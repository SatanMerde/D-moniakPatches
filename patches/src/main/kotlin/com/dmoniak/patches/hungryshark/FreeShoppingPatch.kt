package com.dmoniak.patches.hungryshark

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.AccessFlags
import com.dmoniak.patches.hungryshark.util.findMutableMethodOf
import com.dmoniak.patches.shared.Constants.COMPATIBILITY_HUNGRY_SHARK_WORLD
import java.util.logging.Logger

@Suppress("unused")
val freeShoppingPatch = bytecodePatch(
    name = "Free Shopping",
    description = "Unlocks shop items and in-app purchases in Hungry Shark World by intercepting Google Play Billing transactions for free.",
) {
    compatibleWith(COMPATIBILITY_HUNGRY_SHARK_WORLD)

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        logger.info("Executing Free Shopping / In-App Billing patch for Hungry Shark World...")

        var launchCount = 0

        classDefForEach { classDef ->
            // ONLY inspect Google Play Billing classes - never touch game engine or resource loading classes!
            if (!classDef.type.contains("billingclient")) return@classDefForEach

            val mutableClass by lazy { mutableClassDefBy(classDef) }

            for (method in classDef.methods) {
                // Must be concrete (skip interface/abstract methods)
                if (method.implementation == null) continue

                val isStatic = AccessFlags.STATIC.isSet(method.accessFlags)

                // launchBillingFlow(Activity, BillingFlowParams) -> BillingResult
                if (!isStatic &&
                    method.name == "launchBillingFlow" &&
                    method.returnType == "Lcom/android/billingclient/api/BillingResult;" &&
                    method.parameterTypes.size == 2
                ) {
                    try {
                        val mutableMethod = mutableClass.findMutableMethodOf(method)
                        mutableMethod.addInstructions(
                            0,
                            """
                            invoke-static/range {p0 .. p2}, Lcom/dmoniak/patches/extension/HungrySharkBillingHelper;->handleLaunchBillingFlow(Ljava/lang/Object;Landroid/app/Activity;Ljava/lang/Object;)Ljava/lang/Object;
                            move-result-object p0
                            return-object p0
                            """.trimIndent(),
                        )
                        launchCount++
                    } catch (e: Exception) {
                        logger.warning("Failed to patch launchBillingFlow in ${classDef.type}: ${e.message}")
                    }
                }
            }
        }

        logger.info("Free Shopping results: launchBillingFlow=$launchCount")
        logger.info("Free Shopping patch execution finished.")
    }
}

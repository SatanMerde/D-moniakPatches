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
        var consumeCount = 0
        var acknowledgeCount = 0
        var setListenerCount = 0

        classDefForEach { classDef ->
            val tl = classDef.type.lowercase()
            if (tl.contains("androidx") || tl.contains("android/support")) return@classDefForEach

            val mutableClass by lazy { mutableClassDefBy(classDef) }

            for (method in classDef.methods) {
                // Concrete implementation is required; skip abstract/interface/native methods
                if (method.implementation == null) continue

                val mName = method.name
                val pTypes = method.parameterTypes
                val isStatic = AccessFlags.STATIC.isSet(method.accessFlags)

                // 1. launchBillingFlow(Activity, BillingFlowParams) -> BillingResult
                // Intercepted only on non-static methods of BillingClient implementations
                if (!isStatic &&
                    mName == "launchBillingFlow" &&
                    method.returnType == "Lcom/android/billingclient/api/BillingResult;" &&
                    pTypes.size == 2
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

                // 2. consumeAsync(ConsumeParams, ConsumeResponseListener) -> void
                if (!isStatic &&
                    mName == "consumeAsync" &&
                    method.returnType == "V" &&
                    pTypes.size == 2
                ) {
                    try {
                        val mutableMethod = mutableClass.findMutableMethodOf(method)
                        mutableMethod.addInstructions(
                            0,
                            """
                            invoke-static/range {p0 .. p2}, Lcom/dmoniak/patches/extension/HungrySharkBillingHelper;->handleConsumeAsync(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V
                            return-void
                            """.trimIndent(),
                        )
                        consumeCount++
                    } catch (e: Exception) {
                        logger.warning("Failed to patch consumeAsync in ${classDef.type}: ${e.message}")
                    }
                }

                // 3. acknowledgePurchase(AcknowledgePurchaseParams, AcknowledgePurchaseResponseListener) -> void
                if (!isStatic &&
                    mName == "acknowledgePurchase" &&
                    method.returnType == "V" &&
                    pTypes.size == 2
                ) {
                    try {
                        val mutableMethod = mutableClass.findMutableMethodOf(method)
                        mutableMethod.addInstructions(
                            0,
                            """
                            invoke-static/range {p0 .. p2}, Lcom/dmoniak/patches/extension/HungrySharkBillingHelper;->handleAcknowledgePurchase(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V
                            return-void
                            """.trimIndent(),
                        )
                        acknowledgeCount++
                    } catch (e: Exception) {
                        logger.warning("Failed to patch acknowledgePurchase in ${classDef.type}: ${e.message}")
                    }
                }

                // 4. setListener(PurchasesUpdatedListener) -> Builder
                // Passive listener capture without modifying return or flow
                if (!isStatic &&
                    mName == "setListener" &&
                    pTypes.size == 1 &&
                    pTypes[0] == "Lcom/android/billingclient/api/PurchasesUpdatedListener;"
                ) {
                    try {
                        val mutableMethod = mutableClass.findMutableMethodOf(method)
                        mutableMethod.addInstructions(
                            0,
                            """
                            invoke-static {p1}, Lcom/dmoniak/patches/extension/HungrySharkBillingHelper;->registerPurchasesUpdatedListener(Ljava/lang/Object;)V
                            """.trimIndent(),
                        )
                        setListenerCount++
                    } catch (e: Exception) {
                        logger.warning("Failed to patch setListener in ${classDef.type}: ${e.message}")
                    }
                }
            }
        }

        logger.info("Free Shopping results: launchBillingFlow=$launchCount, consumeAsync=$consumeCount, acknowledgePurchase=$acknowledgeCount, setListener=$setListenerCount")
        logger.info("Free Shopping patch execution finished.")
    }
}

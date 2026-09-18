package com.dmoniak.patches.hungryshark

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction35c
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction3rc
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.dmoniak.patches.hungryshark.util.cloneMethodWithAdditionalRegisters
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

        // 1. Hook Google Play Billing Client implementation
        if (applyBillingClientStrategy(logger)) {
            logger.info("Free Shopping: BillingClient strategy successfully applied.")
        }

        // 2. Hook signature verification methods (Security.verifyPurchase, etc.)
        if (applySecurityVerifyStrategy(logger)) {
            logger.info("Free Shopping: Security verify strategy successfully applied.")
        }

        // 3. Hook Unity Purchasing Google Play bridge
        if (applyUnityPurchasingStrategy(logger)) {
            logger.info("Free Shopping: UnityPurchasing strategy successfully applied.")
        }

        // 4. Global call site scan for launchBillingFlow callsites
        applyBillingCallSiteScanStrategy(logger)

        logger.info("Free Shopping patch execution finished.")
    }
}

/**
 * Strategy for patching Google Play BillingClientImpl methods.
 */
private fun BytecodePatchContext.applyBillingClientStrategy(logger: Logger): Boolean {
    var patched = false

    // 1. launchBillingFlow
    val launchMethod = BillingClientLaunchBillingFlowFingerprint.methodOrNull
        ?: BillingClientLaunchBillingFlowInterfaceFingerprint.methodOrNull

    if (launchMethod != null) {
        try {
            val clonedLaunch = cloneMethodWithAdditionalRegisters(launchMethod, 4)
            clonedLaunch.addInstructions(
                0,
                """
                invoke-static {p0, p1, p2}, Lcom/dmoniak/patches/extension/HungrySharkBillingHelper;->handleLaunchBillingFlow(Ljava/lang/Object;Landroid/app/Activity;Ljava/lang/Object;)Ljava/lang/Object;
                move-result-object v0
                if-eqz v0, :morphe_launch_fallback
                check-cast v0, Lcom/android/billingclient/api/BillingResult;
                return-object v0
                :morphe_launch_fallback
                invoke-static {}, Lcom/android/billingclient/api/BillingResult;->newBuilder()Lcom/android/billingclient/api/BillingResult${'$'}Builder;
                move-result-object v0
                const/4 v1, 0x0
                invoke-virtual {v0, v1}, Lcom/android/billingclient/api/BillingResult${'$'}Builder;->setResponseCode(I)Lcom/android/billingclient/api/BillingResult${'$'}Builder;
                move-result-object v0
                invoke-virtual {v0}, Lcom/android/billingclient/api/BillingResult${'$'}Builder;->build()Lcom/android/billingclient/api/BillingResult;
                move-result-object v0
                return-object v0
                """.trimIndent(),
            )
            logger.info("BillingClient.launchBillingFlow patched.")
            patched = true
        } catch (e: Exception) {
            logger.warning("Failed to patch BillingClient.launchBillingFlow: ${e.message}")
        }
    }

    // 2. isReady
    val readyMethod = BillingClientIsReadyFingerprint.methodOrNull
    if (readyMethod != null) {
        try {
            readyMethod.addInstructions(0, "const/4 v0, 0x1\nreturn v0")
            logger.info("BillingClient.isReady forced to true.")
            patched = true
        } catch (e: Exception) {
            logger.warning("Failed to patch BillingClient.isReady: ${e.message}")
        }
    }

    // 3. consumeAsync (allows repeated purchasing of consumables like gems/coins/pearls)
    val consumeMethod = BillingClientConsumeAsyncFingerprint.methodOrNull
    if (consumeMethod != null) {
        try {
            val clonedConsume = cloneMethodWithAdditionalRegisters(consumeMethod, 2)
            clonedConsume.addInstructions(
                0,
                """
                invoke-static {p0, p1, p2}, Lcom/dmoniak/patches/extension/HungrySharkBillingHelper;->handleConsumeAsync(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V
                return-void
                """.trimIndent(),
            )
            logger.info("BillingClient.consumeAsync patched.")
            patched = true
        } catch (e: Exception) {
            logger.warning("Failed to patch BillingClient.consumeAsync: ${e.message}")
        }
    }

    // 4. acknowledgePurchase
    val acknowledgeMethod = BillingClientAcknowledgePurchaseFingerprint.methodOrNull
    if (acknowledgeMethod != null) {
        try {
            val clonedAcknowledge = cloneMethodWithAdditionalRegisters(acknowledgeMethod, 2)
            clonedAcknowledge.addInstructions(
                0,
                """
                invoke-static {p0, p1, p2}, Lcom/dmoniak/patches/extension/HungrySharkBillingHelper;->handleAcknowledgePurchase(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V
                return-void
                """.trimIndent(),
            )
            logger.info("BillingClient.acknowledgePurchase patched.")
            patched = true
        } catch (e: Exception) {
            logger.warning("Failed to patch BillingClient.acknowledgePurchase: ${e.message}")
        }
    }

    return patched
}

/**
 * Strategy to bypass receipt signature verification checks (Security.verifyPurchase).
 */
private fun BytecodePatchContext.applySecurityVerifyStrategy(logger: Logger): Boolean {
    var patchedCount = 0

    // Fingerprint direct match
    val directVerify = BillingSecurityVerifyPurchaseFingerprint.methodOrNull
    if (directVerify != null) {
        try {
            directVerify.addInstructions(0, "const/4 v0, 0x1\nreturn v0")
            patchedCount++
        } catch (e: Exception) {
            logger.warning("Failed to patch direct verifyPurchase: ${e.message}")
        }
    }

    // ClassDef search for any verifyPurchase methods with signature (String, String, String): boolean
    classDefForEach { classDef ->
        for (method in classDef.methods) {
            if (method.name == "verifyPurchase" && method.returnType == "Z" && method.parameterTypes.size == 3) {
                if (method.parameterTypes.all { it == "Ljava/lang/String;" }) {
                    try {
                        val mutableClass = mutableClassDefBy(classDef)
                        val mutableMethod = mutableClass.findMutableMethodOf(method)
                        mutableMethod.addInstructions(0, "const/4 v0, 0x1\nreturn v0")
                        patchedCount++
                    } catch (_: Exception) {}
                }
            }
        }
    }

    if (patchedCount > 0) {
        logger.info("Security.verifyPurchase patched in $patchedCount method(s).")
    }

    return patchedCount > 0
}

/**
 * Strategy for capturing Unity Purchasing Google Play bridge instance.
 */
private fun BytecodePatchContext.applyUnityPurchasingStrategy(logger: Logger): Boolean {
    var patched = false

    val purchaseMethod = UnityPurchasingGooglePlayPurchaseFingerprint.methodOrNull
    if (purchaseMethod != null) {
        try {
            purchaseMethod.addInstructions(
                0,
                """
                invoke-static {p0}, Lcom/dmoniak/patches/extension/HungrySharkBillingHelper;->registerPurchasesUpdatedListener(Ljava/lang/Object;)V
                """.trimIndent(),
            )
            logger.info("GooglePlayPurchasing.purchase hooked to register listener.")
            patched = true
        } catch (e: Exception) {
            logger.warning("Failed to hook GooglePlayPurchasing.purchase: ${e.message}")
        }
    }

    val updatedMethod = UnityPurchasingGooglePlayOnPurchasesUpdatedFingerprint.methodOrNull
    if (updatedMethod != null) {
        try {
            updatedMethod.addInstructions(
                0,
                """
                invoke-static {p0}, Lcom/dmoniak/patches/extension/HungrySharkBillingHelper;->registerPurchasesUpdatedListener(Ljava/lang/Object;)V
                """.trimIndent(),
            )
            logger.info("GooglePlayPurchasing.onPurchasesUpdated hooked to register listener.")
            patched = true
        } catch (e: Exception) {
            logger.warning("Failed to hook GooglePlayPurchasing.onPurchasesUpdated: ${e.message}")
        }
    }

    return patched
}

/**
 * Scans for call sites of BillingClient.launchBillingFlow across all classes to guarantee interception.
 */
private fun BytecodePatchContext.applyBillingCallSiteScanStrategy(logger: Logger) {
    var patchedCallSites = 0

    classDefForEach { classDef ->
        val tl = classDef.type.lowercase()
        if (tl.contains("androidx") || tl.contains("android/support")) return@classDefForEach

        val mutableClass by lazy { mutableClassDefBy(classDef) }
        for (method in classDef.methods) {
            val mutableMethod by lazy { mutableClass.findMutableMethodOf(method) }
            val impl = method.implementation ?: continue
            val instructions = impl.instructions.toList()

            for ((index, insn) in instructions.withIndex()) {
                val ref = (insn as? ReferenceInstruction)?.reference as? MethodReference ?: continue
                if (ref.name == "launchBillingFlow" && ref.returnType == "Lcom/android/billingclient/api/BillingResult;") {
                    try {
                        val activityReg: Int
                        val paramsReg: Int
                        val clientReg: Int

                        when (insn) {
                            is BuilderInstruction35c -> {
                                if (insn.registerCount < 3) continue
                                clientReg = insn.registerC
                                activityReg = insn.registerD
                                paramsReg = insn.registerE
                            }
                            is BuilderInstruction3rc -> {
                                clientReg = insn.startRegister
                                activityReg = insn.startRegister + 1
                                paramsReg = insn.startRegister + 2
                            }
                            else -> continue
                        }

                        mutableMethod.addInstructions(
                            index,
                            """
                            invoke-static {v$clientReg, v$activityReg, v$paramsReg}, Lcom/dmoniak/patches/extension/HungrySharkBillingHelper;->handleLaunchBillingFlow(Ljava/lang/Object;Landroid/app/Activity;Ljava/lang/Object;)Ljava/lang/Object;
                            """.trimIndent(),
                        )
                        patchedCallSites++
                    } catch (_: Exception) {}
                }
            }
        }
    }

    if (patchedCallSites > 0) {
        logger.info("BillingClient call site scan: patched $patchedCallSites launchBillingFlow call site(s).")
    }
}

package com.dmoniak.patches.hungryshark

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.iface.ClassDef
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

        var isReadyCount = 0
        var launchCount = 0
        var consumeCount = 0
        var acknowledgeCount = 0
        var responseCodeCount = 0
        var listenerUpdateCount = 0
        var verifyCount = 0

        // Phase 1: First pass to inspect Google Play Billing classes and metadata
        var flowParamsClassDef: ClassDef? = null
        var billingClientImplDef: ClassDef? = null
        var detectedListenerFieldName: String? = null

        classDefForEach { classDef ->
            val type = classDef.type
            if (type == "Lcom/android/billingclient/api/BillingFlowParams;") {
                flowParamsClassDef = classDef
            } else if (type == "Lcom/android/billingclient/api/BillingClientImpl;" ||
                (type.contains("billingclient") && type.endsWith("/BillingClientImpl;"))
            ) {
                billingClientImplDef = classDef
                for (field in classDef.fields) {
                    if (field.type == "Lcom/android/billingclient/api/PurchasesUpdatedListener;") {
                        detectedListenerFieldName = field.name
                        logger.info("Found PurchasesUpdatedListener field in BillingClientImpl: ${field.name}")
                        break
                    }
                }
            }
        }

        val listenerFieldName = detectedListenerFieldName ?: "zzd"
        logger.info("Using PurchasesUpdatedListener field name: $listenerFieldName")

        // Inspect BillingFlowParams to find how SKU is stored or accessed
        val skuMethod = flowParamsClassDef?.methods?.firstOrNull {
            it.returnType == "Ljava/lang/String;" && it.parameterTypes.isEmpty()
        }
        val skuField = flowParamsClassDef?.fields?.firstOrNull {
            it.type == "Ljava/lang/String;"
        }

        val skuSmaliExtract = when {
            skuMethod != null -> {
                logger.info("Extracting SKU via BillingFlowParams.${skuMethod.name}()")
                """
                invoke-virtual {v1}, Lcom/android/billingclient/api/BillingFlowParams;->${skuMethod.name}()Ljava/lang/String;
                move-result-object v7
                """.trimIndent()
            }
            skuField != null -> {
                logger.info("Extracting SKU via BillingFlowParams.${skuField.name}")
                """
                iget-object v7, v1, Lcom/android/billingclient/api/BillingFlowParams;->${skuField.name}:Ljava/lang/String;
                """.trimIndent()
            }
            else -> {
                logger.info("Using default SKU fallback for BillingFlowParams")
                """
                const-string v7, "com.ubisoft.hungrysharkworld.gems_pack_1"
                """.trimIndent()
            }
        }

        // Phase 2: Patch target classes
        classDefForEach { classDef ->
            val tl = classDef.type.lowercase()
            if (tl.contains("androidx") || tl.contains("android/support")) return@classDefForEach

            val mutableClass by lazy { mutableClassDefBy(classDef) }

            for (method in classDef.methods.toList()) {
                if (method.implementation == null) continue
                val isStatic = AccessFlags.STATIC.isSet(method.accessFlags)
                val mName = method.name
                val pTypes = method.parameterTypes

                // 1. Target Google Play Billing classes strictly
                if (classDef.type.contains("billingclient")) {

                    // 1a. BillingClient.isReady() -> boolean (always ready)
                    if (!isStatic &&
                        mName == "isReady" &&
                        method.returnType == "Z" &&
                        pTypes.isEmpty()
                    ) {
                        try {
                            val mutableMethod = mutableClass.findMutableMethodOf(method)
                            mutableMethod.addInstructions(
                                0,
                                """
                                const/4 v0, 0x1
                                return v0
                                """.trimIndent(),
                            )
                            isReadyCount++
                            logger.info("Patched isReady in ${classDef.type}")
                        } catch (e: Exception) {
                            logger.warning("Failed to patch isReady in ${classDef.type}: ${e.message}")
                        }
                    }

                    // 1b. BillingResult.getResponseCode() -> int (always OK / 0)
                    if (classDef.type.endsWith("/BillingResult;") &&
                        !isStatic &&
                        mName == "getResponseCode" &&
                        method.returnType == "I" &&
                        pTypes.isEmpty()
                    ) {
                        try {
                            val mutableMethod = mutableClass.findMutableMethodOf(method)
                            mutableMethod.addInstructions(
                                0,
                                """
                                const/4 v0, 0x0
                                return v0
                                """.trimIndent(),
                            )
                            responseCodeCount++
                            logger.info("Patched BillingResult.getResponseCode")
                        } catch (e: Exception) {
                            logger.warning("Failed to patch getResponseCode: ${e.message}")
                        }
                    }

                    // 1c. BillingClient.launchBillingFlow(Activity, BillingFlowParams) -> BillingResult
                    // Intercepts the purchase request, constructs a simulated valid Purchase,
                    // fires onPurchasesUpdated directly on the listener, and returns an OK BillingResult.
                    if (!isStatic &&
                        mName == "launchBillingFlow" &&
                        method.returnType == "Lcom/android/billingclient/api/BillingResult;" &&
                        pTypes.size == 2
                    ) {
                        try {
                            val clonedLaunch = cloneMethodWithAdditionalRegisters(method, 10)
                            clonedLaunch.addInstructions(
                                0,
                                """
                                move-object/from16 v0, p0
                                move-object/from16 v1, p2

                                invoke-static {}, Lcom/android/billingclient/api/BillingResult;->newBuilder()Lcom/android/billingclient/api/BillingResult${'$'}Builder;
                                move-result-object v2
                                const/4 v3, 0x0
                                invoke-virtual {v2, v3}, Lcom/android/billingclient/api/BillingResult${'$'}Builder;->setResponseCode(I)Lcom/android/billingclient/api/BillingResult${'$'}Builder;
                                move-result-object v2
                                invoke-virtual {v2}, Lcom/android/billingclient/api/BillingResult${'$'}Builder;->build()Lcom/android/billingclient/api/BillingResult;
                                move-result-object v2

                                iget-object v3, v0, Lcom/android/billingclient/api/BillingClientImpl;->$listenerFieldName:Lcom/android/billingclient/api/PurchasesUpdatedListener;
                                if-eqz v3, :morphe_billing_skip

                                $skuSmaliExtract

                                if-nez v7, :morphe_sku_ready
                                const-string v7, "com.ubisoft.hungrysharkworld.gems_pack_1"
                                :morphe_sku_ready
                                const-string v8, "REPLACE_SKU"
                                const-string v5, "{\"orderId\":\"GPA.1234-5678-9012-34567\",\"packageName\":\"com.ubisoft.hungrysharkworld\",\"productId\":\"REPLACE_SKU\",\"productIds\":[\"REPLACE_SKU\"],\"purchaseTime\":1700000000000,\"purchaseState\":1,\"purchaseToken\":\"morphe_token\",\"quantity\":1,\"acknowledged\":false}"
                                invoke-virtual {v5, v8, v7}, Ljava/lang/String;->replace(Ljava/lang/CharSequence;Ljava/lang/CharSequence;)Ljava/lang/String;
                                move-result-object v5

                                new-instance v4, Lcom/android/billingclient/api/Purchase;
                                const-string v6, "morphe_sig"
                                invoke-direct {v4, v5, v6}, Lcom/android/billingclient/api/Purchase;-><init>(Ljava/lang/String;Ljava/lang/String;)V

                                new-instance v5, Ljava/util/ArrayList;
                                invoke-direct {v5}, Ljava/util/ArrayList;-><init>()V
                                invoke-virtual {v5, v4}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z

                                invoke-interface {v3, v2, v5}, Lcom/android/billingclient/api/PurchasesUpdatedListener;->onPurchasesUpdated(Lcom/android/billingclient/api/BillingResult;Ljava/util/List;)V

                                :morphe_billing_skip
                                return-object v2
                                """.trimIndent(),
                            )
                            launchCount++
                            logger.info("Patched launchBillingFlow in ${classDef.type}")
                        } catch (e: Exception) {
                            logger.warning("Failed to patch launchBillingFlow in ${classDef.type}: ${e.message}")
                        }
                    }

                    // 1d. BillingClient.consumeAsync(ConsumeParams, ConsumeResponseListener) -> void
                    // Immediately marks consumable items as consumed so they can be repurchased.
                    if (!isStatic &&
                        mName == "consumeAsync" &&
                        method.returnType == "V" &&
                        pTypes.size == 2
                    ) {
                        try {
                            val clonedConsume = cloneMethodWithAdditionalRegisters(method, 4)
                            clonedConsume.addInstructions(
                                0,
                                """
                                move-object/from16 v0, p2
                                if-eqz v0, :morphe_consume_done
                                invoke-static {}, Lcom/android/billingclient/api/BillingResult;->newBuilder()Lcom/android/billingclient/api/BillingResult${'$'}Builder;
                                move-result-object v1
                                const/4 v2, 0x0
                                invoke-virtual {v1, v2}, Lcom/android/billingclient/api/BillingResult${'$'}Builder;->setResponseCode(I)Lcom/android/billingclient/api/BillingResult${'$'}Builder;
                                move-result-object v1
                                invoke-virtual {v1}, Lcom/android/billingclient/api/BillingResult${'$'}Builder;->build()Lcom/android/billingclient/api/BillingResult;
                                move-result-object v1
                                const-string v2, "morphe_token"
                                invoke-interface {v0, v1, v2}, Lcom/android/billingclient/api/ConsumeResponseListener;->onConsumeResponse(Lcom/android/billingclient/api/BillingResult;Ljava/lang/String;)V
                                :morphe_consume_done
                                return-void
                                """.trimIndent(),
                            )
                            consumeCount++
                            logger.info("Patched consumeAsync in ${classDef.type}")
                        } catch (e: Exception) {
                            logger.warning("Failed to patch consumeAsync in ${classDef.type}: ${e.message}")
                        }
                    }

                    // 1e. BillingClient.acknowledgePurchase(AcknowledgePurchaseParams, AcknowledgePurchaseResponseListener) -> void
                    // Immediately marks non-consumable items as acknowledged.
                    if (!isStatic &&
                        mName == "acknowledgePurchase" &&
                        method.returnType == "V" &&
                        pTypes.size == 2
                    ) {
                        try {
                            val clonedAck = cloneMethodWithAdditionalRegisters(method, 4)
                            clonedAck.addInstructions(
                                0,
                                """
                                move-object/from16 v0, p2
                                if-eqz v0, :morphe_ack_done
                                invoke-static {}, Lcom/android/billingclient/api/BillingResult;->newBuilder()Lcom/android/billingclient/api/BillingResult${'$'}Builder;
                                move-result-object v1
                                const/4 v2, 0x0
                                invoke-virtual {v1, v2}, Lcom/android/billingclient/api/BillingResult${'$'}Builder;->setResponseCode(I)Lcom/android/billingclient/api/BillingResult${'$'}Builder;
                                move-result-object v1
                                invoke-virtual {v1}, Lcom/android/billingclient/api/BillingResult${'$'}Builder;->build()Lcom/android/billingclient/api/BillingResult;
                                move-result-object v1
                                invoke-interface {v0, v1}, Lcom/android/billingclient/api/AcknowledgePurchaseResponseListener;->onAcknowledgePurchaseResponse(Lcom/android/billingclient/api/BillingResult;)V
                                :morphe_ack_done
                                return-void
                                """.trimIndent(),
                            )
                            acknowledgeCount++
                            logger.info("Patched acknowledgePurchase in ${classDef.type}")
                        } catch (e: Exception) {
                            logger.warning("Failed to patch acknowledgePurchase in ${classDef.type}: ${e.message}")
                        }
                    }
                }

                // 2. PurchasesUpdatedListener.onPurchasesUpdated(BillingResult, List) -> void
                // Safety net: if onPurchasesUpdated is called anywhere with null or empty purchases,
                // populate a valid purchase so the transaction succeeds.
                if (classDef.interfaces.contains("Lcom/android/billingclient/api/PurchasesUpdatedListener;") ||
                    classDef.type.contains("PurchasesUpdatedListener")
                ) {
                    if (!isStatic &&
                        mName == "onPurchasesUpdated" &&
                        method.returnType == "V" &&
                        pTypes.size == 2
                    ) {
                        try {
                            val clonedListener = cloneMethodWithAdditionalRegisters(method, 4)
                            clonedListener.addInstructions(
                                0,
                                """
                                move-object/from16 v0, p2
                                if-eqz v0, :morphe_p_check_empty
                                goto :morphe_p_create
                                :morphe_p_check_empty
                                invoke-interface {v0}, Ljava/util/List;->isEmpty()Z
                                move-result v1
                                if-eqz v1, :morphe_p_create
                                goto :morphe_p_skip
                                :morphe_p_create
                                new-instance v0, Ljava/util/ArrayList;
                                invoke-direct {v0}, Ljava/util/ArrayList;-><init>()V
                                new-instance v1, Lcom/android/billingclient/api/Purchase;
                                const-string v2, "{\"orderId\":\"GPA.1234-5678-9012-34567\",\"packageName\":\"com.ubisoft.hungrysharkworld\",\"productId\":\"com.ubisoft.hungrysharkworld.gems_pack_1\",\"productIds\":[\"com.ubisoft.hungrysharkworld.gems_pack_1\"],\"purchaseTime\":1700000000000,\"purchaseState\":1,\"purchaseToken\":\"morphe_token\",\"quantity\":1,\"acknowledged\":false}"
                                const-string v3, "morphe_sig"
                                invoke-direct {v1, v2, v3}, Lcom/android/billingclient/api/Purchase;-><init>(Ljava/lang/String;Ljava/lang/String;)V
                                invoke-virtual {v0, v1}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z
                                move-object/from16 p2, v0
                                :morphe_p_skip
                                """.trimIndent(),
                            )
                            listenerUpdateCount++
                            logger.info("Patched onPurchasesUpdated in ${classDef.type}")
                        } catch (e: Exception) {
                            logger.warning("Failed to patch onPurchasesUpdated in ${classDef.type}: ${e.message}")
                        }
                    }
                }

                // 3. verifyPurchase(String, String, String) -> boolean
                // Bypasses receipt signature validation if present in the game code
                if (mName == "verifyPurchase" &&
                    method.returnType == "Z" &&
                    pTypes.size == 3 &&
                    pTypes.all { it == "Ljava/lang/String;" }
                ) {
                    try {
                        val mutableMethod = mutableClass.findMutableMethodOf(method)
                        mutableMethod.addInstructions(
                            0,
                            """
                            const/4 v0, 0x1
                            return v0
                            """.trimIndent(),
                        )
                        verifyCount++
                        logger.info("Patched verifyPurchase in ${classDef.type}")
                    } catch (e: Exception) {
                        logger.warning("Failed to patch verifyPurchase in ${classDef.type}: ${e.message}")
                    }
                }
            }
        }

        logger.info(
            "Free Shopping results: isReady=$isReadyCount, getResponseCode=$responseCodeCount, " +
            "launchBillingFlow=$launchCount, consumeAsync=$consumeCount, " +
            "acknowledgePurchase=$acknowledgeCount, onPurchasesUpdated=$listenerUpdateCount, " +
            "verifyPurchase=$verifyCount."
        )
        logger.info("Free Shopping patch execution finished.")
    }
}

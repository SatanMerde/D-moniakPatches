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
        var setListenerCount = 0
        var purchaseCaptureCount = 0
        var verifyCount = 0

        // Phase 1: Inspect BillingFlowParams to locate SKU getter method or field
        var flowParamsClassDef: ClassDef? = null

        classDefForEach { classDef ->
            val type = classDef.type
            if (type == "Lcom/android/billingclient/api/BillingFlowParams;") {
                flowParamsClassDef = classDef
            }
        }

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

                // =========================================================================
                // 1. Google Play Billing Client Core Methods
                // =========================================================================
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

                    // 1c. BillingClient.Builder.setListener(PurchasesUpdatedListener) -> capture listener
                    if (!isStatic &&
                        mName == "setListener" &&
                        pTypes.size == 1 &&
                        pTypes[0] == "Lcom/android/billingclient/api/PurchasesUpdatedListener;"
                    ) {
                        try {
                            val clonedSetListener = cloneMethodWithAdditionalRegisters(method, 4)
                            clonedSetListener.addInstructions(
                                0,
                                """
                                move-object/from16 v2, p1
                                if-nez v2, :morphe_save_listener_done
                                goto :morphe_skip_save_listener
                                :morphe_save_listener_done
                                invoke-static {}, Ljava/lang/System;->getProperties()Ljava/util/Properties;
                                move-result-object v0
                                const-string v1, "morphe_billing_listener"
                                invoke-virtual {v0, v1, v2}, Ljava/util/Properties;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
                                :morphe_skip_save_listener
                                """.trimIndent(),
                            )
                            setListenerCount++
                            logger.info("Patched setListener in ${classDef.type}")
                        } catch (e: Exception) {
                            logger.warning("Failed to patch setListener in ${classDef.type}: ${e.message}")
                        }
                    }

                    // 1d. BillingClientImpl constructor(..., PurchasesUpdatedListener, ...) -> capture listener
                    if (mName == "<init>" && pTypes.contains("Lcom/android/billingclient/api/PurchasesUpdatedListener;")) {
                        try {
                            val pIndex = pTypes.indexOf("Lcom/android/billingclient/api/PurchasesUpdatedListener;") + 1
                            val clonedCtor = cloneMethodWithAdditionalRegisters(method, 4)
                            clonedCtor.addInstructions(
                                0,
                                """
                                move-object/from16 v2, p$pIndex
                                if-nez v2, :morphe_save_ctor_listener
                                goto :morphe_skip_ctor_listener
                                :morphe_save_ctor_listener
                                invoke-static {}, Ljava/lang/System;->getProperties()Ljava/util/Properties;
                                move-result-object v0
                                const-string v1, "morphe_billing_listener"
                                invoke-virtual {v0, v1, v2}, Ljava/util/Properties;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
                                :morphe_skip_ctor_listener
                                """.trimIndent(),
                            )
                            logger.info("Patched constructor with PurchasesUpdatedListener in ${classDef.type}")
                        } catch (e: Exception) {
                            logger.warning("Failed to patch constructor in ${classDef.type}: ${e.message}")
                        }
                    }

                    // 1e. BillingClient.launchBillingFlow(Activity, BillingFlowParams) -> BillingResult
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

                                $skuSmaliExtract

                                if-nez v7, :morphe_sku_ready
                                const-string v7, "com.ubisoft.hungrysharkworld.gems_pack_1"
                                :morphe_sku_ready

                                invoke-static {}, Ljava/lang/System;->getProperties()Ljava/util/Properties;
                                move-result-object v3
                                const-string v4, "morphe_last_sku"
                                invoke-virtual {v3, v4, v7}, Ljava/util/Properties;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

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

                                invoke-static {}, Ljava/lang/System;->getProperties()Ljava/util/Properties;
                                move-result-object v3
                                const-string v4, "morphe_billing_listener"
                                invoke-virtual {v3, v4}, Ljava/util/Properties;->get(Ljava/lang/Object;)Ljava/lang/Object;
                                move-result-object v3

                                if-nez v3, :morphe_billing_notify
                                goto :morphe_billing_skip

                                :morphe_billing_notify
                                check-cast v3, Lcom/android/billingclient/api/PurchasesUpdatedListener;
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

                    // 1f. BillingClient.consumeAsync(ConsumeParams, ConsumeResponseListener) -> void
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

                    // 1g. BillingClient.acknowledgePurchase(AcknowledgePurchaseParams, AcknowledgePurchaseResponseListener) -> void
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

                // =========================================================================
                // 2. PurchasesUpdatedListener Implementations & Unity GooglePlayPurchasing
                // =========================================================================
                val implementsListener = classDef.interfaces.contains("Lcom/android/billingclient/api/PurchasesUpdatedListener;") ||
                    classDef.type.contains("PurchasesUpdatedListener") ||
                    classDef.type.endsWith("/GooglePlayPurchasing;")

                if (implementsListener) {
                    // Capture listener instance in purchase(...)
                    if (!isStatic && mName == "purchase") {
                        try {
                            val clonedCapture = cloneMethodWithAdditionalRegisters(method, 4)
                            clonedCapture.addInstructions(
                                0,
                                """
                                move-object/from16 v2, p0
                                invoke-static {}, Ljava/lang/System;->getProperties()Ljava/util/Properties;
                                move-result-object v0
                                const-string v1, "morphe_billing_listener"
                                invoke-virtual {v0, v1, v2}, Ljava/util/Properties;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
                                """.trimIndent(),
                            )
                            purchaseCaptureCount++
                            logger.info("Patched $mName to capture PurchasesUpdatedListener in ${classDef.type}")
                        } catch (e: Exception) {
                            logger.warning("Failed to patch $mName in ${classDef.type}: ${e.message}")
                        }
                    }

                    // On onPurchasesUpdated: ensure list is populated if empty or null
                    if (!isStatic &&
                        mName == "onPurchasesUpdated" &&
                        method.returnType == "V" &&
                        pTypes.size == 2
                    ) {
                        try {
                            val clonedListener = cloneMethodWithAdditionalRegisters(method, 6)
                            clonedListener.addInstructions(
                                0,
                                """
                                invoke-static {}, Lcom/android/billingclient/api/BillingResult;->newBuilder()Lcom/android/billingclient/api/BillingResult${'$'}Builder;
                                move-result-object v0
                                const/4 v1, 0x0
                                invoke-virtual {v0, v1}, Lcom/android/billingclient/api/BillingResult${'$'}Builder;->setResponseCode(I)Lcom/android/billingclient/api/BillingResult${'$'}Builder;
                                move-result-object v0
                                invoke-virtual {v0}, Lcom/android/billingclient/api/BillingResult${'$'}Builder;->build()Lcom/android/billingclient/api/BillingResult;
                                move-result-object v0
                                move-object/from16 p1, v0

                                move-object/from16 v0, p2
                                if-nez v0, :morphe_p_not_null
                                goto :morphe_p_create

                                :morphe_p_not_null
                                invoke-interface {v0}, Ljava/util/List;->isEmpty()Z
                                move-result v1
                                if-eqz v1, :morphe_p_skip

                                :morphe_p_create
                                invoke-static {}, Ljava/lang/System;->getProperties()Ljava/util/Properties;
                                move-result-object v0
                                const-string v1, "morphe_last_sku"
                                invoke-virtual {v0, v1}, Ljava/util/Properties;->get(Ljava/lang/Object;)Ljava/lang/Object;
                                move-result-object v2
                                check-cast v2, Ljava/lang/String;
                                if-nez v2, :morphe_sku_default
                                const-string v2, "com.ubisoft.hungrysharkworld.gems_pack_1"
                                :morphe_sku_default

                                const-string v3, "REPLACE_SKU"
                                const-string v4, "{\"orderId\":\"GPA.1234-5678-9012-34567\",\"packageName\":\"com.ubisoft.hungrysharkworld\",\"productId\":\"REPLACE_SKU\",\"productIds\":[\"REPLACE_SKU\"],\"purchaseTime\":1700000000000,\"purchaseState\":1,\"purchaseToken\":\"morphe_token\",\"quantity\":1,\"acknowledged\":false}"
                                invoke-virtual {v4, v3, v2}, Ljava/lang/String;->replace(Ljava/lang/CharSequence;Ljava/lang/CharSequence;)Ljava/lang/String;
                                move-result-object v4

                                new-instance v5, Lcom/android/billingclient/api/Purchase;
                                const-string v3, "morphe_sig"
                                invoke-direct {v5, v4, v3}, Lcom/android/billingclient/api/Purchase;-><init>(Ljava/lang/String;Ljava/lang/String;)V

                                new-instance v0, Ljava/util/ArrayList;
                                invoke-direct {v0}, Ljava/util/ArrayList;-><init>()V
                                invoke-virtual {v0, v5}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z
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

                // =========================================================================
                // 3. Receipt Verification Bypass
                // =========================================================================
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
            "setListener=$setListenerCount, purchaseCapture=$purchaseCaptureCount, " +
            "launchBillingFlow=$launchCount, consumeAsync=$consumeCount, " +
            "acknowledgePurchase=$acknowledgeCount, onPurchasesUpdated=$listenerUpdateCount, " +
            "verifyPurchase=$verifyCount."
        )
        logger.info("Free Shopping patch execution finished.")
    }
}

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
    description = "Unlocks shop items and in-app purchases in Hungry Shark World by intercepting Google Play Billing and Ubisoft Orion Houston receipt validation.",
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
        var gbsPurchaseCount = 0
        var houstonValidationCount = 0
        var gbsValidationCount = 0
        var verifyCount = 0

        // Phase 1: Inspect BillingFlowParams to locate SKU getter method or field
        var flowParamsClassDef: ClassDef? = null

        classDefForEach { classDef ->
            val type = classDef.type
            if (type == "Lcom/android/billingclient/api/BillingFlowParams;") {
                flowParamsClassDef = classDef
            }
        }

        val skuDetailsMethod = flowParamsClassDef?.methods?.firstOrNull {
            it.returnType == "Lcom/android/billingclient/api/SkuDetails;" && it.parameterTypes.isEmpty()
        }
        val skuDetailsField = flowParamsClassDef?.fields?.firstOrNull {
            it.type == "Lcom/android/billingclient/api/SkuDetails;"
        }
        val directSkuMethod = flowParamsClassDef?.methods?.firstOrNull {
            (it.name == "getSku" || it.name.contains("Sku", ignoreCase = true)) &&
            it.returnType == "Ljava/lang/String;" && it.parameterTypes.isEmpty()
        }
        val directSkuField = flowParamsClassDef?.fields?.firstOrNull {
            (it.name == "sku" || it.name.contains("Sku", ignoreCase = true)) &&
            it.type == "Ljava/lang/String;"
        }

        val skuSmaliExtract = when {
            skuDetailsMethod != null -> {
                logger.info("Extracting SKU via BillingFlowParams.${skuDetailsMethod.name}().getSku()")
                """
                invoke-virtual {v1}, Lcom/android/billingclient/api/BillingFlowParams;->${skuDetailsMethod.name}()Lcom/android/billingclient/api/SkuDetails;
                move-result-object v3
                if-eqz v3, :morphe_sku_check_prop
                invoke-virtual {v3}, Lcom/android/billingclient/api/SkuDetails;->getSku()Ljava/lang/String;
                move-result-object v3
                goto :morphe_sku_done
                """.trimIndent()
            }
            skuDetailsField != null -> {
                logger.info("Extracting SKU via BillingFlowParams.${skuDetailsField.name}.getSku()")
                """
                iget-object v3, v1, Lcom/android/billingclient/api/BillingFlowParams;->${skuDetailsField.name}:Lcom/android/billingclient/api/SkuDetails;
                if-eqz v3, :morphe_sku_check_prop
                invoke-virtual {v3}, Lcom/android/billingclient/api/SkuDetails;->getSku()Ljava/lang/String;
                move-result-object v3
                goto :morphe_sku_done
                """.trimIndent()
            }
            directSkuMethod != null -> {
                logger.info("Extracting SKU via BillingFlowParams.${directSkuMethod.name}()")
                """
                invoke-virtual {v1}, Lcom/android/billingclient/api/BillingFlowParams;->${directSkuMethod.name}()Ljava/lang/String;
                move-result-object v3
                if-eqz v3, :morphe_sku_check_prop
                goto :morphe_sku_done
                """.trimIndent()
            }
            directSkuField != null -> {
                logger.info("Extracting SKU via BillingFlowParams.${directSkuField.name}")
                """
                iget-object v3, v1, Lcom/android/billingclient/api/BillingFlowParams;->${directSkuField.name}:Ljava/lang/String;
                if-eqz v3, :morphe_sku_check_prop
                goto :morphe_sku_done
                """.trimIndent()
            }
            else -> {
                logger.info("Extracting SKU via morphe_last_sku property fallback")
                """
                goto :morphe_sku_check_prop
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
                    val isSetListener = !isStatic &&
                        (mName == "setListener" || mName.contains("Listener", ignoreCase = true)) &&
                        pTypes.contains("Lcom/android/billingclient/api/PurchasesUpdatedListener;")

                    if (isSetListener) {
                        try {
                            val pIdx = pTypes.indexOf("Lcom/android/billingclient/api/PurchasesUpdatedListener;") + 1
                            val clonedSetListener = cloneMethodWithAdditionalRegisters(method, 4)
                            clonedSetListener.addInstructions(
                                0,
                                """
                                move-object/from16 v2, p$pIdx
                                if-eqz v2, :morphe_skip_save_listener
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
                                if-eqz v2, :morphe_skip_ctor_listener
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
                            val clonedLaunch = cloneMethodWithAdditionalRegisters(method, 16)
                            clonedLaunch.addInstructions(
                                0,
                                """
                                move-object/from16 v2, p1
                                move-object/from16 v1, p2

                                invoke-static {}, Lcom/android/billingclient/api/BillingResult;->newBuilder()Lcom/android/billingclient/api/BillingResult${'$'}Builder;
                                move-result-object v0
                                const/4 v4, 0x0
                                invoke-virtual {v0, v4}, Lcom/android/billingclient/api/BillingResult${'$'}Builder;->setResponseCode(I)Lcom/android/billingclient/api/BillingResult${'$'}Builder;
                                move-result-object v0
                                invoke-virtual {v0}, Lcom/android/billingclient/api/BillingResult${'$'}Builder;->build()Lcom/android/billingclient/api/BillingResult;
                                move-result-object v0

                                $skuSmaliExtract

                                :morphe_sku_check_prop
                                invoke-static {}, Ljava/lang/System;->getProperties()Ljava/util/Properties;
                                move-result-object v12
                                const-string v4, "morphe_last_sku"
                                invoke-virtual {v12, v4}, Ljava/util/Properties;->get(Ljava/lang/Object;)Ljava/lang/Object;
                                move-result-object v3
                                check-cast v3, Ljava/lang/String;

                                :morphe_sku_done
                                if-nez v3, :morphe_sku_ready
                                const-string v3, "com.ubisoft.hungrysharkworld.gems_pack_1"

                                :morphe_sku_ready
                                invoke-static {}, Ljava/lang/System;->getProperties()Ljava/util/Properties;
                                move-result-object v12
                                const-string v4, "morphe_last_sku"
                                invoke-virtual {v12, v4, v3}, Ljava/util/Properties;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

                                const-string v4, "REPLACE_SKU"
                                const-string v5, "{\"orderId\":\"GPA.1234-5678-9012-34567\",\"packageName\":\"com.ubisoft.hungrysharkworld\",\"productId\":\"REPLACE_SKU\",\"productIds\":[\"REPLACE_SKU\"],\"purchaseTime\":1700000000000,\"purchaseState\":1,\"purchaseToken\":\"morphe_token\",\"quantity\":1,\"acknowledged\":false}"
                                invoke-virtual {v5, v4, v3}, Ljava/lang/String;->replace(Ljava/lang/CharSequence;Ljava/lang/CharSequence;)Ljava/lang/String;
                                move-result-object v5

                                new-instance v7, Lcom/android/billingclient/api/Purchase;
                                const-string v4, "morphe_sig"
                                invoke-direct {v7, v5, v4}, Lcom/android/billingclient/api/Purchase;-><init>(Ljava/lang/String;Ljava/lang/String;)V

                                new-instance v8, Ljava/util/ArrayList;
                                invoke-direct {v8}, Ljava/util/ArrayList;-><init>()V
                                invoke-virtual {v8, v7}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z

                                invoke-static {}, Ljava/lang/System;->getProperties()Ljava/util/Properties;
                                move-result-object v12
                                const-string v4, "morphe_billing_listener"
                                invoke-virtual {v12, v4}, Ljava/util/Properties;->get(Ljava/lang/Object;)Ljava/lang/Object;
                                move-result-object v11

                                if-eqz v11, :morphe_skip_direct_listener
                                instance-of v4, v11, Lcom/android/billingclient/api/PurchasesUpdatedListener;
                                if-eqz v4, :morphe_skip_direct_listener
                                check-cast v11, Lcom/android/billingclient/api/PurchasesUpdatedListener;
                                invoke-interface {v11, v0, v8}, Lcom/android/billingclient/api/PurchasesUpdatedListener;->onPurchasesUpdated(Lcom/android/billingclient/api/BillingResult;Ljava/util/List;)V

                                :morphe_skip_direct_listener
                                return-object v0
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
                // 2. Ubisoft Orion Monetisation Core (GoogleBillingService)
                // =========================================================================
                if (classDef.type == "Lcom/ubisoft/orion/monetisationcore/billing/GoogleBillingService;") {

                    // 2a. Capture PurchasesUpdatedListener in initialise(...)
                    if (mName == "initialise" && pTypes.contains("Landroid/app/Activity;")) {
                        try {
                            val clonedInit = cloneMethodWithAdditionalRegisters(method, 4)
                            clonedInit.addInstructions(
                                0,
                                """
                                iget-object v0, p0, Lcom/ubisoft/orion/monetisationcore/billing/GoogleBillingService;->purchasesUpdatedListener:Lcom/android/billingclient/api/PurchasesUpdatedListener;
                                if-eqz v0, :morphe_skip_init_listener
                                invoke-static {}, Ljava/lang/System;->getProperties()Ljava/util/Properties;
                                move-result-object v1
                                const-string v2, "morphe_billing_listener"
                                invoke-virtual {v1, v2, v0}, Ljava/util/Properties;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
                                :morphe_skip_init_listener
                                """.trimIndent(),
                            )
                            logger.info("Patched GoogleBillingService.initialise to capture PurchasesUpdatedListener")
                        } catch (e: Exception) {
                            logger.warning("Failed to patch GoogleBillingService.initialise: ${e.message}")
                        }
                    }

                    // 2b. Directly dispatch purchase in purchaseProduct(String) and purchaseProductWithOffer(String, String)
                    val isGbsPurchase = !isStatic && (
                        (mName == "purchaseProduct" && pTypes.size == 1 && pTypes[0] == "Ljava/lang/String;") ||
                        (mName == "purchaseProductWithOffer" && pTypes.size == 2 && pTypes[0] == "Ljava/lang/String;")
                    )

                    if (isGbsPurchase) {
                        try {
                            val clonedGbsPurchase = cloneMethodWithAdditionalRegisters(method, 8)
                            clonedGbsPurchase.addInstructions(
                                0,
                                """
                                move-object/from16 v3, p1
                                if-nez v3, :morphe_gbs_sku_ok
                                const-string v3, "com.ubisoft.hungrysharkworld.gems_pack_1"

                                :morphe_gbs_sku_ok
                                invoke-static {}, Ljava/lang/System;->getProperties()Ljava/util/Properties;
                                move-result-object v0
                                const-string v1, "morphe_last_sku"
                                invoke-virtual {v0, v1, v3}, Ljava/util/Properties;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

                                iget-object v0, p0, Lcom/ubisoft/orion/monetisationcore/billing/GoogleBillingService;->purchasesUpdatedListener:Lcom/android/billingclient/api/PurchasesUpdatedListener;
                                if-eqz v0, :morphe_skip_gbs_dispatch

                                invoke-static {}, Lcom/android/billingclient/api/BillingResult;->newBuilder()Lcom/android/billingclient/api/BillingResult${'$'}Builder;
                                move-result-object v1
                                const/4 v2, 0x0
                                invoke-virtual {v1, v2}, Lcom/android/billingclient/api/BillingResult${'$'}Builder;->setResponseCode(I)Lcom/android/billingclient/api/BillingResult${'$'}Builder;
                                move-result-object v1
                                invoke-virtual {v1}, Lcom/android/billingclient/api/BillingResult${'$'}Builder;->build()Lcom/android/billingclient/api/BillingResult;
                                move-result-object v1

                                const-string v2, "REPLACE_SKU"
                                const-string v4, "{\"orderId\":\"GPA.1234-5678-9012-34567\",\"packageName\":\"com.ubisoft.hungrysharkworld\",\"productId\":\"REPLACE_SKU\",\"productIds\":[\"REPLACE_SKU\"],\"purchaseTime\":1700000000000,\"purchaseState\":1,\"purchaseToken\":\"morphe_token\",\"quantity\":1,\"acknowledged\":false}"
                                invoke-virtual {v4, v2, v3}, Ljava/lang/String;->replace(Ljava/lang/CharSequence;Ljava/lang/CharSequence;)Ljava/lang/String;
                                move-result-object v4

                                new-instance v5, Lcom/android/billingclient/api/Purchase;
                                const-string v2, "morphe_sig"
                                invoke-direct {v5, v4, v2}, Lcom/android/billingclient/api/Purchase;-><init>(Ljava/lang/String;Ljava/lang/String;)V

                                new-instance v2, Ljava/util/ArrayList;
                                invoke-direct {v2}, Ljava/util/ArrayList;-><init>()V
                                invoke-virtual {v2, v5}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z

                                invoke-interface {v0, v1, v2}, Lcom/android/billingclient/api/PurchasesUpdatedListener;->onPurchasesUpdated(Lcom/android/billingclient/api/BillingResult;Ljava/util/List;)V

                                :morphe_skip_gbs_dispatch
                                return-void
                                """.trimIndent(),
                            )
                            gbsPurchaseCount++
                            logger.info("Patched GoogleBillingService.$mName to directly dispatch free purchase")
                        } catch (e: Exception) {
                            logger.warning("Failed to patch GoogleBillingService.$mName: ${e.message}")
                        }
                    }

                    // 2c. Bypass validatePurchase in GoogleBillingService
                    if (!isStatic && mName == "validatePurchase" && method.returnType == "V") {
                        try {
                            val clonedVal = cloneMethodWithAdditionalRegisters(method, 6)
                            clonedVal.addInstructions(
                                0,
                                """
                                iget-object v0, p0, Lcom/ubisoft/orion/monetisationcore/billing/GoogleBillingService;->monetisationEvents:Lcom/ubisoft/orion/monetisationcore/MonetisationEvents;
                                if-eqz v0, :morphe_gbs_val_skip
                                const/4 v1, 0x0
                                const-string v2, "OK"
                                const-string v3, "{\"houstonTransactionId\":\"GPA.1234-5678-9012-34567\",\"expiresDate\":\"\",\"expired\":false,\"autoRenewing\":false,\"renewalStatus\":\"\",\"trialPeriod\":false}"
                                invoke-interface {v0, v1, v2, v3}, Lcom/ubisoft/orion/monetisationcore/MonetisationEvents;->OnValidatePurchaseListener(ILjava/lang/String;Ljava/lang/String;)V
                                :morphe_gbs_val_skip
                                return-void
                                """.trimIndent(),
                            )
                            gbsValidationCount++
                            logger.info("Patched GoogleBillingService.validatePurchase")
                        } catch (e: Exception) {
                            logger.warning("Failed to patch GoogleBillingService.validatePurchase: ${e.message}")
                        }
                    }

                    // 2d. Bypass validatePurchaseV2 in GoogleBillingService
                    if (!isStatic && mName == "validatePurchaseV2" && method.returnType == "V") {
                        try {
                            val clonedVal2 = cloneMethodWithAdditionalRegisters(method, 6)
                            clonedVal2.addInstructions(
                                0,
                                """
                                iget-object v0, p0, Lcom/ubisoft/orion/monetisationcore/billing/GoogleBillingService;->monetisationEvents:Lcom/ubisoft/orion/monetisationcore/MonetisationEvents;
                                if-eqz v0, :morphe_gbs_val2_skip
                                const/16 v1, 0xc8
                                const-string v2, "OK"
                                const-string v3, "{\"ubisoftTransactionId\":\"GPA.1234-5678-9012-34567\"}"
                                invoke-interface {v0, v1, v2, v3}, Lcom/ubisoft/orion/monetisationcore/MonetisationEvents;->OnValidatePurchaseV2Listener(ILjava/lang/String;Ljava/lang/String;)V
                                :morphe_gbs_val2_skip
                                return-void
                                """.trimIndent(),
                            )
                            gbsValidationCount++
                            logger.info("Patched GoogleBillingService.validatePurchaseV2")
                        } catch (e: Exception) {
                            logger.warning("Failed to patch GoogleBillingService.validatePurchaseV2: ${e.message}")
                        }
                    }

                    // 2e. Bypass completePurchase in GoogleBillingService
                    if (!isStatic && mName == "completePurchase" && method.returnType == "V" && pTypes.size == 2) {
                        try {
                            val clonedComp = cloneMethodWithAdditionalRegisters(method, 8)
                            clonedComp.addInstructions(
                                0,
                                """
                                iget-object v0, p0, Lcom/ubisoft/orion/monetisationcore/billing/GoogleBillingService;->monetisationEvents:Lcom/ubisoft/orion/monetisationcore/MonetisationEvents;
                                if-eqz v0, :morphe_gbs_comp_skip
                                invoke-static {}, Ljava/lang/System;->getProperties()Ljava/util/Properties;
                                move-result-object v1
                                const-string v2, "morphe_last_sku"
                                invoke-virtual {v1, v2}, Ljava/util/Properties;->get(Ljava/lang/Object;)Ljava/lang/Object;
                                move-result-object v3
                                check-cast v3, Ljava/lang/String;
                                if-nez v3, :morphe_gbs_comp_sku_ok
                                const-string v3, "com.ubisoft.hungrysharkworld.gems_pack_1"
                                :morphe_gbs_comp_sku_ok
                                const-string v2, "REPLACE_SKU"
                                const-string v4, "{\"sku\":\"REPLACE_SKU\",\"orderId\":\"GPA.1234-5678-9012-34567\",\"packageName\":\"com.ubisoft.hungrysharkworld\",\"productId\":\"REPLACE_SKU\",\"productIds\":[\"REPLACE_SKU\"],\"purchaseTime\":1700000000000,\"purchaseState\":1,\"purchaseToken\":\"morphe_token\",\"quantity\":1,\"acknowledged\":true}"
                                invoke-virtual {v4, v2, v3}, Ljava/lang/String;->replace(Ljava/lang/CharSequence;Ljava/lang/CharSequence;)Ljava/lang/String;
                                move-result-object v4
                                const/4 v1, 0x0
                                const-string v2, "OK"
                                invoke-interface {v0, v1, v2, v4}, Lcom/ubisoft/orion/monetisationcore/MonetisationEvents;->OnPurchaseCompletedListener(ILjava/lang/String;Ljava/lang/String;)V
                                :morphe_gbs_comp_skip
                                return-void
                                """.trimIndent(),
                            )
                            gbsValidationCount++
                            logger.info("Patched GoogleBillingService.completePurchase")
                        } catch (e: Exception) {
                            logger.warning("Failed to patch GoogleBillingService.completePurchase: ${e.message}")
                        }
                    }

                    // 2f. Bypass acknowledgePurchase(String) in GoogleBillingService
                    if (!isStatic && mName == "acknowledgePurchase" && pTypes.size == 1 && pTypes[0] == "Ljava/lang/String;") {
                        try {
                            val clonedAckStr = cloneMethodWithAdditionalRegisters(method, 6)
                            clonedAckStr.addInstructions(
                                0,
                                """
                                iget-object v0, p0, Lcom/ubisoft/orion/monetisationcore/billing/GoogleBillingService;->monetisationEvents:Lcom/ubisoft/orion/monetisationcore/MonetisationEvents;
                                if-eqz v0, :morphe_gbs_ack_skip
                                const/4 v1, 0x0
                                const-string v2, "OK"
                                const-string v3, "{\"orderId\":\"GPA.1234-5678-9012-34567\"}"
                                invoke-interface {v0, v1, v2, v3}, Lcom/ubisoft/orion/monetisationcore/MonetisationEvents;->OnTransactionFinishedListener(ILjava/lang/String;Ljava/lang/String;)V
                                :morphe_gbs_ack_skip
                                return-void
                                """.trimIndent(),
                            )
                            logger.info("Patched GoogleBillingService.acknowledgePurchase(String)")
                        } catch (e: Exception) {
                            logger.warning("Failed to patch GoogleBillingService.acknowledgePurchase: ${e.message}")
                        }
                    }

                    // 2g. Force isStoreAvailable and isInitialised to true
                    if (!isStatic && (mName == "isStoreAvailable" || mName == "isInitialised") && method.returnType == "Z" && pTypes.isEmpty()) {
                        try {
                            val mutableMethod = mutableClass.findMutableMethodOf(method)
                            mutableMethod.addInstructions(
                                0,
                                """
                                const/4 v0, 0x1
                                return v0
                                """.trimIndent(),
                            )
                            logger.info("Patched GoogleBillingService.$mName -> true")
                        } catch (e: Exception) {
                            logger.warning("Failed to patch GoogleBillingService.$mName: ${e.message}")
                        }
                    }
                }

                // =========================================================================
                // 3. Ubisoft Orion Houston Validation Runnables
                // =========================================================================
                if (classDef.type == "Lcom/ubisoft/orion/monetisationcore/houston/HoustonValidation;") {
                    if (!isStatic && mName == "run" && method.returnType == "V" && pTypes.isEmpty()) {
                        try {
                            val clonedRun = cloneMethodWithAdditionalRegisters(method, 6)
                            clonedRun.addInstructions(
                                0,
                                """
                                iget-object v0, p0, Lcom/ubisoft/orion/monetisationcore/houston/HoustonValidation;->monetisationEvents:Lcom/ubisoft/orion/monetisationcore/MonetisationEvents;
                                if-eqz v0, :morphe_hv_skip
                                const/4 v1, 0x0
                                const-string v2, "OK"
                                const-string v3, "{\"houstonTransactionId\":\"GPA.1234-5678-9012-34567\",\"expiresDate\":\"\",\"expired\":false,\"autoRenewing\":false,\"renewalStatus\":\"\",\"trialPeriod\":false}"
                                invoke-interface {v0, v1, v2, v3}, Lcom/ubisoft/orion/monetisationcore/MonetisationEvents;->OnValidatePurchaseListener(ILjava/lang/String;Ljava/lang/String;)V
                                :morphe_hv_skip
                                return-void
                                """.trimIndent(),
                            )
                            houstonValidationCount++
                            logger.info("Patched HoustonValidation.run")
                        } catch (e: Exception) {
                            logger.warning("Failed to patch HoustonValidation.run: ${e.message}")
                        }
                    }
                }

                if (classDef.type == "Lcom/ubisoft/orion/monetisationcore/houston/HoustonValidationV2;") {
                    if (!isStatic && mName == "run" && method.returnType == "V" && pTypes.isEmpty()) {
                        try {
                            val clonedRun2 = cloneMethodWithAdditionalRegisters(method, 6)
                            clonedRun2.addInstructions(
                                0,
                                """
                                iget-object v0, p0, Lcom/ubisoft/orion/monetisationcore/houston/HoustonValidationV2;->monetisationEvents:Lcom/ubisoft/orion/monetisationcore/MonetisationEvents;
                                if-eqz v0, :morphe_hv2_skip
                                const/16 v1, 0xc8
                                const-string v2, "OK"
                                const-string v3, "{\"ubisoftTransactionId\":\"GPA.1234-5678-9012-34567\"}"
                                invoke-interface {v0, v1, v2, v3}, Lcom/ubisoft/orion/monetisationcore/MonetisationEvents;->OnValidatePurchaseV2Listener(ILjava/lang/String;Ljava/lang/String;)V
                                :morphe_hv2_skip
                                return-void
                                """.trimIndent(),
                            )
                            houstonValidationCount++
                            logger.info("Patched HoustonValidationV2.run")
                        } catch (e: Exception) {
                            logger.warning("Failed to patch HoustonValidationV2.run: ${e.message}")
                        }
                    }
                }

                // =========================================================================
                // 4. PurchasesUpdatedListener Implementations & Unity GooglePlayPurchasing
                // =========================================================================
                val implementsListener = classDef.interfaces.contains("Lcom/android/billingclient/api/PurchasesUpdatedListener;") ||
                    classDef.type.contains("PurchasesUpdatedListener") ||
                    classDef.type.contains("GooglePlayPurchasing") ||
                    classDef.type.contains("Purchasing")

                if (implementsListener) {
                    val isPurchaseMethod = !isStatic && (
                        mName.equals("purchase", ignoreCase = true) ||
                        mName.equals("startPurchase", ignoreCase = true) ||
                        mName.equals("initiatePurchase", ignoreCase = true) ||
                        mName.equals("buy", ignoreCase = true)
                    )

                    // Capture listener instance (if it implements PurchasesUpdatedListener) and SKU in purchase(...)
                    if (isPurchaseMethod) {
                        try {
                            val clonedCapture = cloneMethodWithAdditionalRegisters(method, 4)
                            val saveSkuCode = if (pTypes.isNotEmpty() && pTypes[0] == "Ljava/lang/String;") {
                                """
                                move-object/from16 v3, p1
                                if-eqz v3, :morphe_skip_save_sku
                                const-string v1, "morphe_last_sku"
                                invoke-virtual {v0, v1, v3}, Ljava/util/Properties;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
                                :morphe_skip_save_sku
                                """.trimIndent()
                            } else ""

                            clonedCapture.addInstructions(
                                0,
                                """
                                invoke-static {}, Ljava/lang/System;->getProperties()Ljava/util/Properties;
                                move-result-object v0

                                instance-of v2, p0, Lcom/android/billingclient/api/PurchasesUpdatedListener;
                                if-eqz v2, :morphe_skip_save_listener
                                const-string v1, "morphe_billing_listener"
                                invoke-virtual {v0, v1, p0}, Ljava/util/Properties;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
                                :morphe_skip_save_listener

                                $saveSkuCode
                                """.trimIndent(),
                            )
                            purchaseCaptureCount++
                            logger.info("Patched $mName to capture PurchasesUpdatedListener and SKU in ${classDef.type}")
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
                                move-object/from16 v2, p0
                                invoke-static {}, Ljava/lang/System;->getProperties()Ljava/util/Properties;
                                move-result-object v0
                                const-string v1, "morphe_billing_listener"
                                invoke-virtual {v0, v1, v2}, Ljava/util/Properties;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

                                invoke-static {}, Lcom/android/billingclient/api/BillingResult;->newBuilder()Lcom/android/billingclient/api/BillingResult${'$'}Builder;
                                move-result-object v0
                                const/4 v1, 0x0
                                invoke-virtual {v0, v1}, Lcom/android/billingclient/api/BillingResult${'$'}Builder;->setResponseCode(I)Lcom/android/billingclient/api/BillingResult${'$'}Builder;
                                move-result-object v0
                                invoke-virtual {v0}, Lcom/android/billingclient/api/BillingResult${'$'}Builder;->build()Lcom/android/billingclient/api/BillingResult;
                                move-result-object v0
                                move-object/from16 p1, v0

                                move-object/from16 v0, p2
                                if-eqz v0, :morphe_p_create

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
                // 5. Receipt Verification Bypass
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
            "gbsPurchase=$gbsPurchaseCount, gbsValidation=$gbsValidationCount, " +
            "houstonValidation=$houstonValidationCount, launchBillingFlow=$launchCount, " +
            "consumeAsync=$consumeCount, acknowledgePurchase=$acknowledgeCount, " +
            "onPurchasesUpdated=$listenerUpdateCount, verifyPurchase=$verifyCount."
        )
        logger.info("Free Shopping patch execution finished.")
    }
}

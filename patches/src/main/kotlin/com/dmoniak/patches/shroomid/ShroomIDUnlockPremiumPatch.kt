package com.dmoniak.patches.shroomid

import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.AccessFlags
import com.dmoniak.patches.shared.BillingHookHelper.executeGooglePlayBillingBypass
import com.dmoniak.patches.shared.Constants.COMPATIBILITY_SHROOMID
import com.dmoniak.patches.shared.replaceMethod
import java.util.logging.Logger

val shroomIDUnlockPremiumPatch = bytecodePatch(
    name = "Unlock Premium & Pro - ShroomID",
    description = "Unlocks full lifetime Premium & Pro subscription features in ShroomID, including AI mushroom identification, detailed edibility guides, spore prints, offline database access, and removes Adapty paywalls.",
) {
    compatibleWith(COMPATIBILITY_SHROOMID)

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        executeShroomIDUnlockPremiumLogic(logger)
    }
}

fun BytecodePatchContext.executeShroomIDUnlockPremiumLogic(logger: Logger) {
    logger.info("Executing Unlock Premium & Pro patch for ShroomID (com.shroomid)...")

    // 1. Google Play Billing Client bypass
    executeGooglePlayBillingBypass(logger, "ShroomID")

    var adaptyModelCount = 0
    var adaptyHandlerCount = 0
    var generalPremiumCount = 0

    classDefForEach { classDef ->
        val type = classDef.type
        val tl = type.lowercase()

        if (!tl.contains("androidx") && !tl.contains("android/support")) {
            val mutableClass by lazy { mutableClassDefBy(classDef) }

            // =========================================================================
            // 1. Adapty AccessLevel and Subscription Models
            // =========================================================================
            val isAdaptyAccessLevel = type.endsWith("/AdaptyProfile${'$'}AccessLevel;")
            val isAdaptySubscription = type.endsWith("/AdaptyProfile${'$'}Subscription;")

            if (isAdaptyAccessLevel || isAdaptySubscription) {
                for (method in classDef.methods.toList()) {
                    if (method.implementation == null) continue
                    val isStatic = AccessFlags.STATIC.isSet(method.accessFlags)
                    val mName = method.name
                    val pTypes = method.parameterTypes
                    val returnType = method.returnType

                    // isActive() -> true
                    if (!isStatic && mName == "isActive" && returnType == "Z" && pTypes.isEmpty()) {
                        try {
                            mutableClass.replaceMethod(
                                method,
                                registerCount = 3,
                                smaliCode = """
                                    const/4 v0, 0x1
                                    return v0
                                """.trimIndent(),
                            )
                            adaptyModelCount++
                            logger.info("[ShroomID Adapty] Hooked ${type}->isActive() -> true")
                        } catch (e: Exception) {
                            logger.warning("[ShroomID Adapty] Failed to hook ${type}->isActive: ${e.message}")
                        }
                    }

                    // isLifetime() -> true
                    if (!isStatic && mName == "isLifetime" && returnType == "Z" && pTypes.isEmpty()) {
                        try {
                            mutableClass.replaceMethod(
                                method,
                                registerCount = 3,
                                smaliCode = """
                                    const/4 v0, 0x1
                                    return v0
                                """.trimIndent(),
                            )
                            adaptyModelCount++
                            logger.info("[ShroomID Adapty] Hooked ${type}->isLifetime() -> true")
                        } catch (e: Exception) {
                            logger.warning("[ShroomID Adapty] Failed to hook ${type}->isLifetime: ${e.message}")
                        }
                    }

                    // getWillRenew() -> true
                    if (!isStatic && mName == "getWillRenew" && returnType == "Z" && pTypes.isEmpty()) {
                        try {
                            mutableClass.replaceMethod(
                                method,
                                registerCount = 3,
                                smaliCode = """
                                    const/4 v0, 0x1
                                    return v0
                                """.trimIndent(),
                            )
                            adaptyModelCount++
                            logger.info("[ShroomID Adapty] Hooked ${type}->getWillRenew() -> true")
                        } catch (e: Exception) {
                            logger.warning("[ShroomID Adapty] Failed to hook ${type}->getWillRenew: ${e.message}")
                        }
                    }

                    // isInGracePeriod() -> false
                    if (!isStatic && mName == "isInGracePeriod" && returnType == "Z" && pTypes.isEmpty()) {
                        try {
                            mutableClass.replaceMethod(
                                method,
                                registerCount = 3,
                                smaliCode = """
                                    const/4 v0, 0x0
                                    return v0
                                """.trimIndent(),
                            )
                            adaptyModelCount++
                            logger.info("[ShroomID Adapty] Hooked ${type}->isInGracePeriod() -> false")
                        } catch (e: Exception) {
                            logger.warning("[ShroomID Adapty] Failed to hook ${type}->isInGracePeriod: ${e.message}")
                        }
                    }

                    // isRefund() -> false
                    if (!isStatic && mName == "isRefund" && returnType == "Z" && pTypes.isEmpty()) {
                        try {
                            mutableClass.replaceMethod(
                                method,
                                registerCount = 3,
                                smaliCode = """
                                    const/4 v0, 0x0
                                    return v0
                                """.trimIndent(),
                            )
                            adaptyModelCount++
                            logger.info("[ShroomID Adapty] Hooked ${type}->isRefund() -> false")
                        } catch (e: Exception) {
                            logger.warning("[ShroomID Adapty] Failed to hook ${type}->isRefund: ${e.message}")
                        }
                    }

                    // isSandbox() -> false (Subscription only)
                    if (!isStatic && mName == "isSandbox" && returnType == "Z" && pTypes.isEmpty()) {
                        try {
                            mutableClass.replaceMethod(
                                method,
                                registerCount = 3,
                                smaliCode = """
                                    const/4 v0, 0x0
                                    return v0
                                """.trimIndent(),
                            )
                            adaptyModelCount++
                            logger.info("[ShroomID Adapty] Hooked ${type}->isSandbox() -> false")
                        } catch (e: Exception) {
                            logger.warning("[ShroomID Adapty] Failed to hook ${type}->isSandbox: ${e.message}")
                        }
                    }
                }
            }

            // =========================================================================
            // 2. AdaptyCallHandler Crossplatform Serialization Hook
            // =========================================================================
            if (type.endsWith("/crossplatform/AdaptyCallHandler;")) {
                for (method in classDef.methods.toList()) {
                    if (method.implementation == null) continue
                    val mName = method.name
                    val pTypes = method.parameterTypes
                    val returnType = method.returnType

                    if (mName == "toSuccessJson" && returnType == "Ljava/lang/String;" && pTypes.size == 1 &&
                        pTypes[0].toString() == "Ljava/lang/Object;"
                    ) {
                        try {
                            mutableClass.replaceMethod(
                                method,
                                registerCount = 6,
                                smaliCode = """
                                    const-string v0, "success"
                                    invoke-static {v0, p1}, Lkotlin/TuplesKt;->to(Ljava/lang/Object;Ljava/lang/Object;)Lkotlin/Pair;
                                    move-result-object v0
                                    invoke-static {v0}, Lkotlin/collections/MapsKt;->mapOf(Lkotlin/Pair;)Ljava/util/Map;
                                    move-result-object v0
                                    invoke-static {v0}, Lcom/adapty/internal/crossplatform/SerializationHelper;->toJson(Ljava/lang/Object;)Ljava/lang/String;
                                    move-result-object v0
                                    if-nez v0, :cond_ret
                                    const-string v1, "\"access_levels\":{}"
                                    const-string v2, "\"access_levels\":{\"premium\":{\"id\":\"premium\",\"is_active\":true,\"is_lifetime\":true,\"vendor_product_id\":\"com.shroomid.premium.yearly\",\"store\":\"play_store\",\"activated_at\":\"2024-01-01T00:00:00.000Z\",\"renewed_at\":\"2024-01-01T00:00:00.000Z\",\"expires_at\":\"2099-01-01T00:00:00.000Z\",\"starts_at\":\"2024-01-01T00:00:00.000Z\",\"is_in_grace_period\":false,\"is_refund\":false,\"will_renew\":true},\"all_access\":{\"id\":\"all_access\",\"is_active\":true,\"is_lifetime\":true,\"vendor_product_id\":\"com.shroomid.premium.yearly\",\"store\":\"play_store\",\"activated_at\":\"2024-01-01T00:00:00.000Z\",\"renewed_at\":\"2024-01-01T00:00:00.000Z\",\"expires_at\":\"2099-01-01T00:00:00.000Z\",\"starts_at\":\"2024-01-01T00:00:00.000Z\",\"is_in_grace_period\":false,\"is_refund\":false,\"will_renew\":true}}"
                                    invoke-virtual {v0, v1, v2}, Ljava/lang/String;->replace(Ljava/lang/CharSequence;Ljava/lang/CharSequence;)Ljava/lang/String;
                                    move-result-object v0
                                    const-string v1, "\"paid_access_levels\":{}"
                                    const-string v2, "\"paid_access_levels\":{\"premium\":{\"id\":\"premium\",\"is_active\":true,\"is_lifetime\":true,\"vendor_product_id\":\"com.shroomid.premium.yearly\",\"store\":\"play_store\",\"activated_at\":\"2024-01-01T00:00:00.000Z\",\"renewed_at\":\"2024-01-01T00:00:00.000Z\",\"expires_at\":\"2099-01-01T00:00:00.000Z\",\"starts_at\":\"2024-01-01T00:00:00.000Z\",\"is_in_grace_period\":false,\"is_refund\":false,\"will_renew\":true},\"all_access\":{\"id\":\"all_access\",\"is_active\":true,\"is_lifetime\":true,\"vendor_product_id\":\"com.shroomid.premium.yearly\",\"store\":\"play_store\",\"activated_at\":\"2024-01-01T00:00:00.000Z\",\"renewed_at\":\"2024-01-01T00:00:00.000Z\",\"expires_at\":\"2099-01-01T00:00:00.000Z\",\"starts_at\":\"2024-01-01T00:00:00.000Z\",\"is_in_grace_period\":false,\"is_refund\":false,\"will_renew\":true}}"
                                    invoke-virtual {v0, v1, v2}, Ljava/lang/String;->replace(Ljava/lang/CharSequence;Ljava/lang/CharSequence;)Ljava/lang/String;
                                    move-result-object v0
                                    const-string v1, "\"subscriptions\":{}"
                                    const-string v2, "\"subscriptions\":{\"com.shroomid.premium.yearly\":{\"is_active\":true,\"is_lifetime\":true,\"vendor_product_id\":\"com.shroomid.premium.yearly\",\"store\":\"play_store\",\"activated_at\":\"2024-01-01T00:00:00.000Z\",\"renewed_at\":\"2024-01-01T00:00:00.000Z\",\"expires_at\":\"2099-01-01T00:00:00.000Z\",\"starts_at\":\"2024-01-01T00:00:00.000Z\",\"is_in_grace_period\":false,\"is_refund\":false,\"is_sandbox\":false,\"will_renew\":true}}"
                                    invoke-virtual {v0, v1, v2}, Ljava/lang/String;->replace(Ljava/lang/CharSequence;Ljava/lang/CharSequence;)Ljava/lang/String;
                                    move-result-object v0
                                    const-string v1, "\"is_active\":false"
                                    const-string v2, "\"is_active\":true"
                                    invoke-virtual {v0, v1, v2}, Ljava/lang/String;->replace(Ljava/lang/CharSequence;Ljava/lang/CharSequence;)Ljava/lang/String;
                                    move-result-object v0
                                    const-string v1, "\"is_lifetime\":false"
                                    const-string v2, "\"is_lifetime\":true"
                                    invoke-virtual {v0, v1, v2}, Ljava/lang/String;->replace(Ljava/lang/CharSequence;Ljava/lang/CharSequence;)Ljava/lang/String;
                                    move-result-object v0
                                    :cond_ret
                                    return-object v0
                                """.trimIndent(),
                            )
                            adaptyHandlerCount++
                            logger.info("[ShroomID Adapty] Hooked AdaptyCallHandler->toSuccessJson to inject active access levels")
                        } catch (e: Exception) {
                            logger.warning("[ShroomID Adapty] Failed to hook AdaptyCallHandler->toSuccessJson: ${e.message}")
                        }
                    }
                }
            }

            // =========================================================================
            // 3. General ShroomID / React Native Premium checks
            // =========================================================================
            if (tl.contains("shroomid") || tl.contains("subscription") || tl.contains("premium")) {
                for (method in classDef.methods.toList()) {
                    if (method.implementation == null) continue
                    val isStatic = AccessFlags.STATIC.isSet(method.accessFlags)
                    val mName = method.name
                    val mNameLower = mName.lowercase()
                    val pTypes = method.parameterTypes
                    val returnType = method.returnType

                    val isPremiumMethod = (
                        mNameLower == "ispremium" ||
                        mNameLower == "ispro" ||
                        mNameLower == "issubscribed" ||
                        mNameLower == "hasactivepurchase" ||
                        mNameLower == "hasactivesubscription"
                    ) && !isStatic && pTypes.isEmpty()

                    if (isPremiumMethod) {
                        try {
                            if (returnType == "Z") {
                                mutableClass.replaceMethod(
                                    method,
                                    registerCount = 3,
                                    smaliCode = """
                                        const/4 v0, 0x1
                                        return v0
                                    """.trimIndent(),
                                )
                                generalPremiumCount++
                                logger.info("[ShroomID Premium] Hooked ${type}->${method.name}() -> true")
                            } else if (returnType == "Ljava/lang/Boolean;") {
                                mutableClass.replaceMethod(
                                    method,
                                    registerCount = 3,
                                    smaliCode = """
                                        sget-object v0, Ljava/lang/Boolean;->TRUE:Ljava/lang/Boolean;
                                        return-object v0
                                    """.trimIndent(),
                                )
                                generalPremiumCount++
                                logger.info("[ShroomID Premium] Hooked ${type}->${method.name}() -> Boolean.TRUE")
                            }
                        } catch (e: Exception) {
                            logger.fine("[ShroomID Premium] Failed to hook ${method.name}: ${e.message}")
                        }
                    }
                }
            }
        }
    }

    logger.info(
        "[ShroomID Premium] Patch completed: " +
            "adaptyModelCount=$adaptyModelCount, " +
            "adaptyHandlerCount=$adaptyHandlerCount, " +
            "generalPremiumCount=$generalPremiumCount",
    )
}

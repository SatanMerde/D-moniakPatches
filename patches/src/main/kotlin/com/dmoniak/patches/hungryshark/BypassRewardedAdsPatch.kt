package com.dmoniak.patches.hungryshark

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import com.android.tools.smali.dexlib2.AccessFlags
import com.dmoniak.patches.shared.Constants.COMPATIBILITY_HUNGRY_SHARK_WORLD

// ============================================================================
// Smali snippets for MAX Unity events (injected directly, no Java extension)
// Matches the exact approach of Nai64/Nai64Patches AdsFreeRewardsPatch.kt
// ============================================================================

/**
 * Builds the smali block that sends the three MAX Unity lifecycle events
 * (Displayed → ReceivedReward → Hidden) synchronously via the static
 * MaxUnityAdManager.forwardUnityEvent(JSONObject) call.
 *
 * p1 must hold the adUnitId String register at call time.
 * Returns: smali string ready for addInstructions(0, ...).
 */
private fun maxUnityShowSmali(adUnitIdParam: String): String = """
    move-object/from16 v0, $adUnitIdParam
    new-instance v1, Lorg/json/JSONObject;
    invoke-direct {v1}, Lorg/json/JSONObject;-><init>()V
    const-string v2, "name"
    const-string v3, "OnRewardedAdDisplayedEvent"
    invoke-static {v1, v2, v3}, Lcom/applovin/impl/sdk/utils/JsonUtils;->putString(Lorg/json/JSONObject;Ljava/lang/String;Ljava/lang/String;)V
    const-string v2, "adUnitId"
    invoke-static {v1, v2, v0}, Lcom/applovin/impl/sdk/utils/JsonUtils;->putString(Lorg/json/JSONObject;Ljava/lang/String;Ljava/lang/String;)V
    const-string v2, "adFormat"
    const-string v3, "rewarded"
    invoke-static {v1, v2, v3}, Lcom/applovin/impl/sdk/utils/JsonUtils;->putString(Lorg/json/JSONObject;Ljava/lang/String;Ljava/lang/String;)V
    invoke-static {v1}, Lcom/applovin/mediation/unity/MaxUnityAdManager;->forwardUnityEvent(Lorg/json/JSONObject;)V
    new-instance v1, Lorg/json/JSONObject;
    invoke-direct {v1}, Lorg/json/JSONObject;-><init>()V
    const-string v2, "name"
    const-string v3, "OnRewardedAdReceivedRewardEvent"
    invoke-static {v1, v2, v3}, Lcom/applovin/impl/sdk/utils/JsonUtils;->putString(Lorg/json/JSONObject;Ljava/lang/String;Ljava/lang/String;)V
    const-string v2, "adUnitId"
    invoke-static {v1, v2, v0}, Lcom/applovin/impl/sdk/utils/JsonUtils;->putString(Lorg/json/JSONObject;Ljava/lang/String;Ljava/lang/String;)V
    const-string v2, "adFormat"
    const-string v3, "rewarded"
    invoke-static {v1, v2, v3}, Lcom/applovin/impl/sdk/utils/JsonUtils;->putString(Lorg/json/JSONObject;Ljava/lang/String;Ljava/lang/String;)V
    const-string v2, "rewardLabel"
    const-string v3, "reward"
    invoke-static {v1, v2, v3}, Lcom/applovin/impl/sdk/utils/JsonUtils;->putString(Lorg/json/JSONObject;Ljava/lang/String;Ljava/lang/String;)V
    const-string v2, "rewardAmount"
    const-string v3, "1"
    invoke-static {v1, v2, v3}, Lcom/applovin/impl/sdk/utils/JsonUtils;->putString(Lorg/json/JSONObject;Ljava/lang/String;Ljava/lang/String;)V
    invoke-static {v1}, Lcom/applovin/mediation/unity/MaxUnityAdManager;->forwardUnityEvent(Lorg/json/JSONObject;)V
    new-instance v1, Lorg/json/JSONObject;
    invoke-direct {v1}, Lorg/json/JSONObject;-><init>()V
    const-string v2, "name"
    const-string v3, "OnRewardedAdHiddenEvent"
    invoke-static {v1, v2, v3}, Lcom/applovin/impl/sdk/utils/JsonUtils;->putString(Lorg/json/JSONObject;Ljava/lang/String;Ljava/lang/String;)V
    const-string v2, "adUnitId"
    invoke-static {v1, v2, v0}, Lcom/applovin/impl/sdk/utils/JsonUtils;->putString(Lorg/json/JSONObject;Ljava/lang/String;Ljava/lang/String;)V
    const-string v2, "adFormat"
    const-string v3, "rewarded"
    invoke-static {v1, v2, v3}, Lcom/applovin/impl/sdk/utils/JsonUtils;->putString(Lorg/json/JSONObject;Ljava/lang/String;Ljava/lang/String;)V
    invoke-static {v1}, Lcom/applovin/mediation/unity/MaxUnityAdManager;->forwardUnityEvent(Lorg/json/JSONObject;)V
    return-void
""".trimIndent()

/**
 * Builds the smali block that sends OnRewardedAdLoadedEvent via
 * MaxUnityAdManager.forwardUnityEvent(JSONObject).
 */
private fun maxUnityLoadSmali(adUnitIdParam: String): String = """
    move-object/from16 v0, $adUnitIdParam
    new-instance v1, Lorg/json/JSONObject;
    invoke-direct {v1}, Lorg/json/JSONObject;-><init>()V
    const-string v2, "name"
    const-string v3, "OnRewardedAdLoadedEvent"
    invoke-static {v1, v2, v3}, Lcom/applovin/impl/sdk/utils/JsonUtils;->putString(Lorg/json/JSONObject;Ljava/lang/String;Ljava/lang/String;)V
    const-string v2, "adUnitId"
    invoke-static {v1, v2, v0}, Lcom/applovin/impl/sdk/utils/JsonUtils;->putString(Lorg/json/JSONObject;Ljava/lang/String;Ljava/lang/String;)V
    const-string v2, "adFormat"
    const-string v3, "rewarded"
    invoke-static {v1, v2, v3}, Lcom/applovin/impl/sdk/utils/JsonUtils;->putString(Lorg/json/JSONObject;Ljava/lang/String;Ljava/lang/String;)V
    invoke-static {v1}, Lcom/applovin/mediation/unity/MaxUnityAdManager;->forwardUnityEvent(Lorg/json/JSONObject;)V
    return-void
""".trimIndent()

/** Force the method to return true (1) immediately. */
private fun MutableMethod.forceReturnTrue() {
    if (this.implementation != null) {
        this.addInstructions(
            0,
            """
                const/4 v0, 0x1
                return v0
            """.trimIndent()
        )
    }
}

private fun MutableMethod.safeAddInstructions(index: Int, smali: String) {
    if (this.implementation != null) {
        this.addInstructions(index, smali.trimIndent())
    }
}

@Suppress("unused")
val bypassRewardedAdsPatch = bytecodePatch(
    name = "Bypass Rewarded Ads",
    description = "Allows claiming all ad rewards (free revives, coin/gem multipliers, shop chests, spins) immediately without watching ads in Hungry Shark World.",
    default = true
) {
    compatibleWith(COMPATIBILITY_HUNGRY_SHARK_WORLD)

    // NOTE: No extendWith() — no Java extension needed; smali is injected directly.

    execute {
        val patchedMethods = mutableSetOf<String>()

        fun patchOnce(method: MutableMethod, tag: String, block: (MutableMethod) -> Unit) {
            val key = "${method.definingClass}->${method.name}${method.parameterTypes}"
            if (patchedMethods.add(key)) {
                block(method)
            }
        }

        fun hookReadinessCheck(method: MutableMethod?) {
            method?.let { m ->
                patchOnce(m, "isReady") { it.forceReturnTrue() }
            }
        }

        // ====================================================================
        // 1. Force Ad Availability / Readiness across all SDKs & wrappers
        // ====================================================================
        hookReadinessCheck(IsRewardedAdReadyFingerprint.matchOrNull()?.method)
        hookReadinessCheck(IsRewardedAdReady0ArgFingerprint.matchOrNull()?.method)
        hookReadinessCheck(MaxUnityPluginIsRewardedAdReadyFingerprint.matchOrNull()?.method)
        hookReadinessCheck(MaxUnityAdManagerIsRewardedAdReadyFingerprint.matchOrNull()?.method)
        hookReadinessCheck(MaxRewardedAdIsReadyFingerprint.matchOrNull()?.method)
        hookReadinessCheck(MaxRewardedAdImplIsReadyFingerprint.matchOrNull()?.method)
        hookReadinessCheck(MaxInterstitialAdIsReadyFingerprint.matchOrNull()?.method)
        hookReadinessCheck(MaxAppOpenAdIsReadyFingerprint.matchOrNull()?.method)
        hookReadinessCheck(GoogleUnityRewardedAdCanShowFingerprint.matchOrNull()?.method)
        hookReadinessCheck(UnityAdsAdvertisementIsReadyFingerprint.matchOrNull()?.method)
        hookReadinessCheck(UnityAdsAdvertisementIsReadyPlacementFingerprint.matchOrNull()?.method)
        hookReadinessCheck(UnityAdsSdkIsReadyFingerprint.matchOrNull()?.method)
        hookReadinessCheck(IronSourceIsRewardedVideoAvailableFingerprint.matchOrNull()?.method)
        hookReadinessCheck(IronSourceIsInterstitialReadyFingerprint.matchOrNull()?.method)
        hookReadinessCheck(LevelPlayRewardedAdIsReadyFingerprint.matchOrNull()?.method)
        hookReadinessCheck(IronSourceUnityRewardedAdIsReadyFingerprint.matchOrNull()?.method)
        hookReadinessCheck(IronSourceAdsRewardedIsReadyPreciseFingerprint.matchOrNull()?.method)

        // ====================================================================
        // 2. Intercept loadRewardedAd — emit OnRewardedAdLoadedEvent then return
        // ====================================================================

        // Primary: unbound 1-arg showRewardedAd/loadRewardedAd (Nai64 approach)
        ShowRewardedAdFingerprint.matchOrNull()?.method?.let { m ->
            patchOnce(m, "showRewardedAd-primary") { target ->
                // p1 = adUnitId (String)
                target.safeAddInstructions(0, maxUnityShowSmali("p1"))
            }
        }

        LoadRewardedAdFingerprint.matchOrNull()?.method?.let { m ->
            patchOnce(m, "loadRewardedAd-primary") { target ->
                // p1 = adUnitId (String)
                target.safeAddInstructions(0, maxUnityLoadSmali("p1"))
            }
        }

        // Additional unbound variants (2- and 3-arg show, 0-arg load)
        ShowRewardedAd3ArgFingerprint.matchOrNull()?.method?.let { m ->
            patchOnce(m, "showRewardedAd-3arg") { target ->
                target.safeAddInstructions(0, maxUnityShowSmali("p1"))
            }
        }

        ShowRewardedAd2ArgFingerprint.matchOrNull()?.method?.let { m ->
            patchOnce(m, "showRewardedAd-2arg") { target ->
                target.safeAddInstructions(0, maxUnityShowSmali("p1"))
            }
        }

        ShowRewardedAd1ArgFingerprint.matchOrNull()?.method?.let { m ->
            patchOnce(m, "showRewardedAd-1arg") { target ->
                target.safeAddInstructions(0, maxUnityShowSmali("p1"))
            }
        }

        LoadRewardedAd0ArgFingerprint.matchOrNull()?.method?.let { m ->
            patchOnce(m, "loadRewardedAd-0arg") { target ->
                // No adUnitId param → use a literal string
                target.safeAddInstructions(
                    0,
                    """
                        const-string v0, "rewardedAd"
                        new-instance v1, Lorg/json/JSONObject;
                        invoke-direct {v1}, Lorg/json/JSONObject;-><init>()V
                        const-string v2, "name"
                        const-string v3, "OnRewardedAdLoadedEvent"
                        invoke-static {v1, v2, v3}, Lcom/applovin/impl/sdk/utils/JsonUtils;->putString(Lorg/json/JSONObject;Ljava/lang/String;Ljava/lang/String;)V
                        const-string v2, "adUnitId"
                        invoke-static {v1, v2, v0}, Lcom/applovin/impl/sdk/utils/JsonUtils;->putString(Lorg/json/JSONObject;Ljava/lang/String;Ljava/lang/String;)V
                        const-string v2, "adFormat"
                        const-string v3, "rewarded"
                        invoke-static {v1, v2, v3}, Lcom/applovin/impl/sdk/utils/JsonUtils;->putString(Lorg/json/JSONObject;Ljava/lang/String;Ljava/lang/String;)V
                        invoke-static {v1}, Lcom/applovin/mediation/unity/MaxUnityAdManager;->forwardUnityEvent(Lorg/json/JSONObject;)V
                        return-void
                    """
                )
            }
        }

        // Explicit class bindings for MaxUnityPlugin / MaxUnityAdManager
        MaxUnityPluginLoadRewardedAdFingerprint.matchOrNull()?.method?.let { m ->
            patchOnce(m, "MaxUnityPlugin-load") { target ->
                val isStatic = AccessFlags.STATIC.isSet(target.accessFlags)
                val paramCount = target.parameterTypes.size
                val adUnitParam = if (isStatic && paramCount >= 1) "p0"
                                  else if (!isStatic && paramCount >= 1) "p1"
                                  else null
                if (adUnitParam != null) {
                    target.safeAddInstructions(0, maxUnityLoadSmali(adUnitParam))
                } else {
                    target.safeAddInstructions(0, """
                        const-string v0, "rewardedAd"
                        new-instance v1, Lorg/json/JSONObject;
                        invoke-direct {v1}, Lorg/json/JSONObject;-><init>()V
                        const-string v2, "name"
                        const-string v3, "OnRewardedAdLoadedEvent"
                        invoke-static {v1, v2, v3}, Lcom/applovin/impl/sdk/utils/JsonUtils;->putString(Lorg/json/JSONObject;Ljava/lang/String;Ljava/lang/String;)V
                        const-string v2, "adUnitId"
                        invoke-static {v1, v2, v0}, Lcom/applovin/impl/sdk/utils/JsonUtils;->putString(Lorg/json/JSONObject;Ljava/lang/String;Ljava/lang/String;)V
                        const-string v2, "adFormat"
                        const-string v3, "rewarded"
                        invoke-static {v1, v2, v3}, Lcom/applovin/impl/sdk/utils/JsonUtils;->putString(Lorg/json/JSONObject;Ljava/lang/String;Ljava/lang/String;)V
                        invoke-static {v1}, Lcom/applovin/mediation/unity/MaxUnityAdManager;->forwardUnityEvent(Lorg/json/JSONObject;)V
                        return-void
                    """)
                }
            }
        }

        MaxUnityAdManagerLoadRewardedAdFingerprint.matchOrNull()?.method?.let { m ->
            patchOnce(m, "MaxUnityAdManager-load") { target ->
                val isStatic = AccessFlags.STATIC.isSet(target.accessFlags)
                val paramCount = target.parameterTypes.size
                val adUnitParam = if (isStatic && paramCount >= 1) "p0"
                                  else if (!isStatic && paramCount >= 1) "p1"
                                  else null
                if (adUnitParam != null) {
                    target.safeAddInstructions(0, maxUnityLoadSmali(adUnitParam))
                } else {
                    target.safeAddInstructions(0, """
                        const-string v0, "rewardedAd"
                        new-instance v1, Lorg/json/JSONObject;
                        invoke-direct {v1}, Lorg/json/JSONObject;-><init>()V
                        const-string v2, "name"
                        const-string v3, "OnRewardedAdLoadedEvent"
                        invoke-static {v1, v2, v3}, Lcom/applovin/impl/sdk/utils/JsonUtils;->putString(Lorg/json/JSONObject;Ljava/lang/String;Ljava/lang/String;)V
                        const-string v2, "adUnitId"
                        invoke-static {v1, v2, v0}, Lcom/applovin/impl/sdk/utils/JsonUtils;->putString(Lorg/json/JSONObject;Ljava/lang/String;Ljava/lang/String;)V
                        const-string v2, "adFormat"
                        const-string v3, "rewarded"
                        invoke-static {v1, v2, v3}, Lcom/applovin/impl/sdk/utils/JsonUtils;->putString(Lorg/json/JSONObject;Ljava/lang/String;Ljava/lang/String;)V
                        invoke-static {v1}, Lcom/applovin/mediation/unity/MaxUnityAdManager;->forwardUnityEvent(Lorg/json/JSONObject;)V
                        return-void
                    """)
                }
            }
        }

        MaxUnityPluginShowRewardedAdFingerprint.matchOrNull()?.method?.let { m ->
            patchOnce(m, "MaxUnityPlugin-show") { target ->
                val isStatic = AccessFlags.STATIC.isSet(target.accessFlags)
                val adUnitParam = if (isStatic) "p0" else "p1"
                target.safeAddInstructions(0, maxUnityShowSmali(adUnitParam))
            }
        }

        MaxUnityAdManagerShowRewardedAdFingerprint.matchOrNull()?.method?.let { m ->
            patchOnce(m, "MaxUnityAdManager-show") { target ->
                val isStatic = AccessFlags.STATIC.isSet(target.accessFlags)
                val adUnitParam = if (isStatic) "p0" else "p1"
                target.safeAddInstructions(0, maxUnityShowSmali(adUnitParam))
            }
        }

        // ====================================================================
        // 3. Unity Ads Interception (v3 RewardedAd + v4)
        // ====================================================================
        UnityAdsV4Show3ArgFingerprint.matchOrNull()?.let { match ->
            patchOnce(match.method, "unityAdsV4Show3") { m ->
                m.safeAddInstructions(
                    0,
                    """
                        move-object/from16 v1, p2
                        move-object/from16 v2, p1
                        invoke-interface {v1, v2}, Lcom/unity3d/ads/IUnityAdsShowListener;->onUnityAdsShowStart(Ljava/lang/String;)V
                        sget-object v0, Lcom/unity3d/ads/UnityAds${'$'}UnityAdsShowCompletionState;->COMPLETED:Lcom/unity3d/ads/UnityAds${'$'}UnityAdsShowCompletionState;
                        invoke-interface {v1, v2, v0}, Lcom/unity3d/ads/IUnityAdsShowListener;->onUnityAdsShowComplete(Ljava/lang/String;Lcom/unity3d/ads/UnityAds${'$'}UnityAdsShowCompletionState;)V
                        return-void
                    """
                )
            }
        }

        UnityAdsV4Show4ArgFingerprint.matchOrNull()?.let { match ->
            patchOnce(match.method, "unityAdsV4Show4") { m ->
                m.safeAddInstructions(
                    0,
                    """
                        move-object/from16 v1, p3
                        move-object/from16 v2, p1
                        invoke-interface {v1, v2}, Lcom/unity3d/ads/IUnityAdsShowListener;->onUnityAdsShowStart(Ljava/lang/String;)V
                        sget-object v0, Lcom/unity3d/ads/UnityAds${'$'}UnityAdsShowCompletionState;->COMPLETED:Lcom/unity3d/ads/UnityAds${'$'}UnityAdsShowCompletionState;
                        invoke-interface {v1, v2, v0}, Lcom/unity3d/ads/IUnityAdsShowListener;->onUnityAdsShowComplete(Ljava/lang/String;Lcom/unity3d/ads/UnityAds${'$'}UnityAdsShowCompletionState;)V
                        return-void
                    """
                )
            }
        }

        UnityRewardedAdShowFingerprint.matchOrNull()?.let { match ->
            patchOnce(match.method, "unityRewardedAdShow") { m ->
                m.safeAddInstructions(
                    0,
                    """
                        move-object/from16 v0, p3
                        move-object/from16 v1, p0
                        invoke-interface {v0, v1}, Lcom/unity3d/ads/RewardedShowListener;->onRewarded(Lcom/unity3d/ads/RewardedAd;)V
                        invoke-interface {v0, v1}, Lcom/unity3d/ads/ShowListener;->onStarted(Ljava/lang/Object;)V
                        sget-object v2, Lcom/unity3d/ads/ShowFinishState;->COMPLETED:Lcom/unity3d/ads/ShowFinishState;
                        invoke-interface {v0, v1, v2}, Lcom/unity3d/ads/ShowListener;->onCompleted(Ljava/lang/Object;Lcom/unity3d/ads/ShowFinishState;)V
                        return-void
                    """
                )
            }
        }

        // ====================================================================
        // 4. IronSource Interception
        // ====================================================================
        IronSourceAdsRewardedShowPreciseFingerprint.matchOrNull()?.let { match ->
            patchOnce(match.method, "ironSourceAdsRewardedShowPrecise") { m ->
                m.safeAddInstructions(
                    0,
                    """
                        move-object/from16 v1, p0
                        invoke-virtual {v1}, Lcom/unity3d/ironsourceads/rewarded/RewardedAd;->getListener()Lcom/unity3d/ironsourceads/rewarded/RewardedAdListener;
                        move-result-object v0
                        if-eqz v0, :morphe_isads_done
                        invoke-interface {v0, v1}, Lcom/unity3d/ironsourceads/rewarded/RewardedAdListener;->onRewardedAdShown(Lcom/unity3d/ironsourceads/rewarded/RewardedAd;)V
                        invoke-interface {v0, v1}, Lcom/unity3d/ironsourceads/rewarded/RewardedAdListener;->onUserEarnedReward(Lcom/unity3d/ironsourceads/rewarded/RewardedAd;)V
                        invoke-interface {v0, v1}, Lcom/unity3d/ironsourceads/rewarded/RewardedAdListener;->onRewardedAdDismissed(Lcom/unity3d/ironsourceads/rewarded/RewardedAd;)V
                        :morphe_isads_done
                        return-void
                    """
                )
            }
        }

        // ====================================================================
        // 5. Google Mobile Ads (Unity plugin UnityRewardedAd)
        // ====================================================================
        GoogleUnityRewardedAdShowFingerprint.matchOrNull()?.let { match ->
            patchOnce(match.method, "googleUnityRewardedShow") { m ->
                m.safeAddInstructions(
                    0,
                    """
                        iget-object v0, p0, Lcom/google/unity/ads/UnityRewardedAd;->callback:Lcom/google/unity/ads/UnityRewardedAdCallback;
                        if-eqz v0, :morphe_admob_done
                        const-string v1, "reward"
                        const/4 v2, 0x1
                        invoke-virtual {v0, v1, v2}, Lcom/google/unity/ads/UnityRewardedAdCallback;->onUserEarnedReward(Ljava/lang/String;F)V
                        :morphe_admob_done
                        return-void
                    """
                )
            }
        }
    }
}

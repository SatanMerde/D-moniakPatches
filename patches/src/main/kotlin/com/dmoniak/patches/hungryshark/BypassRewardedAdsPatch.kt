package com.dmoniak.patches.hungryshark

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import com.android.tools.smali.dexlib2.AccessFlags
import com.dmoniak.patches.shared.Constants.COMPATIBILITY_HUNGRY_SHARK_WORLD

// ============================================================================
// Smali helpers — zero external SDK dependencies.
// Uses JSONObject(String jsonString) constructor directly (Android framework)
// instead of AppLovin-internal JsonUtils, which may be obfuscated or absent.
// ============================================================================

/**
 * Builds a JSON string literal for a MAX Unity lifecycle event.
 * All fields are hardcoded so the smali only needs a const-string + JSONObject ctor.
 */
private fun maxUnityEventJson(eventName: String, adUnitId: String = "") = when {
    adUnitId.isEmpty() ->
        """{"name":"$eventName","adFormat":"rewarded","adUnitId":"rewardedAd"}"""
    else ->
        """{"name":"$eventName","adFormat":"rewarded","adUnitId":"$adUnitId"}"""
}

/**
 * Smali that fires MaxUnityAdManager.forwardUnityEvent() for a given JSON payload,
 * using only v0 and v1 as scratch registers (safe even if method has few local regs,
 * because we need registerCount >= 2 — guaranteed by Morphe's patcher for any real method).
 *
 * The JSON is built with JSONObject(String) — part of Android framework, always present.
 */
private fun forwardEventSmali(jsonLiteral: String): String = """
    const-string v0, "$jsonLiteral"
    new-instance v1, Lorg/json/JSONObject;
    invoke-direct {v1, v0}, Lorg/json/JSONObject;-><init>(Ljava/lang/String;)V
    invoke-static {v1}, Lcom/applovin/mediation/unity/MaxUnityAdManager;->forwardUnityEvent(Lorg/json/JSONObject;)V
""".trimIndent()

/**
 * Full "show" injection: emits Displayed, ReceivedReward (with label+amount), Hidden.
 * adUnitId is hardcoded as empty string — the game already knows its own adUnitId.
 * MaxUnityAdManager routes by event name, not by adUnitId.
 */
private val maxUnityShowSmali: String = buildString {
    appendLine(forwardEventSmali("""{"name":"OnRewardedAdDisplayedEvent","adFormat":"rewarded","adUnitId":"rewardedAd"}"""))
    appendLine(forwardEventSmali("""{"name":"OnRewardedAdReceivedRewardEvent","adFormat":"rewarded","adUnitId":"rewardedAd","rewardLabel":"reward","rewardAmount":"1"}"""))
    appendLine(forwardEventSmali("""{"name":"OnRewardedAdHiddenEvent","adFormat":"rewarded","adUnitId":"rewardedAd"}"""))
    append("return-void")
}

/**
 * Load injection: emits OnRewardedAdLoadedEvent so the game considers ad ready.
 */
private val maxUnityLoadSmali: String = buildString {
    appendLine(forwardEventSmali("""{"name":"OnRewardedAdLoadedEvent","adFormat":"rewarded","adUnitId":"rewardedAd"}"""))
    append("return-void")
}

/** Forces the method to return true immediately. Needs registerCount >= 1. */
private fun MutableMethod.forceReturnTrue() {
    if (this.implementation != null) {
        this.addInstructions(0, "const/4 v0, 0x1\nreturn v0")
    }
}

/** Safely injects smali at position 0. */
private fun MutableMethod.inject(smali: String) {
    if (this.implementation != null) {
        this.addInstructions(0, smali.trimIndent())
    }
}

@Suppress("unused")
val bypassRewardedAdsPatch = bytecodePatch(
    name = "Bypass Rewarded Ads",
    description = "Allows claiming all ad rewards (free revives, coin/gem multipliers, shop chests, spins) immediately without watching ads in Hungry Shark World.",
    default = true
) {
    compatibleWith(COMPATIBILITY_HUNGRY_SHARK_WORLD)

    execute {
        val patched = mutableSetOf<String>()

        fun once(m: MutableMethod, block: (MutableMethod) -> Unit) {
            val key = "${m.definingClass}->${m.name}(${m.parameterTypes.joinToString()})"
            if (patched.add(key)) block(m)
        }

        // ── 1. Force readiness (isReady / isRewardedAdReady → always true) ─────────
        listOf(
            IsRewardedAdReadyFingerprint,
            IsRewardedAdReady0ArgFingerprint,
            MaxUnityPluginIsRewardedAdReadyFingerprint,
            MaxUnityAdManagerIsRewardedAdReadyFingerprint,
            MaxRewardedAdIsReadyFingerprint,
            MaxRewardedAdImplIsReadyFingerprint,
            MaxInterstitialAdIsReadyFingerprint,
            MaxAppOpenAdIsReadyFingerprint,
            GoogleUnityRewardedAdCanShowFingerprint,
            UnityAdsAdvertisementIsReadyFingerprint,
            UnityAdsAdvertisementIsReadyPlacementFingerprint,
            UnityAdsSdkIsReadyFingerprint,
            IronSourceIsRewardedVideoAvailableFingerprint,
            IronSourceIsInterstitialReadyFingerprint,
            LevelPlayRewardedAdIsReadyFingerprint,
            IronSourceUnityRewardedAdIsReadyFingerprint,
            IronSourceAdsRewardedIsReadyPreciseFingerprint,
        ).forEach { fp ->
            fp.matchOrNull()?.method?.let { m -> once(m) { it.forceReturnTrue() } }
        }

        // ── 2. Intercept loadRewardedAd → emit OnRewardedAdLoadedEvent + return ─────
        listOf(
            LoadRewardedAdFingerprint,
            LoadRewardedAd0ArgFingerprint,
            MaxUnityPluginLoadRewardedAdFingerprint,
            MaxUnityAdManagerLoadRewardedAdFingerprint,
        ).forEach { fp ->
            fp.matchOrNull()?.method?.let { m -> once(m) { it.inject(maxUnityLoadSmali) } }
        }

        // ── 3. Intercept showRewardedAd → emit Displayed+Reward+Hidden + return ─────
        listOf(
            ShowRewardedAdFingerprint,
            ShowRewardedAd3ArgFingerprint,
            ShowRewardedAd2ArgFingerprint,
            ShowRewardedAd1ArgFingerprint,
            MaxUnityPluginShowRewardedAdFingerprint,
            MaxUnityAdManagerShowRewardedAdFingerprint,
        ).forEach { fp ->
            fp.matchOrNull()?.method?.let { m -> once(m) { it.inject(maxUnityShowSmali) } }
        }

        // ── 4. Unity Ads SDK v4 ──────────────────────────────────────────────────────
        UnityAdsV4Show3ArgFingerprint.matchOrNull()?.method?.let { m ->
            once(m) { it.inject("""
                move-object/from16 v1, p2
                move-object/from16 v2, p1
                invoke-interface {v1, v2}, Lcom/unity3d/ads/IUnityAdsShowListener;->onUnityAdsShowStart(Ljava/lang/String;)V
                sget-object v0, Lcom/unity3d/ads/UnityAds${'$'}UnityAdsShowCompletionState;->COMPLETED:Lcom/unity3d/ads/UnityAds${'$'}UnityAdsShowCompletionState;
                invoke-interface {v1, v2, v0}, Lcom/unity3d/ads/IUnityAdsShowListener;->onUnityAdsShowComplete(Ljava/lang/String;Lcom/unity3d/ads/UnityAds${'$'}UnityAdsShowCompletionState;)V
                return-void
            """) }
        }

        UnityAdsV4Show4ArgFingerprint.matchOrNull()?.method?.let { m ->
            once(m) { it.inject("""
                move-object/from16 v1, p3
                move-object/from16 v2, p1
                invoke-interface {v1, v2}, Lcom/unity3d/ads/IUnityAdsShowListener;->onUnityAdsShowStart(Ljava/lang/String;)V
                sget-object v0, Lcom/unity3d/ads/UnityAds${'$'}UnityAdsShowCompletionState;->COMPLETED:Lcom/unity3d/ads/UnityAds${'$'}UnityAdsShowCompletionState;
                invoke-interface {v1, v2, v0}, Lcom/unity3d/ads/IUnityAdsShowListener;->onUnityAdsShowComplete(Ljava/lang/String;Lcom/unity3d/ads/UnityAds${'$'}UnityAdsShowCompletionState;)V
                return-void
            """) }
        }

        UnityRewardedAdShowFingerprint.matchOrNull()?.method?.let { m ->
            once(m) { it.inject("""
                move-object/from16 v0, p3
                move-object/from16 v1, p0
                invoke-interface {v0, v1}, Lcom/unity3d/ads/RewardedShowListener;->onRewarded(Lcom/unity3d/ads/RewardedAd;)V
                invoke-interface {v0, v1}, Lcom/unity3d/ads/ShowListener;->onStarted(Ljava/lang/Object;)V
                sget-object v2, Lcom/unity3d/ads/ShowFinishState;->COMPLETED:Lcom/unity3d/ads/ShowFinishState;
                invoke-interface {v0, v1, v2}, Lcom/unity3d/ads/ShowListener;->onCompleted(Ljava/lang/Object;Lcom/unity3d/ads/ShowFinishState;)V
                return-void
            """) }
        }

        // ── 5. IronSource unity bridge ───────────────────────────────────────────────
        IronSourceAdsRewardedShowPreciseFingerprint.matchOrNull()?.method?.let { m ->
            once(m) { it.inject("""
                move-object/from16 v1, p0
                invoke-virtual {v1}, Lcom/unity3d/ironsourceads/rewarded/RewardedAd;->getListener()Lcom/unity3d/ironsourceads/rewarded/RewardedAdListener;
                move-result-object v0
                if-eqz v0, :morphe_isads_done
                invoke-interface {v0, v1}, Lcom/unity3d/ironsourceads/rewarded/RewardedAdListener;->onRewardedAdShown(Lcom/unity3d/ironsourceads/rewarded/RewardedAd;)V
                invoke-interface {v0, v1}, Lcom/unity3d/ironsourceads/rewarded/RewardedAdListener;->onUserEarnedReward(Lcom/unity3d/ironsourceads/rewarded/RewardedAd;)V
                invoke-interface {v0, v1}, Lcom/unity3d/ironsourceads/rewarded/RewardedAdListener;->onRewardedAdDismissed(Lcom/unity3d/ironsourceads/rewarded/RewardedAd;)V
                :morphe_isads_done
                return-void
            """) }
        }
    }
}

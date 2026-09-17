package com.dmoniak.patches.hungryshark

import app.morphe.patcher.Fingerprint

// ============================================================================
// 1. Google Mobile Ads (AdMob)
// ============================================================================

/**
 * Fingerprint for Google Mobile Ads RewardedAd.show(Activity, OnUserEarnedRewardListener).
 */
object GoogleRewardedAdShowFingerprint : Fingerprint(
    name = "show",
    parameters = listOf("Landroid/app/Activity;", "Lcom/google/android/gms/ads/OnUserEarnedRewardListener;"),
    custom = { method, _ -> method.implementation != null }
)

/**
 * Fingerprint for Google Mobile Ads Unity plugin UnityRewardedAd.show().
 */
object GoogleUnityRewardedAdShowFingerprint : Fingerprint(
    definingClass = "Lcom/google/unity/ads/UnityRewardedAd;",
    name = "show",
    parameters = emptyList(),
    custom = { method, _ -> method.implementation != null }
)

/**
 * Fingerprint for Google Mobile Ads Unity plugin canShowAd() readiness check.
 */
object GoogleUnityRewardedAdCanShowFingerprint : Fingerprint(
    definingClass = "Lcom/google/unity/ads/UnityRewardedAd;",
    returnType = "Z",
    name = "canShowAd",
    custom = { method, _ -> method.implementation != null }
)

// ============================================================================
// 2. Universal AppLovin MAX Unity Bridge (Unbound, from Nai64)
// ============================================================================

/**
 * Unbound 3-arg showRewardedAd(String, String, String) - matches any Unity wrapper.
 */
object ShowRewardedAd3ArgFingerprint : Fingerprint(
    name = "showRewardedAd",
    returnType = "V",
    parameters = listOf("Ljava/lang/String;", "Ljava/lang/String;", "Ljava/lang/String;"),
    custom = { method, _ -> method.implementation != null }
)

/**
 * Unbound 2-arg showRewardedAd(String, String).
 */
object ShowRewardedAd2ArgFingerprint : Fingerprint(
    name = "showRewardedAd",
    returnType = "V",
    parameters = listOf("Ljava/lang/String;", "Ljava/lang/String;"),
    custom = { method, _ -> method.implementation != null }
)

/**
 * Unbound 1-arg showRewardedAd(String).
 */
object ShowRewardedAd1ArgFingerprint : Fingerprint(
    name = "showRewardedAd",
    returnType = "V",
    parameters = listOf("Ljava/lang/String;"),
    custom = { method, _ -> method.implementation != null }
)

/**
 * Unbound 1-arg loadRewardedAd(String) - matches any Unity wrapper.
 */
object LoadRewardedAdFingerprint : Fingerprint(
    name = "loadRewardedAd",
    returnType = "V",
    parameters = listOf("Ljava/lang/String;"),
    custom = { method, _ -> method.implementation != null }
)

/**
 * Unbound 0-arg loadRewardedAd().
 */
object LoadRewardedAd0ArgFingerprint : Fingerprint(
    name = "loadRewardedAd",
    returnType = "V",
    parameters = emptyList(),
    custom = { method, _ -> method.implementation != null }
)

/**
 * Unbound 1-arg isRewardedAdReady(String) -> boolean.
 */
object IsRewardedAdReadyFingerprint : Fingerprint(
    name = "isRewardedAdReady",
    returnType = "Z",
    parameters = listOf("Ljava/lang/String;"),
    custom = { method, _ -> method.implementation != null }
)

/**
 * Unbound 0-arg isRewardedAdReady() -> boolean.
 */
object IsRewardedAdReady0ArgFingerprint : Fingerprint(
    name = "isRewardedAdReady",
    returnType = "Z",
    parameters = emptyList(),
    custom = { method, _ -> method.implementation != null }
)

// ============================================================================
// 3. AppLovin MAX Unity Plugin (Explicit Class Bindings)
// ============================================================================

object MaxUnityPluginIsRewardedAdReadyFingerprint : Fingerprint(
    definingClass = "Lcom/applovin/mediation/unity/MaxUnityPlugin;",
    name = "isRewardedAdReady",
    returnType = "Z",
    custom = { method, _ -> method.implementation != null }
)

object MaxUnityPluginLoadRewardedAdFingerprint : Fingerprint(
    definingClass = "Lcom/applovin/mediation/unity/MaxUnityPlugin;",
    name = "loadRewardedAd",
    custom = { method, _ -> method.implementation != null }
)

object MaxUnityPluginShowRewardedAdFingerprint : Fingerprint(
    definingClass = "Lcom/applovin/mediation/unity/MaxUnityPlugin;",
    name = "showRewardedAd",
    custom = { method, _ -> method.implementation != null }
)

object MaxUnityAdManagerIsRewardedAdReadyFingerprint : Fingerprint(
    definingClass = "Lcom/applovin/mediation/unity/MaxUnityAdManager;",
    name = "isRewardedAdReady",
    returnType = "Z",
    custom = { method, _ -> method.implementation != null }
)

object MaxUnityAdManagerLoadRewardedAdFingerprint : Fingerprint(
    definingClass = "Lcom/applovin/mediation/unity/MaxUnityAdManager;",
    name = "loadRewardedAd",
    custom = { method, _ -> method.implementation != null }
)

object MaxUnityAdManagerShowRewardedAdFingerprint : Fingerprint(
    definingClass = "Lcom/applovin/mediation/unity/MaxUnityAdManager;",
    name = "showRewardedAd",
    custom = { method, _ -> method.implementation != null }
)

// ============================================================================
// 4. AppLovin MAX Native SDK
// ============================================================================

object MaxRewardedAdSetListenerFingerprint : Fingerprint(
    definingClass = "Lcom/applovin/mediation/ads/MaxRewardedAd;",
    name = "setListener",
    custom = { method, _ -> method.implementation != null }
)

object MaxRewardedAdLoadAdFingerprint : Fingerprint(
    definingClass = "Lcom/applovin/mediation/ads/MaxRewardedAd;",
    name = "loadAd",
    custom = { method, _ -> method.implementation != null }
)

object MaxRewardedAdIsReadyFingerprint : Fingerprint(
    definingClass = "Lcom/applovin/mediation/ads/MaxRewardedAd;",
    name = "isReady",
    returnType = "Z",
    custom = { method, _ -> method.implementation != null }
)

object MaxRewardedAdImplIsReadyFingerprint : Fingerprint(
    definingClass = "Lcom/applovin/impl/mediation/ads/MaxRewardedAdImpl;",
    name = "isReady",
    returnType = "Z",
    custom = { method, _ -> method.implementation != null }
)

object MaxInterstitialAdIsReadyFingerprint : Fingerprint(
    definingClass = "Lcom/applovin/mediation/ads/MaxInterstitialAd;",
    name = "isReady",
    returnType = "Z",
    custom = { method, _ -> method.implementation != null }
)

object MaxAppOpenAdIsReadyFingerprint : Fingerprint(
    definingClass = "Lcom/applovin/mediation/ads/MaxAppOpenAd;",
    name = "isReady",
    returnType = "Z",
    custom = { method, _ -> method.implementation != null }
)

object MaxRewardedAdShowFingerprint : Fingerprint(
    definingClass = "Lcom/applovin/mediation/ads/MaxRewardedAd;",
    name = "showAd",
    custom = { method, _ -> method.implementation != null }
)

// ============================================================================
// 5. Unity Ads (Readiness & Show)
// ============================================================================

object UnityAdsAdvertisementIsReadyFingerprint : Fingerprint(
    definingClass = "Lcom/unity3d/ads/Advertisement;",
    name = "isReady",
    returnType = "Z",
    parameters = emptyList(),
    custom = { method, _ -> method.implementation != null }
)

object UnityAdsAdvertisementIsReadyPlacementFingerprint : Fingerprint(
    definingClass = "Lcom/unity3d/ads/Advertisement;",
    name = "isReady",
    returnType = "Z",
    parameters = listOf("Ljava/lang/String;"),
    custom = { method, _ -> method.implementation != null }
)

object UnityAdsSdkIsReadyFingerprint : Fingerprint(
    definingClass = "Lcom/unity3d/ads/UnityAds;",
    name = "isReady",
    returnType = "Z",
    parameters = emptyList(),
    custom = { method, _ -> method.implementation != null }
)

object UnityAdsLoadFingerprint : Fingerprint(
    definingClass = "Lcom/unity3d/ads/UnityAds;",
    name = "load",
    custom = { method, _ -> method.implementation != null }
)

object UnityAdsShowFingerprint : Fingerprint(
    definingClass = "Lcom/unity3d/ads/UnityAds;",
    name = "show",
    custom = { method, _ -> method.implementation != null }
)

object UnityRewardedAdShowFingerprint : Fingerprint(
    definingClass = "Lcom/unity3d/ads/RewardedAd;",
    name = "show",
    custom = { method, _ -> method.implementation != null }
)

object UnityAdsV4Show3ArgFingerprint : Fingerprint(
    definingClass = "Lcom/unity3d/ads/UnityAds;",
    name = "show",
    parameters = listOf(
        "Landroid/app/Activity;",
        "Ljava/lang/String;",
        "Lcom/unity3d/ads/IUnityAdsShowListener;"
    ),
    custom = { method, _ -> method.implementation != null }
)

object UnityAdsV4Show4ArgFingerprint : Fingerprint(
    definingClass = "Lcom/unity3d/ads/UnityAds;",
    name = "show",
    parameters = listOf(
        "Landroid/app/Activity;",
        "Ljava/lang/String;",
        "Lcom/unity3d/ads/UnityAdsShowOptions;",
        "Lcom/unity3d/ads/IUnityAdsShowListener;"
    ),
    custom = { method, _ -> method.implementation != null }
)

// ============================================================================
// 6. IronSource & LevelPlay
// ============================================================================

object IronSourceIsRewardedVideoAvailableFingerprint : Fingerprint(
    definingClass = "Lcom/ironsource/mediationsdk/IronSource;",
    name = "isRewardedVideoAvailable",
    returnType = "Z",
    parameters = emptyList(),
    custom = { method, _ -> method.implementation != null }
)

object IronSourceIsInterstitialReadyFingerprint : Fingerprint(
    definingClass = "Lcom/ironsource/mediationsdk/IronSource;",
    name = "isInterstitialReady",
    returnType = "Z",
    parameters = emptyList(),
    custom = { method, _ -> method.implementation != null }
)

object LevelPlayRewardedAdIsReadyFingerprint : Fingerprint(
    definingClass = "Lcom/unity3d/mediation/rewarded/LevelPlayRewardedAd;",
    name = "isAdReady",
    returnType = "Z",
    parameters = emptyList(),
    custom = { method, _ -> method.implementation != null }
)

object IronSourceUnityRewardedAdIsReadyFingerprint : Fingerprint(
    definingClass = "Lcom/ironsource/unity/androidbridge/RewardedAd;",
    name = "isAdReady",
    returnType = "Z",
    parameters = emptyList(),
    custom = { method, _ -> method.implementation != null }
)

object IronSourceAdsRewardedIsReadyPreciseFingerprint : Fingerprint(
    definingClass = "Lcom/unity3d/ironsourceads/rewarded/RewardedAd;",
    name = "isReadyToShow",
    returnType = "Z",
    parameters = emptyList(),
    custom = { method, _ -> method.implementation != null }
)

object IronSourceSetListenerFingerprint : Fingerprint(
    definingClass = "Lcom/ironsource/mediationsdk/IronSource;",
    name = "setRewardedVideoListener",
    custom = { method, _ -> method.implementation != null }
)

object IronSourceShowRewardedVideoFingerprint : Fingerprint(
    definingClass = "Lcom/ironsource/mediationsdk/IronSource;",
    name = "showRewardedVideo",
    custom = { method, _ -> method.implementation != null }
)

object IronSourceAdsRewardedShowPreciseFingerprint : Fingerprint(
    definingClass = "Lcom/unity3d/ironsourceads/rewarded/RewardedAd;",
    name = "show",
    parameters = listOf("Landroid/app/Activity;"),
    custom = { method, _ -> method.implementation != null }
)

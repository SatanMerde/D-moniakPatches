package com.dmoniak.patches.hungryshark

import app.morphe.patcher.Fingerprint

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

/**
 * Fingerprint for Unity Ads show(Activity, String, ...).
 */
object UnityAdsShowFingerprint : Fingerprint(
    definingClass = "Lcom/unity3d/ads/UnityAds;",
    name = "show",
    custom = { method, _ -> method.implementation != null }
)

/**
 * Fingerprint for Unity Ads load(String, IUnityAdsLoadListener).
 */
object UnityAdsLoadFingerprint : Fingerprint(
    definingClass = "Lcom/unity3d/ads/UnityAds;",
    name = "load",
    custom = { method, _ -> method.implementation != null }
)

/**
 * Fingerprint for Unity Ads isReady().
 */
object UnityAdsIsReadyFingerprint : Fingerprint(
    definingClass = "Lcom/unity3d/ads/UnityAds;",
    name = "isReady",
    parameters = emptyList(),
    returnType = "Z",
    custom = { method, _ -> method.implementation != null }
)

/**
 * Fingerprint for Unity Ads isReady(String placementId).
 */
object UnityAdsIsReadyPlacementFingerprint : Fingerprint(
    definingClass = "Lcom/unity3d/ads/UnityAds;",
    name = "isReady",
    parameters = listOf("Ljava/lang/String;"),
    returnType = "Z",
    custom = { method, _ -> method.implementation != null }
)

/**
 * Fingerprint for AppLovin MAX setListener(MaxRewardedAdListener) on MaxRewardedAd.
 */
object MaxRewardedAdSetListenerFingerprint : Fingerprint(
    definingClass = "Lcom/applovin/mediation/ads/MaxRewardedAd;",
    name = "setListener",
    custom = { method, _ -> method.implementation != null }
)

/**
 * Fingerprint for AppLovin MAX loadAd() on MaxRewardedAd.
 */
object MaxRewardedAdLoadAdFingerprint : Fingerprint(
    definingClass = "Lcom/applovin/mediation/ads/MaxRewardedAd;",
    name = "loadAd",
    custom = { method, _ -> method.implementation != null }
)

/**
 * Fingerprint for AppLovin MAX isReady() on MaxRewardedAd.
 */
object MaxRewardedAdIsReadyFingerprint : Fingerprint(
    definingClass = "Lcom/applovin/mediation/ads/MaxRewardedAd;",
    name = "isReady",
    returnType = "Z",
    custom = { method, _ -> method.implementation != null }
)

/**
 * Fingerprint for AppLovin MAX showAd(...) on native MaxRewardedAd.
 */
object MaxRewardedAdShowFingerprint : Fingerprint(
    definingClass = "Lcom/applovin/mediation/ads/MaxRewardedAd;",
    name = "showAd",
    custom = { method, _ -> method.implementation != null }
)

/**
 * Fingerprint for IronSource setRewardedVideoListener().
 */
object IronSourceSetListenerFingerprint : Fingerprint(
    definingClass = "Lcom/ironsource/mediationsdk/IronSource;",
    name = "setRewardedVideoListener",
    custom = { method, _ -> method.implementation != null }
)

/**
 * Fingerprint for IronSource showRewardedVideo().
 */
object IronSourceShowRewardedVideoFingerprint : Fingerprint(
    definingClass = "Lcom/ironsource/mediationsdk/IronSource;",
    name = "showRewardedVideo",
    custom = { method, _ -> method.implementation != null }
)

/**
 * Fingerprint for IronSource isRewardedVideoAvailable().
 */
object IronSourceIsAvailableFingerprint : Fingerprint(
    definingClass = "Lcom/ironsource/mediationsdk/IronSource;",
    name = "isRewardedVideoAvailable",
    returnType = "Z",
    custom = { method, _ -> method.implementation != null }
)

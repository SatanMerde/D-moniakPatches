package com.dmoniak.patches.hungryshark

import app.morphe.patcher.Fingerprint

/**
 * Fingerprint for Google Mobile Ads RewardedAd.show(Activity, OnUserEarnedRewardListener).
 * Strictly requires a non-null MethodImplementation to avoid abstract declarations.
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
 * Fingerprint for Unity Ads show(Activity, String, IUnityAdsShowListener).
 */
object UnityAdsShowFingerprint : Fingerprint(
    definingClass = "Lcom/unity3d/ads/UnityAds;",
    name = "show",
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
 * Fingerprint for AppLovin MAX showAd() on native MaxRewardedAd.
 */
object MaxRewardedAdShowFingerprint : Fingerprint(
    definingClass = "Lcom/applovin/mediation/ads/MaxRewardedAd;",
    name = "showAd",
    custom = { method, _ -> method.implementation != null }
)

/**
 * Fingerprint for AppLovin MAX isReady() on native MaxRewardedAd.
 */
object MaxRewardedAdIsReadyFingerprint : Fingerprint(
    definingClass = "Lcom/applovin/mediation/ads/MaxRewardedAd;",
    name = "isReady",
    returnType = "Z",
    custom = { method, _ -> method.implementation != null }
)

/**
 * Fingerprint for AppLovin MAX AppLovinSdk.isInitialized().
 */
object MaxSdkIsInitializedFingerprint : Fingerprint(
    definingClass = "Lcom/applovin/sdk/AppLovinSdk;",
    name = "isInitialized",
    parameters = emptyList(),
    returnType = "Z",
    custom = { method, _ -> method.implementation != null }
)

/**
 * Fingerprint for AppLovin MAX Unity Plugin MaxUnityPlugin.isInitialized().
 */
object MaxUnityPluginIsInitializedFingerprint : Fingerprint(
    definingClass = "Lcom/applovin/mediation/unity/MaxUnityPlugin;",
    name = "isInitialized",
    parameters = emptyList(),
    returnType = "Z",
    custom = { method, _ -> method.implementation != null }
)

/**
 * Fingerprint for AppLovin MAX Unity Plugin MaxUnityPlugin.isRewardedAdReady(String).
 */
object MaxUnityPluginIsRewardedAdReadyFingerprint : Fingerprint(
    definingClass = "Lcom/applovin/mediation/unity/MaxUnityPlugin;",
    name = "isRewardedAdReady",
    returnType = "Z",
    custom = { method, _ -> method.implementation != null }
)

/**
 * Fingerprint for AppLovin MAX Unity Plugin MaxUnityPlugin.showRewardedAd(String, String, String).
 */
object MaxUnityPluginShowRewardedAdFingerprint : Fingerprint(
    definingClass = "Lcom/applovin/mediation/unity/MaxUnityPlugin;",
    name = "showRewardedAd",
    custom = { method, _ -> method.implementation != null }
)

/**
 * Fingerprint for AppLovin MAX Unity Plugin MaxUnityPlugin.loadRewardedAd(String).
 */
object MaxUnityPluginLoadRewardedAdFingerprint : Fingerprint(
    definingClass = "Lcom/applovin/mediation/unity/MaxUnityPlugin;",
    name = "loadRewardedAd",
    custom = { method, _ -> method.implementation != null }
)

/**
 * Fingerprint for AppLovin MAX Unity Ad Manager MaxUnityAdManager.isRewardedAdReady(String).
 */
object MaxUnityAdManagerIsRewardedAdReadyFingerprint : Fingerprint(
    definingClass = "Lcom/applovin/mediation/unity/MaxUnityAdManager;",
    name = "isRewardedAdReady",
    returnType = "Z",
    custom = { method, _ -> method.implementation != null }
)

/**
 * Fingerprint for AppLovin MAX Unity Ad Manager MaxUnityAdManager.showRewardedAd(String, String, String).
 */
object MaxUnityAdManagerShowRewardedAdFingerprint : Fingerprint(
    definingClass = "Lcom/applovin/mediation/unity/MaxUnityAdManager;",
    name = "showRewardedAd",
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

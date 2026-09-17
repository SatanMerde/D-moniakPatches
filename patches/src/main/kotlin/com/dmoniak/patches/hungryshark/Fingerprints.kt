package com.dmoniak.patches.hungryshark

import app.morphe.patcher.Fingerprint

/**
 * Fingerprint for Google Mobile Ads RewardedAd.show(Activity, OnUserEarnedRewardListener).
 */
object GoogleRewardedAdShowFingerprint : Fingerprint(
    definingClass = "/RewardedAd;",
    name = "show",
    parameters = listOf("Landroid/app/Activity;", "Lcom/google/android/gms/ads/OnUserEarnedRewardListener;")
)

/**
 * Fingerprint for Google Mobile Ads Unity plugin UnityRewardedAd.show().
 */
object GoogleUnityRewardedAdShowFingerprint : Fingerprint(
    definingClass = "Lcom/google/unity/ads/UnityRewardedAd;",
    name = "show",
    parameters = emptyList()
)

/**
 * Fingerprint for Google Mobile Ads Unity plugin canShowAd() readiness check.
 */
object GoogleUnityRewardedAdCanShowFingerprint : Fingerprint(
    definingClass = "Lcom/google/unity/ads/UnityRewardedAd;",
    returnType = "Z",
    name = "canShowAd"
)

/**
 * Fingerprint for Unity Ads show(Activity, String, IUnityAdsShowListener).
 */
object UnityAdsShowFingerprint : Fingerprint(
    definingClass = "Lcom/unity3d/ads/UnityAds;",
    name = "show"
)

/**
 * Fingerprint for Unity Ads isReady().
 */
object UnityAdsIsReadyFingerprint : Fingerprint(
    definingClass = "Lcom/unity3d/ads/UnityAds;",
    name = "isReady",
    returnType = "Z"
)

/**
 * Fingerprint for AppLovin MAX showAd() on MaxRewardedAd.
 */
object MaxRewardedAdShowFingerprint : Fingerprint(
    definingClass = "Lcom/applovin/mediation/ads/MaxRewardedAd;",
    name = "showAd"
)

/**
 * Fingerprint for AppLovin MAX isReady() on MaxRewardedAd.
 */
object MaxRewardedAdIsReadyFingerprint : Fingerprint(
    definingClass = "Lcom/applovin/mediation/ads/MaxRewardedAd;",
    name = "isReady",
    returnType = "Z"
)

/**
 * Fingerprint for IronSource showRewardedVideo().
 */
object IronSourceShowRewardedVideoFingerprint : Fingerprint(
    definingClass = "Lcom/ironsource/mediationsdk/IronSource;",
    name = "showRewardedVideo"
)

/**
 * Fingerprint for IronSource isRewardedVideoAvailable().
 */
object IronSourceIsAvailableFingerprint : Fingerprint(
    definingClass = "Lcom/ironsource/mediationsdk/IronSource;",
    name = "isRewardedVideoAvailable",
    returnType = "Z"
)

package com.dmoniak.patches.hungryshark

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import com.android.tools.smali.dexlib2.AccessFlags
import com.dmoniak.patches.shared.Constants.COMPATIBILITY_HUNGRY_SHARK_WORLD

private const val EXTENSION_CLASS = "Lcom/dmoniak/patches/extension/HungrySharkAdsRewardHelper;"

private fun MutableMethod.safeAddInstructions(index: Int, smali: String) {
    if (this.implementation != null) {
        this.addInstructions(index, smali.trimIndent())
    }
}

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

@Suppress("unused")
val bypassRewardedAdsPatch = bytecodePatch(
    name = "Bypass Rewarded Ads",
    description = "Allows claiming all ad rewards (free revives, coin/gem multipliers, shop chests, spins) immediately without watching ads in Hungry Shark World.",
    default = true
) {
    compatibleWith(COMPATIBILITY_HUNGRY_SHARK_WORLD)

    extendWith("extensions/extension.mpe")

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

        fun hookLoadRewardedAd(method: MutableMethod?) {
            method?.let { m ->
                patchOnce(m, "loadRewardedAd") { target ->
                    val isStatic = AccessFlags.STATIC.isSet(target.accessFlags)
                    val paramCount = target.parameterTypes.size
                    if (isStatic) {
                        if (paramCount >= 1) {
                            target.safeAddInstructions(
                                0,
                                """
                                    invoke-static { p0 }, $EXTENSION_CLASS->bypassMaxUnityPluginLoad(Ljava/lang/Object;)V
                                    return-void
                                """
                            )
                        } else {
                            target.safeAddInstructions(
                                0,
                                """
                                    const-string v0, "rewardedAd"
                                    invoke-static { v0 }, $EXTENSION_CLASS->bypassMaxUnityPluginLoad(Ljava/lang/Object;)V
                                    return-void
                                """
                            )
                        }
                    } else {
                        // Instance method: p0 is this
                        if (paramCount >= 1) {
                            target.safeAddInstructions(
                                0,
                                """
                                    invoke-static { p0 }, $EXTENSION_CLASS->registerMaxUnityAdManager(Ljava/lang/Object;)V
                                    invoke-static { p1 }, $EXTENSION_CLASS->bypassMaxUnityPluginLoad(Ljava/lang/Object;)V
                                    return-void
                                """
                            )
                        } else {
                            target.safeAddInstructions(
                                0,
                                """
                                    invoke-static { p0 }, $EXTENSION_CLASS->registerMaxUnityAdManager(Ljava/lang/Object;)V
                                    const-string v0, "rewardedAd"
                                    invoke-static { v0 }, $EXTENSION_CLASS->bypassMaxUnityPluginLoad(Ljava/lang/Object;)V
                                    return-void
                                """
                            )
                        }
                    }
                }
            }
        }

        fun hookShowRewardedAd(method: MutableMethod?) {
            method?.let { m ->
                patchOnce(m, "showRewardedAd") { target ->
                    val isStatic = AccessFlags.STATIC.isSet(target.accessFlags)
                    val paramCount = target.parameterTypes.size
                    if (isStatic) {
                        when (paramCount) {
                            0 -> {
                                target.safeAddInstructions(
                                    0,
                                    """
                                        const-string v0, "rewardedAd"
                                        invoke-static { v0 }, $EXTENSION_CLASS->bypassMaxUnityShow(Ljava/lang/Object;)V
                                        return-void
                                    """
                                )
                            }
                            1 -> {
                                target.safeAddInstructions(
                                    0,
                                    """
                                        invoke-static { p0 }, $EXTENSION_CLASS->bypassMaxUnityShow(Ljava/lang/Object;)V
                                        return-void
                                    """
                                )
                            }
                            2 -> {
                                target.safeAddInstructions(
                                    0,
                                    """
                                        invoke-static { p0, p1 }, $EXTENSION_CLASS->bypassMaxUnityShow(Ljava/lang/Object;Ljava/lang/Object;)V
                                        return-void
                                    """
                                )
                            }
                            else -> {
                                target.safeAddInstructions(
                                    0,
                                    """
                                        invoke-static { p0, p1, p2 }, $EXTENSION_CLASS->bypassMaxUnityShow(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V
                                        return-void
                                    """
                                )
                            }
                        }
                    } else {
                        // Instance method: p0 is this
                        when (paramCount) {
                            0 -> {
                                target.safeAddInstructions(
                                    0,
                                    """
                                        invoke-static { p0 }, $EXTENSION_CLASS->registerMaxUnityAdManager(Ljava/lang/Object;)V
                                        const-string v0, "rewardedAd"
                                        invoke-static { v0 }, $EXTENSION_CLASS->bypassMaxUnityShow(Ljava/lang/Object;)V
                                        return-void
                                    """
                                )
                            }
                            1 -> {
                                target.safeAddInstructions(
                                    0,
                                    """
                                        invoke-static { p0, p1 }, $EXTENSION_CLASS->bypassMaxUnityManagerShow(Ljava/lang/Object;Ljava/lang/Object;)V
                                        return-void
                                    """
                                )
                            }
                            2 -> {
                                target.safeAddInstructions(
                                    0,
                                    """
                                        invoke-static { p0, p1, p2 }, $EXTENSION_CLASS->bypassMaxUnityManagerShow(Ljava/lang/Object;Ljava/lang/Object;)V
                                        return-void
                                    """
                                )
                            }
                            else -> {
                                target.safeAddInstructions(
                                    0,
                                    """
                                        invoke-static { p0, p1, p2, p3 }, $EXTENSION_CLASS->bypassMaxUnityManagerShow(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V
                                        return-void
                                    """
                                )
                            }
                        }
                    }
                }
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
        // 2. Intercept loadRewardedAd calls (immediately emit loaded & return)
        // ====================================================================
        hookLoadRewardedAd(LoadRewardedAdFingerprint.matchOrNull()?.method)
        hookLoadRewardedAd(LoadRewardedAd0ArgFingerprint.matchOrNull()?.method)
        hookLoadRewardedAd(MaxUnityPluginLoadRewardedAdFingerprint.matchOrNull()?.method)
        hookLoadRewardedAd(MaxUnityAdManagerLoadRewardedAdFingerprint.matchOrNull()?.method)

        // Native MAX loadAd
        MaxRewardedAdLoadAdFingerprint.matchOrNull()?.let { match ->
            patchOnce(match.method, "maxLoadAd") { m ->
                m.safeAddInstructions(
                    0,
                    """
                        invoke-static {}, $EXTENSION_CLASS->onMaxRewardedAdLoad()V
                        return-void
                    """
                )
            }
        }

        // ====================================================================
        // 3. Intercept showRewardedAd calls (immediately emit rewards & return)
        // ====================================================================
        hookShowRewardedAd(ShowRewardedAd3ArgFingerprint.matchOrNull()?.method)
        hookShowRewardedAd(ShowRewardedAd2ArgFingerprint.matchOrNull()?.method)
        hookShowRewardedAd(ShowRewardedAd1ArgFingerprint.matchOrNull()?.method)
        hookShowRewardedAd(MaxUnityPluginShowRewardedAdFingerprint.matchOrNull()?.method)
        hookShowRewardedAd(MaxUnityAdManagerShowRewardedAdFingerprint.matchOrNull()?.method)

        // Native AppLovin MAX showAd
        MaxRewardedAdSetListenerFingerprint.matchOrNull()?.let { match ->
            patchOnce(match.method, "maxSetListener") { m ->
                m.safeAddInstructions(
                    0,
                    """
                        invoke-static { p1 }, $EXTENSION_CLASS->registerMaxRewardedListener(Ljava/lang/Object;)V
                    """
                )
            }
        }

        MaxRewardedAdShowFingerprint.matchOrNull()?.let { match ->
            patchOnce(match.method, "maxShowAd") { m ->
                m.safeAddInstructions(
                    0,
                    """
                        invoke-static { p0 }, $EXTENSION_CLASS->bypassMaxRewardedAd(Ljava/lang/Object;)V
                        return-void
                    """
                )
            }
        }

        // ====================================================================
        // 4. Unity Ads Interception
        // ====================================================================
        UnityAdsLoadFingerprint.matchOrNull()?.let { match ->
            patchOnce(match.method, "unityAdsLoad") { m ->
                m.safeAddInstructions(
                    0,
                    """
                        invoke-static { p0, p1 }, $EXTENSION_CLASS->bypassUnityAdsLoad(Ljava/lang/Object;Ljava/lang/Object;)V
                    """
                )
            }
        }

        UnityAdsShowFingerprint.matchOrNull()?.let { match ->
            patchOnce(match.method, "unityAdsShow") { m ->
                val paramCount = m.parameterTypes.size
                when (paramCount) {
                    2 -> {
                        m.safeAddInstructions(
                            0,
                            """
                                invoke-static { p0, p1 }, $EXTENSION_CLASS->bypassUnityAdsShow(Ljava/lang/Object;Ljava/lang/Object;)V
                                return-void
                            """
                        )
                    }
                    3 -> {
                        m.safeAddInstructions(
                            0,
                            """
                                invoke-static { p0, p1, p2 }, $EXTENSION_CLASS->bypassUnityAdsShow(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V
                                return-void
                            """
                        )
                    }
                    else -> {
                        m.safeAddInstructions(
                            0,
                            """
                                invoke-static { p0, p1, p2, p3 }, $EXTENSION_CLASS->bypassUnityAdsShow(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V
                                return-void
                            """
                        )
                    }
                }
            }
        }

        UnityRewardedAdShowFingerprint.matchOrNull()?.let { match ->
            patchOnce(match.method, "unityRewardedAdShow") { m ->
                m.safeAddInstructions(
                    0,
                    """
                        invoke-static { p0, p1, p2, p3 }, $EXTENSION_CLASS->bypassUnityAdsShow(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V
                        return-void
                    """
                )
            }
        }

        UnityAdsV4Show3ArgFingerprint.matchOrNull()?.let { match ->
            patchOnce(match.method, "unityAdsV4Show3") { m ->
                m.safeAddInstructions(
                    0,
                    """
                        move-object/from16 v1, p2
                        move-object/from16 v2, p1
                        invoke-interface { v1, v2 }, Lcom/unity3d/ads/IUnityAdsShowListener;->onUnityAdsShowStart(Ljava/lang/String;)V
                        sget-object v0, Lcom/unity3d/ads/UnityAds${'$'}UnityAdsShowCompletionState;->COMPLETED:Lcom/unity3d/ads/UnityAds${'$'}UnityAdsShowCompletionState;
                        invoke-interface { v1, v2, v0 }, Lcom/unity3d/ads/IUnityAdsShowListener;->onUnityAdsShowComplete(Ljava/lang/String;Lcom/unity3d/ads/UnityAds${'$'}UnityAdsShowCompletionState;)V
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
                        invoke-interface { v1, v2 }, Lcom/unity3d/ads/IUnityAdsShowListener;->onUnityAdsShowStart(Ljava/lang/String;)V
                        sget-object v0, Lcom/unity3d/ads/UnityAds${'$'}UnityAdsShowCompletionState;->COMPLETED:Lcom/unity3d/ads/UnityAds${'$'}UnityAdsShowCompletionState;
                        invoke-interface { v1, v2, v0 }, Lcom/unity3d/ads/IUnityAdsShowListener;->onUnityAdsShowComplete(Ljava/lang/String;Lcom/unity3d/ads/UnityAds${'$'}UnityAdsShowCompletionState;)V
                        return-void
                    """
                )
            }
        }

        // ====================================================================
        // 5. IronSource Interception
        // ====================================================================
        IronSourceSetListenerFingerprint.matchOrNull()?.let { match ->
            patchOnce(match.method, "ironSourceSetListener") { m ->
                m.safeAddInstructions(
                    0,
                    """
                        invoke-static { p0 }, $EXTENSION_CLASS->registerIronSourceListener(Ljava/lang/Object;)V
                    """
                )
            }
        }

        IronSourceShowRewardedVideoFingerprint.matchOrNull()?.let { match ->
            patchOnce(match.method, "ironSourceShowRewardedVideo") { m ->
                val params = m.parameterTypes
                if (params.isEmpty()) {
                    m.safeAddInstructions(
                        0,
                        """
                            const-string v0, "rewardedVideo"
                            invoke-static { v0 }, $EXTENSION_CLASS->bypassIronSourceReward(Ljava/lang/Object;)V
                            return-void
                        """
                    )
                } else {
                    m.safeAddInstructions(
                        0,
                        """
                            invoke-static { p0 }, $EXTENSION_CLASS->bypassIronSourceReward(Ljava/lang/Object;)V
                            return-void
                        """
                    )
                }
            }
        }

        IronSourceAdsRewardedShowPreciseFingerprint.matchOrNull()?.let { match ->
            patchOnce(match.method, "ironSourceAdsRewardedShowPrecise") { m ->
                m.safeAddInstructions(
                    0,
                    """
                        invoke-static { p0 }, $EXTENSION_CLASS->bypassIronSourceReward(Ljava/lang/Object;)V
                        return-void
                    """
                )
            }
        }

        // ====================================================================
        // 6. Google Mobile Ads (AdMob) Interception
        // ====================================================================
        GoogleRewardedAdShowFingerprint.matchOrNull()?.let { match ->
            patchOnce(match.method, "adMobRewardedShow") { m ->
                m.safeAddInstructions(
                    0,
                    """
                        invoke-static { p1, p2 }, $EXTENSION_CLASS->bypassGoogleRewardedAd(Ljava/lang/Object;Ljava/lang/Object;)V
                        return-void
                    """
                )
            }
        }

        GoogleUnityRewardedAdShowFingerprint.matchOrNull()?.let { match ->
            patchOnce(match.method, "googleUnityRewardedShow") { m ->
                m.safeAddInstructions(
                    0,
                    """
                        iget-object v0, p0, Lcom/google/unity/ads/UnityRewardedAd;->callback:Lcom/google/unity/ads/UnityRewardedAdCallback;
                        invoke-static { v0 }, $EXTENSION_CLASS->bypassUnityRewardedAdCallback(Ljava/lang/Object;)V
                        return-void
                    """
                )
            }
        }
    }
}

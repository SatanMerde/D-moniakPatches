package com.dmoniak.patches.hungryshark

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import com.dmoniak.patches.shared.Constants.COMPATIBILITY_HUNGRY_SHARK_WORLD

private const val EXTENSION_CLASS = "Lcom/dmoniak/patches/extension/HungrySharkAdsRewardHelper;"

private fun MutableMethod.safeAddInstructions(index: Int, smali: String) {
    if (this.implementation != null) {
        this.addInstructions(index, smali)
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
        // 1. Google Mobile Ads (AdMob)
        GoogleRewardedAdShowFingerprint.matchOrNull()?.let { match ->
            match.method.safeAddInstructions(
                0,
                """
                    invoke-static { p1, p2 }, $EXTENSION_CLASS->bypassGoogleRewardedAd(Ljava/lang/Object;Ljava/lang/Object;)V
                    return-void
                """
            )
        }

        // 2. Google Mobile Ads Unity Plugin
        GoogleUnityRewardedAdShowFingerprint.matchOrNull()?.let { match ->
            match.method.safeAddInstructions(
                0,
                """
                    iget-object v0, p0, Lcom/google/unity/ads/UnityRewardedAd;->callback:Lcom/google/unity/ads/UnityRewardedAdCallback;
                    invoke-static { v0 }, $EXTENSION_CLASS->bypassUnityRewardedAdCallback(Ljava/lang/Object;)V
                    return-void
                """
            )
        }

        GoogleUnityRewardedAdCanShowFingerprint.matchOrNull()?.let { match ->
            match.method.safeAddInstructions(
                0,
                """
                    const/4 v0, 0x1
                    return v0
                """
            )
        }

        // 3. Unity Ads
        UnityAdsIsReadyFingerprint.matchOrNull()?.let { match ->
            match.method.safeAddInstructions(
                0,
                """
                    const/4 v0, 0x1
                    return v0
                """
            )
        }

        UnityAdsIsReadyPlacementFingerprint.matchOrNull()?.let { match ->
            match.method.safeAddInstructions(
                0,
                """
                    const/4 v0, 0x1
                    return v0
                """
            )
        }

        UnityAdsShowFingerprint.matchOrNull()?.let { match ->
            val paramCount = match.method.parameterTypes.size
            if (paramCount >= 2) {
                match.method.safeAddInstructions(
                    0,
                    """
                        invoke-static { p1, p2 }, $EXTENSION_CLASS->bypassUnityAdsShow(Ljava/lang/Object;Ljava/lang/Object;)V
                        return-void
                    """
                )
            }
        }

        // 4. AppLovin MAX (Native Android SDK)
        MaxSdkIsInitializedFingerprint.matchOrNull()?.let { match ->
            match.method.safeAddInstructions(
                0,
                """
                    const/4 v0, 0x1
                    return v0
                """
            )
        }

        MaxRewardedAdIsReadyFingerprint.matchOrNull()?.let { match ->
            match.method.safeAddInstructions(
                0,
                """
                    const/4 v0, 0x1
                    return v0
                """
            )
        }

        MaxRewardedAdShowFingerprint.matchOrNull()?.let { match ->
            match.method.safeAddInstructions(
                0,
                """
                    invoke-static { p0 }, $EXTENSION_CLASS->bypassMaxRewardedAd(Ljava/lang/Object;)V
                    return-void
                """
            )
        }

        // 5. AppLovin MAX (Unity Plugin Bridge - MaxUnityPlugin & MaxUnityAdManager)
        MaxUnityPluginIsInitializedFingerprint.matchOrNull()?.let { match ->
            match.method.safeAddInstructions(
                0,
                """
                    const/4 v0, 0x1
                    return v0
                """
            )
        }

        MaxUnityPluginIsRewardedAdReadyFingerprint.matchOrNull()?.let { match ->
            match.method.safeAddInstructions(
                0,
                """
                    const/4 v0, 0x1
                    return v0
                """
            )
        }

        MaxUnityAdManagerIsRewardedAdReadyFingerprint.matchOrNull()?.let { match ->
            match.method.safeAddInstructions(
                0,
                """
                    const/4 v0, 0x1
                    return v0
                """
            )
        }

        MaxUnityPluginLoadRewardedAdFingerprint.matchOrNull()?.let { match ->
            match.method.safeAddInstructions(
                0,
                """
                    invoke-static { p0 }, $EXTENSION_CLASS->bypassMaxUnityPluginLoad(Ljava/lang/String;)V
                """
            )
        }

        MaxUnityPluginShowRewardedAdFingerprint.matchOrNull()?.let { match ->
            match.method.safeAddInstructions(
                0,
                """
                    invoke-static { p0, p1, p2 }, $EXTENSION_CLASS->bypassMaxUnityPluginShow(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V
                    return-void
                """
            )
        }

        MaxUnityAdManagerShowRewardedAdFingerprint.matchOrNull()?.let { match ->
            match.method.safeAddInstructions(
                0,
                """
                    invoke-static { p1, p2, p3 }, $EXTENSION_CLASS->bypassMaxUnityPluginShow(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V
                    return-void
                """
            )
        }

        // 6. IronSource
        IronSourceIsAvailableFingerprint.matchOrNull()?.let { match ->
            match.method.safeAddInstructions(
                0,
                """
                    const/4 v0, 0x1
                    return v0
                """
            )
        }

        IronSourceShowRewardedVideoFingerprint.matchOrNull()?.let { match ->
            match.method.safeAddInstructions(
                0,
                """
                    invoke-static {}, $EXTENSION_CLASS->isAdReady()Z
                """
            )
        }
    }
}

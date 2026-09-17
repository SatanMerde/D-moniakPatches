package com.dmoniak.patches.hungryshark

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import com.dmoniak.patches.shared.Constants.COMPATIBILITY_HUNGRY_SHARK_WORLD

private const val EXTENSION_CLASS = "Lcom/dmoniak/patches/extension/HungrySharkAdsRewardHelper;"

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
            match.method.addInstructions(
                0,
                """
                    invoke-static { p1, p2 }, $EXTENSION_CLASS->bypassGoogleRewardedAd(Ljava/lang/Object;Ljava/lang/Object;)V
                    return-void
                """
            )
        }

        // 2. Google Mobile Ads Unity Plugin
        GoogleUnityRewardedAdShowFingerprint.matchOrNull()?.let { match ->
            match.method.addInstructions(
                0,
                """
                    invoke-static {}, $EXTENSION_CLASS->isAdReady()Z
                """
            )
        }

        GoogleUnityRewardedAdCanShowFingerprint.matchOrNull()?.let { match ->
            match.method.addInstructions(
                0,
                """
                    const/4 v0, 0x1
                    return v0
                """
            )
        }

        // 3. Unity Ads
        UnityAdsIsReadyFingerprint.matchOrNull()?.let { match ->
            match.method.addInstructions(
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
                match.method.addInstructions(
                    0,
                    """
                        invoke-static { p1, p2 }, $EXTENSION_CLASS->bypassUnityAdsShow(Ljava/lang/Object;Ljava/lang/Object;)V
                        return-void
                    """
                )
            }
        }

        // 4. AppLovin MAX
        MaxRewardedAdIsReadyFingerprint.matchOrNull()?.let { match ->
            match.method.addInstructions(
                0,
                """
                    const/4 v0, 0x1
                    return v0
                """
            )
        }

        MaxRewardedAdShowFingerprint.matchOrNull()?.let { match ->
            match.method.addInstructions(
                0,
                """
                    invoke-static {}, $EXTENSION_CLASS->isAdReady()Z
                """
            )
        }

        // 5. IronSource
        IronSourceIsAvailableFingerprint.matchOrNull()?.let { match ->
            match.method.addInstructions(
                0,
                """
                    const/4 v0, 0x1
                    return v0
                """
            )
        }
    }
}

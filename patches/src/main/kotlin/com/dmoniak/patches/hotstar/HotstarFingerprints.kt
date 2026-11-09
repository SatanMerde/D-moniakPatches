package com.dmoniak.patches.hotstar

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.literal
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

// ============================================================================
// 1. Ads & SSAI (Server-Side Ad Insertion) Fingerprints
// ============================================================================

/**
 * AdMetadata constructor — controls pre-roll/mid-roll ad flags and SSAI tags.
 * Parameters: (disablePreRoll, disableMidRoll, hasPreRoll, hasPlayablePreRoll, enableMidRollLoad, ssaiTag, perPodPositions)
 */
object AdMetadataConstructorFingerprint : Fingerprint(
    returnType = "V",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR),
    parameters = listOf("Z", "Z", "Z", "Z", "Z", "Ljava/lang/String;", "Ljava/util/List;"),
    filters = listOf(
        string("ssaiTag"),
        string("perPodPositions")
    )
)

/**
 * WatchLiveAdsViewModel.b() — Initializes live ad sessions.
 */
object WatchLiveAdsInitFingerprint : Fingerprint(
    definingClass = "Lcom/hotstar/ads/watch/WatchLiveAdsViewModel;",
    name = "b",
    returnType = "V",
)

/**
 * Inner composable that renders display ad content (home feed banner/sponsored ads).
 */
object DisplayAdComposableFingerprint : Fingerprint(
    returnType = "V",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC, AccessFlags.FINAL),
    filters = listOf(
        string("DisplayAd"),
    )
)

/**
 * SGAI Prefetcher getAd — fetches live ads from server cache.
 */
object SgaiPrefetcherGetAdFingerprint : Fingerprint(
    returnType = "Ljava/lang/Object;",
    filters = listOf(
        string("SGAI_Prefetcher"),
        string("getAd: ")
    )
)

/**
 * LiveAdsManager main ad processing loop.
 */
object LiveAdsManagerProcessFingerprint : Fingerprint(
    returnType = "Ljava/lang/Object;",
    filters = listOf(
        string("LiveAdsManager"),
        string("Removed stale ad at %d")
    )
)

/**
 * CapabilitiesConfig.getEnableSsai() — Controls whether Server-Side Ad Insertion is enabled.
 */
object CapabilitiesConfigEnableSsaiFingerprint : Fingerprint(
    definingClass = "Lcom/hotstar/player/models/capabilities/CapabilitiesConfig;",
    name = "getEnableSsai",
    returnType = "Z"
)

/**
 * CapabilitiesConfig.getEnableSgai() — Controls whether Server-Guided Ad Insertion is enabled.
 */
object CapabilitiesConfigEnableSgaiFingerprint : Fingerprint(
    definingClass = "Lcom/hotstar/player/models/capabilities/CapabilitiesConfig;",
    name = "getEnableSgai",
    returnType = "Z"
)

/**
 * PlayerConfig.getEnableLiveCheckForSSAIStream() — Live check for SSAI stream insertion.
 */
object PlayerConfigLiveCheckSsaiFingerprint : Fingerprint(
    definingClass = "Lcom/hotstar/player/models/config/PlayerConfig;",
    name = "getEnableLiveCheckForSSAIStream",
    returnType = "Z"
)

/**
 * PlayerConfig.getEnableCohortSwitchingWithNoPcRetryForLiveSsai() — Cohort switching for live SSAI.
 */
object PlayerConfigCohortSwitchingLiveSsaiFingerprint : Fingerprint(
    definingClass = "Lcom/hotstar/player/models/config/PlayerConfig;",
    name = "getEnableCohortSwitchingWithNoPcRetryForLiveSsai",
    returnType = "Z"
)

/**
 * PlayerWidget$LiveStreamAdData.getSsaiTag() — Returns live match SSAI tag string.
 */
object LiveStreamAdDataSsaiTagFingerprint : Fingerprint(
    definingClass = "Lcom/hotstar/ui/model/widget/PlayerWidget\$LiveStreamAdData;",
    name = "getSsaiTag",
    returnType = "Ljava/lang/String;"
)

/**
 * Midroll.getSsaiTag() — Returns VOD mid-roll SSAI tag string.
 */
object MidrollSsaiTagFingerprint : Fingerprint(
    definingClass = "Lcom/hotstar/ui/model/feature/videoads/Midroll;",
    name = "getSsaiTag",
    returnType = "Ljava/lang/String;"
)

/**
 * UnifiedCacheMetadata.getSsaiTag() — Cache metadata SSAI tag.
 */
object UnifiedCacheMetadataSsaiTagFingerprint : Fingerprint(
    definingClass = "Lcom/hotstar/bff/api/v2/enrichment/cachemetadata/UnifiedCacheMetadata;",
    name = "getSsaiTag",
    returnType = "Ljava/lang/String;"
)

// ============================================================================
// 2. Screenshots & Screen Recording Protection Fingerprints
// ============================================================================

/**
 * ABConfig.getUseSecureView() — Controls whether SurfaceView.setSecure(true) is called.
 */
object UseSecureViewFingerprint : Fingerprint(
    definingClass = "Lcom/hotstar/player/models/config/ABConfig;",
    name = "getUseSecureView",
    returnType = "Z"
)

/**
 * Compose FLAG_SECURE — dynamically adds FLAG_SECURE (0x2000) to Activity window in Compose UI.
 */
object ComposeFlagSecureFingerprint : Fingerprint(
    returnType = "V",
    filters = listOf(
        string("null cannot be cast to non-null type android.app.Activity"),
        methodCall(definingClass = "Landroid/app/Activity;", name = "getWindow"),
        literal(0x2000),
        methodCall(definingClass = "Landroid/view/Window;", name = "addFlags")
    )
)

// ============================================================================
// 3. Signature & Anti-Tamper Verification Fingerprints
// ============================================================================

/**
 * Computes MD5 of app signing certificate, compares with JNI value,
 * and sends encrypted tamper status header to backend API.
 */
object SignatureCheckFingerprint : Fingerprint(
    returnType = "Ljava/lang/String;",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC, AccessFlags.FINAL),
    filters = listOf(
        string("MD5"),
        methodCall(definingClass = "Landroid/util/Base64;", name = "encodeToString"),
        methodCall(definingClass = "Lcom/hotstar/secrets/Secrets;", name = "getSignFromJNI")
    )
)

// ============================================================================
// 4. Premium Entitlements & Subscription Nudges Fingerprints
// ============================================================================

/**
 * MyPageData.getIsSubscribed() — Protobuf-generated class for subscribed UI state.
 */
object IsSubscribedFingerprint : Fingerprint(
    definingClass = "Lcom/hotstar/ui/model/pagedata/MyPageData;",
    name = "getIsSubscribed",
    returnType = "Z"
)

/**
 * DownloadInfo.getIsPremium() — Protobuf-generated class for content download restriction.
 */
object IsPremiumFingerprint : Fingerprint(
    definingClass = "Lcom/hotstar/ui/model/feature/download/DownloadInfo;",
    name = "getIsPremium",
    returnType = "Z"
)

/**
 * Subscription nudge handler — suppresses paywall prompts during playback.
 */
object SubscriptionNudgeFingerprint : Fingerprint(
    returnType = "V",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    filters = listOf(
        string("interventionWidget"),
        string("null cannot be cast to non-null type com.hotstar.bff.models.widget.BffWidgetVisibilityIntervention"),
        string("SubscriptionNudgeWidget")
    )
)

// ============================================================================
// 5. Screen Mirroring & HDMI Multi-Display Fingerprints
// ============================================================================

/**
 * Checks DisplayManager.getDisplays().length > 1 and blocks playback over HDMI or wireless display.
 */
object MultipleDisplayCheckFingerprint : Fingerprint(
    returnType = "V",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC, AccessFlags.FINAL),
    filters = listOf(
        methodCall(definingClass = "Landroid/hardware/display/DisplayManager;", name = "getDisplays"),
        opcode(Opcode.ARRAY_LENGTH),
        literal(1)
    )
)

/**
 * PlayerConfig.getEnableHdmiBroadcastListener() — Listener that shuts off playback when HDMI is connected.
 */
object PlayerConfigHdmiBroadcastFingerprint : Fingerprint(
    definingClass = "Lcom/hotstar/player/models/config/PlayerConfig;",
    name = "getEnableHdmiBroadcastListener",
    returnType = "Z"
)

// ============================================================================
// 6. Playback Codecs, HDR10, Dolby Vision & 4K Capabilities Fingerprints
// ============================================================================

/**
 * 4K resolution support check — bypasses 4K blacklist.
 */
object Is4kSupportedFingerprint : Fingerprint(
    returnType = "Z",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    parameters = listOf("Landroid/content/Context;"),
    filters = listOf(
        string("4k"),
    )
)

/**
 * Video codec support check (H265, VP9, AV1).
 */
object IsCodecSupportedFingerprint : Fingerprint(
    returnType = "Z",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    filters = listOf(
        string("supportsCodec Picked from cache")
    )
)

/**
 * Dolby Vision / HDR10 support check — bypasses device capability blacklist.
 */
object IsDolbyVisionSupportedFingerprint : Fingerprint(
    returnType = "Z",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    parameters = listOf("Landroid/content/Context;", "Z", "I", "Z"),
    filters = listOf(
        string("dv"),
        string("bklst"),
    )
)

/**
 * CapabilitiesConfig.getEnableAv1Support() — Enables AV1 video decoding.
 */
object CapabilitiesConfigEnableAv1Fingerprint : Fingerprint(
    definingClass = "Lcom/hotstar/player/models/capabilities/CapabilitiesConfig;",
    name = "getEnableAv1Support",
    returnType = "Z"
)

/**
 * CapabilitiesConfig.getEnableGoogleDav1d() — Enables DAV1D AV1 software decoder fallback.
 */
object CapabilitiesConfigEnableGoogleDav1dFingerprint : Fingerprint(
    definingClass = "Lcom/hotstar/player/models/capabilities/CapabilitiesConfig;",
    name = "getEnableGoogleDav1d",
    returnType = "Z"
)

/**
 * CapabilitiesConfig.getEnableDolby51ForMobile() — Enables Dolby 5.1 surround sound on mobile.
 */
object CapabilitiesConfigEnableDolby51Fingerprint : Fingerprint(
    definingClass = "Lcom/hotstar/player/models/capabilities/CapabilitiesConfig;",
    name = "getEnableDolby51ForMobile",
    returnType = "Z"
)

/**
 * CapabilitiesConfig.getEnableEC3Dolby51ForMobileLive() — Enables E-AC3 Dolby 5.1 for live streams.
 */
object CapabilitiesConfigEnableEC3Dolby51Fingerprint : Fingerprint(
    definingClass = "Lcom/hotstar/player/models/capabilities/CapabilitiesConfig;",
    name = "getEnableEC3Dolby51ForMobileLive",
    returnType = "Z"
)

/**
 * CapabilitiesConfig.getForceDetectAllAudioCapabilities() — Forces full audio capability discovery.
 */
object CapabilitiesConfigForceDetectAudioFingerprint : Fingerprint(
    definingClass = "Lcom/hotstar/player/models/capabilities/CapabilitiesConfig;",
    name = "getForceDetectAllAudioCapabilities",
    returnType = "Z"
)

/**
 * CapabilitiesConfig.getEnableNonSecureVideoCodecs() — Allows non-secure video decoders (prevents DRM level downgrade lock).
 */
object CapabilitiesConfigEnableNonSecureVideoCodecsFingerprint : Fingerprint(
    definingClass = "Lcom/hotstar/player/models/capabilities/CapabilitiesConfig;",
    name = "getEnableNonSecureVideoCodecs",
    returnType = "Z"
)

/**
 * CapabilitiesConfig.getEnableHevcCheckFor4KResolution() — Enables HEVC 4K resolution check.
 */
object CapabilitiesConfigEnableHevcCheck4kFingerprint : Fingerprint(
    definingClass = "Lcom/hotstar/player/models/capabilities/CapabilitiesConfig;",
    name = "getEnableHevcCheckFor4KResolution",
    returnType = "Z"
)

/**
 * CapabilitiesConfig.getBlackListConfig() — Device blacklist for resolution, codecs, and HDR.
 */
object CapabilitiesConfigBlackListConfigFingerprint : Fingerprint(
    definingClass = "Lcom/hotstar/player/models/capabilities/CapabilitiesConfig;",
    name = "getBlackListConfig",
    returnType = "Lcom/hotstar/player/models/config/BlackListConfig;"
)

/**
 * CapabilitiesConfig.isFhdLayerOutputProtected() — FHD layer HDCP protection flag.
 */
object CapabilitiesConfigIsFhdLayerProtectedFingerprint : Fingerprint(
    definingClass = "Lcom/hotstar/player/models/capabilities/CapabilitiesConfig;",
    name = "isFhdLayerOutputProtected",
    returnType = "Z"
)

/**
 * CapabilitiesConfig.getUseCodecCheckForDV() — Dolby Vision codec profile check.
 */
object CapabilitiesConfigUseCodecCheckDVFingerprint : Fingerprint(
    definingClass = "Lcom/hotstar/player/models/capabilities/CapabilitiesConfig;",
    name = "getUseCodecCheckForDV",
    returnType = "Z"
)

/**
 * CapabilitiesConfig.getUseCodecCheckForHdr10() — HDR10 codec profile check.
 */
object CapabilitiesConfigUseCodecCheckHdr10Fingerprint : Fingerprint(
    definingClass = "Lcom/hotstar/player/models/capabilities/CapabilitiesConfig;",
    name = "getUseCodecCheckForHdr10",
    returnType = "Z"
)

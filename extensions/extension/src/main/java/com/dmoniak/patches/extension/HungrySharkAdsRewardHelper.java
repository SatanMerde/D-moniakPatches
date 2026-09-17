package com.dmoniak.patches.extension;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Extension helper for Hungry Shark World Morphe Patch.
 * Intercepts rewarded ad requests and immediately triggers completion callbacks
 * to grant the user rewards (coins, gems, revives, bonus chests) without playing ads.
 */
public final class HungrySharkAdsRewardHelper {

    private static final String TAG = "D-moniakPatches";
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    private HungrySharkAdsRewardHelper() {}

    /**
     * Always returns true for ad availability checks so buttons are never disabled.
     */
    public static boolean isAdReady() {
        Log.i(TAG, "Ad availability check intercepted: returning ready = true");
        return true;
    }

    /**
     * Intercepts Google Mobile Ads (AdMob) RewardedAd.show(activity, listener).
     */
    public static void bypassGoogleRewardedAd(Object activityObj, Object listenerObj) {
        Log.i(TAG, "Bypassing Google Mobile Ads RewardedAd.show...");
        if (listenerObj == null) {
            Log.w(TAG, "OnUserEarnedRewardListener is null");
            return;
        }

        MAIN_HANDLER.post(() -> {
            try {
                // Find onUserEarnedReward(RewardItem)
                for (Method m : listenerObj.getClass().getMethods()) {
                    if ("onUserEarnedReward".equals(m.getName()) && m.getParameterTypes().length == 1) {
                        Class<?> paramType = m.getParameterTypes()[0];
                        Object rewardProxy = createDummyRewardItemProxy(paramType);
                        m.invoke(listenerObj, rewardProxy);
                        Log.i(TAG, "Successfully invoked onUserEarnedReward!");
                        return;
                    }
                }
                Log.w(TAG, "Could not find onUserEarnedReward method on listener");
            } catch (Throwable t) {
                Log.e(TAG, "Error invoking onUserEarnedReward", t);
            }
        });
    }

    /**
     * Intercepts Google Mobile Ads Unity plugin (UnityRewardedAd) show().
     * Handles UnityRewardedAdCallback directly.
     */
    public static void bypassUnityRewardedAdCallback(Object callbackObj) {
        Log.i(TAG, "Bypassing Google UnityRewardedAd callback...");
        if (callbackObj == null) return;

        MAIN_HANDLER.post(() -> {
            try {
                // 1. Trigger onUserEarnedReward(String, float)
                try {
                    Method onReward = callbackObj.getClass().getMethod("onUserEarnedReward", String.class, float.class);
                    onReward.invoke(callbackObj, "reward", 1.0f);
                    Log.i(TAG, "Invoked UnityRewardedAdCallback.onUserEarnedReward");
                } catch (NoSuchMethodException ignored) {
                    for (Method m : callbackObj.getClass().getMethods()) {
                        if ("onUserEarnedReward".equals(m.getName())) {
                            m.invoke(callbackObj, "reward", 1.0f);
                            break;
                        }
                    }
                }

                // 2. Trigger onAdDismissedFullScreenContent()
                try {
                    Method onDismiss = callbackObj.getClass().getMethod("onAdDismissedFullScreenContent");
                    onDismiss.invoke(callbackObj);
                    Log.i(TAG, "Invoked UnityRewardedAdCallback.onAdDismissedFullScreenContent");
                } catch (Throwable ignored) {}
            } catch (Throwable t) {
                Log.e(TAG, "Error bypassing UnityRewardedAdCallback", t);
            }
        });
    }

    /**
     * Intercepts Unity Ads UnityAds.show(..., showListener).
     */
    public static void bypassUnityAdsShow(Object placementIdObj, Object showListenerObj) {
        final String placementId = placementIdObj != null ? placementIdObj.toString() : "rewardedVideo";
        Log.i(TAG, "Bypassing Unity Ads show for placement: " + placementId);
        if (showListenerObj == null) return;

        MAIN_HANDLER.post(() -> {
            try {
                Class<?> listenerClass = showListenerObj.getClass();

                // Call onUnityAdsShowStart if available
                try {
                    for (Method m : listenerClass.getMethods()) {
                        if ("onUnityAdsShowStart".equals(m.getName()) && m.getParameterTypes().length == 1) {
                            m.invoke(showListenerObj, placementId);
                            break;
                        }
                    }
                } catch (Throwable ignored) {}

                // Call onUnityAdsShowComplete(placementId, COMPLETED)
                for (Method m : listenerClass.getMethods()) {
                    if ("onUnityAdsShowComplete".equals(m.getName()) && m.getParameterTypes().length == 2) {
                        Class<?> enumType = m.getParameterTypes()[1];
                        Object completedEnum = null;
                        if (enumType.isEnum()) {
                            for (Object constant : enumType.getEnumConstants()) {
                                if ("COMPLETED".equalsIgnoreCase(constant.toString())) {
                                    completedEnum = constant;
                                    break;
                                }
                            }
                        }
                        m.invoke(showListenerObj, placementId, completedEnum);
                        Log.i(TAG, "Invoked UnityAds showListener.onUnityAdsShowComplete with COMPLETED");
                        return;
                    }
                }
            } catch (Throwable t) {
                Log.e(TAG, "Error bypassing UnityAds showListener", t);
            }
        });
    }

    /**
     * Intercepts AppLovin MAX Rewarded ad callbacks.
     */
    public static void bypassAppLovinMaxReward(Object listenerObj, Object maxAdObj) {
        Log.i(TAG, "Bypassing AppLovin MAX rewarded ad...");
        if (listenerObj == null) return;

        MAIN_HANDLER.post(() -> {
            try {
                // onUserRewarded(MaxAd, MaxReward)
                for (Method m : listenerObj.getClass().getMethods()) {
                    if ("onUserRewarded".equals(m.getName()) && m.getParameterTypes().length == 2) {
                        Class<?> rewardClass = m.getParameterTypes()[1];
                        Object dummyReward = createDummyMaxRewardProxy(rewardClass);
                        m.invoke(listenerObj, maxAdObj, dummyReward);
                        Log.i(TAG, "Invoked AppLovin onUserRewarded!");
                    }
                }

                // onAdHidden(MaxAd)
                for (Method m : listenerObj.getClass().getMethods()) {
                    if ("onAdHidden".equals(m.getName()) && m.getParameterTypes().length == 1) {
                        m.invoke(listenerObj, maxAdObj);
                        Log.i(TAG, "Invoked AppLovin onAdHidden!");
                    }
                }
            } catch (Throwable t) {
                Log.e(TAG, "Error bypassing AppLovin MAX", t);
            }
        });
    }

    /**
     * Intercepts IronSource Rewarded Video callbacks.
     */
    public static void bypassIronSourceReward(Object listenerObj, Object placementObj) {
        Log.i(TAG, "Bypassing IronSource Rewarded Video...");
        if (listenerObj == null) return;

        MAIN_HANDLER.post(() -> {
            try {
                // onRewardedVideoAdRewarded(Placement)
                for (Method m : listenerObj.getClass().getMethods()) {
                    if ("onRewardedVideoAdRewarded".equals(m.getName())) {
                        m.invoke(listenerObj, placementObj);
                        Log.i(TAG, "Invoked IronSource onRewardedVideoAdRewarded!");
                    }
                }

                // onRewardedVideoAdClosed()
                for (Method m : listenerObj.getClass().getMethods()) {
                    if ("onRewardedVideoAdClosed".equals(m.getName()) && m.getParameterTypes().length == 0) {
                        m.invoke(listenerObj);
                        Log.i(TAG, "Invoked IronSource onRewardedVideoAdClosed!");
                    }
                }
            } catch (Throwable t) {
                Log.e(TAG, "Error bypassing IronSource", t);
            }
        });
    }

    /**
     * Generic fallback trigger to send reward message directly to Unity game objects if applicable.
     */
    public static void sendUnityRewardMessage(String gameObject, String methodName, String param) {
        try {
            Class<?> unityPlayerClass = Class.forName("com.unity3d.player.UnityPlayer");
            Method sendMessage = unityPlayerClass.getMethod("UnitySendMessage", String.class, String.class, String.class);
            sendMessage.invoke(null, gameObject, methodName, param != null ? param : "");
            Log.i(TAG, "Sent UnitySendMessage to " + gameObject + "->" + methodName);
        } catch (Throwable t) {
            Log.d(TAG, "UnitySendMessage not available or failed: " + t.getMessage());
        }
    }

    private static Object createDummyRewardItemProxy(Class<?> interfaceClass) {
        if (!interfaceClass.isInterface()) return null;
        return Proxy.newProxyInstance(
            interfaceClass.getClassLoader(),
            new Class<?>[]{interfaceClass},
            new InvocationHandler() {
                @Override
                public Object invoke(Object proxy, Method method, Object[] args) {
                    String name = method.getName();
                    if ("getAmount".equals(name)) return 1;
                    if ("getType".equals(name)) return "reward";
                    if ("toString".equals(name)) return "DummyRewardItem[amount=1, type=reward]";
                    return null;
                }
            }
        );
    }

    private static Object createDummyMaxRewardProxy(Class<?> interfaceClass) {
        if (!interfaceClass.isInterface()) return null;
        return Proxy.newProxyInstance(
            interfaceClass.getClassLoader(),
            new Class<?>[]{interfaceClass},
            new InvocationHandler() {
                @Override
                public Object invoke(Object proxy, Method method, Object[] args) {
                    String name = method.getName();
                    if ("getAmount".equals(name)) return 1;
                    if ("getLabel".equals(name)) return "reward";
                    return null;
                }
            }
        );
    }
}

package com.dmoniak.patches.extension;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashSet;
import java.util.Set;

/**
 * Extension helper for Hungry Shark World Morphe Patch.
 * 
 * Intercepts rewarded ads across AppLovin MAX, Unity Ads, IronSource, and Google AdMob
 * so that ads are always reported as ready/loaded, and rewards are granted immediately
 * upon show request without playing any video advertisements.
 * 
 * DISCLAIMER:
 * 100% AI Generated code for educational and research purposes only.
 * No liability accepted.
 */
public final class HungrySharkAdsRewardHelper {

    private static final String TAG = "D-moniakPatches";
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    private static volatile Object sSavedMaxListener = null;
    private static volatile Object sSavedIronSourceListener = null;

    private HungrySharkAdsRewardHelper() {}

    /**
     * Always returns true for ad availability / readiness checks.
     */
    public static boolean isAdReady() {
        Log.i(TAG, "Ad availability check intercepted: returning true");
        return true;
    }

    // =========================================================================
    // 1. AppLovin MAX Rewarded Ads
    // =========================================================================

    /**
     * Captured when MaxRewardedAd.setListener(...) is called by the game.
     */
    public static void registerMaxRewardedListener(Object listener) {
        Log.i(TAG, "registerMaxRewardedListener: " + listener);
        if (listener == null) return;
        sSavedMaxListener = listener;
        MAIN_HANDLER.post(() -> notifyMaxAdLoaded(listener));
    }

    /**
     * Triggered when MaxRewardedAd.loadAd() is called by the game.
     */
    public static void onMaxRewardedAdLoad() {
        Log.i(TAG, "onMaxRewardedAdLoad called");
        if (sSavedMaxListener != null) {
            MAIN_HANDLER.post(() -> notifyMaxAdLoaded(sSavedMaxListener));
        }
    }

    /**
     * Notifies MaxRewardedAdListener that the ad has loaded.
     */
    private static void notifyMaxAdLoaded(Object listenerObj) {
        if (listenerObj == null) return;
        try {
            for (Method m : listenerObj.getClass().getMethods()) {
                if ("onAdLoaded".equals(m.getName()) && m.getParameterTypes().length == 1) {
                    Class<?> paramType = m.getParameterTypes()[0];
                    Object dummyAd = createDummyMaxAdProxy(paramType);
                    m.invoke(listenerObj, dummyAd);
                    Log.i(TAG, "Successfully invoked onAdLoaded on MaxRewardedAdListener!");
                    break;
                }
            }
        } catch (Throwable t) {
            Log.e(TAG, "Error invoking onAdLoaded on MaxRewardedAdListener", t);
        }
    }

    /**
     * Intercepts AppLovin MaxRewardedAd.showAd(...) calls.
     */
    public static void bypassMaxRewardedAd(Object maxRewardedAdInstance) {
        Log.i(TAG, "Bypassing AppLovin MaxRewardedAd.showAd...");
        Object listener = sSavedMaxListener;
        if (listener == null && maxRewardedAdInstance != null) {
            listener = findListenerInObject(maxRewardedAdInstance, "MaxRewardedAdListener");
            if (listener == null) {
                listener = findListenerByMethod(maxRewardedAdInstance, "onUserRewarded");
            }
        }

        if (listener != null) {
            bypassAppLovinMaxReward(listener, maxRewardedAdInstance);
        } else {
            Log.w(TAG, "MaxRewardedAdListener not found on instance or saved listener");
        }
    }

    /**
     * Dispatches the full AppLovin MAX rewarded ad lifecycle:
     * 1. onAdDisplayed(MaxAd)
     * 2. onUserRewarded(MaxAd, MaxReward)
     * 3. onAdHidden(MaxAd)
     */
    public static void bypassAppLovinMaxReward(Object listenerObj, Object maxAdObj) {
        if (listenerObj == null) return;

        MAIN_HANDLER.post(() -> {
            try {
                Object effectiveMaxAd = maxAdObj;

                // 1. onAdDisplayed(MaxAd)
                for (Method m : listenerObj.getClass().getMethods()) {
                    if ("onAdDisplayed".equals(m.getName()) && m.getParameterTypes().length == 1) {
                        Class<?> paramType = m.getParameterTypes()[0];
                        if (effectiveMaxAd == null || !paramType.isInstance(effectiveMaxAd)) {
                            effectiveMaxAd = createDummyMaxAdProxy(paramType);
                        }
                        try {
                            m.invoke(listenerObj, effectiveMaxAd);
                            Log.i(TAG, "Invoked AppLovin onAdDisplayed!");
                        } catch (Throwable t) {
                            Log.e(TAG, "Error invoking onAdDisplayed", t);
                        }
                        break;
                    }
                }

                final Object finalMaxAd = effectiveMaxAd;

                // 2. onUserRewarded(MaxAd, MaxReward) after 50ms
                MAIN_HANDLER.postDelayed(() -> {
                    try {
                        for (Method m : listenerObj.getClass().getMethods()) {
                            if ("onUserRewarded".equals(m.getName()) && m.getParameterTypes().length == 2) {
                                Class<?>[] params = m.getParameterTypes();
                                Object dummyReward = createDummyMaxRewardProxy(params[1]);
                                m.invoke(listenerObj, finalMaxAd, dummyReward);
                                Log.i(TAG, "Invoked AppLovin onUserRewarded!");
                                break;
                            }
                        }

                        // 3. onAdHidden(MaxAd) after another 50ms
                        MAIN_HANDLER.postDelayed(() -> {
                            try {
                                for (Method m : listenerObj.getClass().getMethods()) {
                                    if ("onAdHidden".equals(m.getName()) && m.getParameterTypes().length == 1) {
                                        m.invoke(listenerObj, finalMaxAd);
                                        Log.i(TAG, "Invoked AppLovin onAdHidden!");
                                        break;
                                    }
                                }
                            } catch (Throwable t) {
                                Log.e(TAG, "Error invoking onAdHidden", t);
                            }
                        }, 50);

                    } catch (Throwable t) {
                        Log.e(TAG, "Error invoking onUserRewarded", t);
                    }
                }, 50);

            } catch (Throwable t) {
                Log.e(TAG, "Error triggering AppLovin MAX callbacks", t);
            }
        });
    }

    // =========================================================================
    // 2. Unity Ads
    // =========================================================================

    /**
     * Intercepts UnityAds.load(placementId, loadListener).
     * Immediately notifies the listener that the ad is loaded.
     */
    public static void bypassUnityAdsLoad(Object placementIdObj, Object loadListenerObj) {
        final String placementId = placementIdObj != null ? placementIdObj.toString() : "rewardedVideo";
        Log.i(TAG, "bypassUnityAdsLoad for placement: " + placementId);
        if (loadListenerObj == null) return;

        MAIN_HANDLER.post(() -> {
            try {
                for (Method m : loadListenerObj.getClass().getMethods()) {
                    if ("onUnityAdsAdLoaded".equals(m.getName()) && m.getParameterTypes().length == 1) {
                        m.invoke(loadListenerObj, placementId);
                        Log.i(TAG, "Invoked onUnityAdsAdLoaded for " + placementId);
                        return;
                    }
                }
            } catch (Throwable t) {
                Log.e(TAG, "Error in bypassUnityAdsLoad", t);
            }
        });
    }

    public static void bypassUnityAdsShow(Object p1, Object p2) {
        handleUnityAdsShow(new Object[]{p1, p2});
    }

    public static void bypassUnityAdsShow(Object p1, Object p2, Object p3) {
        handleUnityAdsShow(new Object[]{p1, p2, p3});
    }

    public static void bypassUnityAdsShow(Object p1, Object p2, Object p3, Object p4) {
        handleUnityAdsShow(new Object[]{p1, p2, p3, p4});
    }

    private static void handleUnityAdsShow(Object[] params) {
        Log.i(TAG, "handleUnityAdsShow called with " + params.length + " args");
        Object listener = null;
        String placementId = "rewardedVideo";

        for (Object p : params) {
            if (p == null) continue;
            if (p instanceof String) {
                placementId = (String) p;
            } else {
                for (Method m : p.getClass().getMethods()) {
                    if ("onUnityAdsShowComplete".equals(m.getName())) {
                        listener = p;
                        break;
                    }
                }
            }
        }

        final Object finalListener = listener;
        final String finalPlacementId = placementId;
        if (finalListener == null) {
            Log.w(TAG, "UnityAds showListener not found in arguments");
            return;
        }

        MAIN_HANDLER.post(() -> {
            try {
                Class<?> listenerClass = finalListener.getClass();

                // 1. onUnityAdsShowStart
                for (Method m : listenerClass.getMethods()) {
                    if ("onUnityAdsShowStart".equals(m.getName()) && m.getParameterTypes().length == 1) {
                        try {
                            m.invoke(finalListener, finalPlacementId);
                        } catch (Throwable ignored) {}
                        break;
                    }
                }

                // 2. onUnityAdsShowComplete(placementId, COMPLETED)
                MAIN_HANDLER.postDelayed(() -> {
                    try {
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
                                m.invoke(finalListener, finalPlacementId, completedEnum);
                                Log.i(TAG, "Invoked UnityAds onUnityAdsShowComplete with COMPLETED");
                                return;
                            }
                        }
                    } catch (Throwable t) {
                        Log.e(TAG, "Error invoking onUnityAdsShowComplete", t);
                    }
                }, 50);

            } catch (Throwable t) {
                Log.e(TAG, "Error in handleUnityAdsShow", t);
            }
        });
    }

    // =========================================================================
    // 3. IronSource
    // =========================================================================

    public static void registerIronSourceListener(Object listenerObj) {
        Log.i(TAG, "registerIronSourceListener: " + listenerObj);
        sSavedIronSourceListener = listenerObj;
        if (listenerObj != null) {
            MAIN_HANDLER.post(() -> {
                try {
                    for (Method m : listenerObj.getClass().getMethods()) {
                        if ("onRewardedVideoAvailabilityChanged".equals(m.getName()) && m.getParameterTypes().length == 1) {
                            m.invoke(listenerObj, true);
                            Log.i(TAG, "Invoked onRewardedVideoAvailabilityChanged(true)");
                            break;
                        }
                    }
                } catch (Throwable t) {
                    Log.e(TAG, "Error in registerIronSourceListener", t);
                }
            });
        }
    }

    public static void bypassIronSourceReward(Object placementObj) {
        Log.i(TAG, "bypassIronSourceReward called");
        Object listener = sSavedIronSourceListener;
        if (listener == null) return;

        MAIN_HANDLER.post(() -> {
            try {
                // onRewardedVideoAdOpened
                for (Method m : listener.getClass().getMethods()) {
                    if ("onRewardedVideoAdOpened".equals(m.getName()) && m.getParameterTypes().length == 0) {
                        try { m.invoke(listener); } catch (Throwable ignored) {}
                        break;
                    }
                }

                MAIN_HANDLER.postDelayed(() -> {
                    try {
                        // onRewardedVideoAdRewarded
                        for (Method m : listener.getClass().getMethods()) {
                            if ("onRewardedVideoAdRewarded".equals(m.getName())) {
                                m.invoke(listener, placementObj);
                                Log.i(TAG, "Invoked IronSource onRewardedVideoAdRewarded!");
                                break;
                            }
                        }

                        // onRewardedVideoAdClosed
                        MAIN_HANDLER.postDelayed(() -> {
                            try {
                                for (Method m : listener.getClass().getMethods()) {
                                    if ("onRewardedVideoAdClosed".equals(m.getName()) && m.getParameterTypes().length == 0) {
                                        m.invoke(listener);
                                        Log.i(TAG, "Invoked IronSource onRewardedVideoAdClosed!");
                                        break;
                                    }
                                }
                            } catch (Throwable t) {
                                Log.e(TAG, "Error in onRewardedVideoAdClosed", t);
                            }
                        }, 50);

                    } catch (Throwable t) {
                        Log.e(TAG, "Error in onRewardedVideoAdRewarded", t);
                    }
                }, 50);

            } catch (Throwable t) {
                Log.e(TAG, "Error in bypassIronSourceReward", t);
            }
        });
    }

    // =========================================================================
    // 4. Google Mobile Ads (AdMob)
    // =========================================================================

    public static void bypassGoogleRewardedAd(Object activityObj, Object listenerObj) {
        Log.i(TAG, "Bypassing Google Mobile Ads RewardedAd.show...");
        if (listenerObj == null) {
            Log.w(TAG, "OnUserEarnedRewardListener is null");
            return;
        }

        MAIN_HANDLER.post(() -> {
            try {
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

    public static void bypassUnityRewardedAdCallback(Object callbackObj) {
        Log.i(TAG, "Bypassing Google UnityRewardedAd callback...");
        if (callbackObj == null) return;

        MAIN_HANDLER.post(() -> {
            try {
                // 1. onUserEarnedReward(String, float)
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

                // 2. onAdDismissedFullScreenContent()
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

    // =========================================================================
    // Reflection & Proxy Helpers
    // =========================================================================

    private static Object createDummyMaxAdProxy(Class<?> interfaceClass) {
        if (interfaceClass == null || !interfaceClass.isInterface()) return null;
        return Proxy.newProxyInstance(
            interfaceClass.getClassLoader(),
            new Class<?>[]{interfaceClass},
            (proxy, method, args) -> {
                String name = method.getName();
                if ("getAdUnitId".equals(name)) return "rewardedAd";
                if ("getPlacement".equals(name)) return "default";
                if ("getNetworkName".equals(name)) return "AppLovin";
                if ("getFormat".equals(name)) return null;
                if ("getSize".equals(name)) return null;
                if ("getRevenue".equals(name)) return 0.0;
                if ("toString".equals(name)) return "DummyMaxAd";
                if ("hashCode".equals(name)) return 1;
                if ("equals".equals(name)) return args != null && args.length > 0 && args[0] == proxy;
                return null;
            }
        );
    }

    private static Object createDummyMaxRewardProxy(Class<?> interfaceClass) {
        if (interfaceClass == null || !interfaceClass.isInterface()) return null;
        return Proxy.newProxyInstance(
            interfaceClass.getClassLoader(),
            new Class<?>[]{interfaceClass},
            (proxy, method, args) -> {
                String name = method.getName();
                if ("getAmount".equals(name)) return 1;
                if ("getLabel".equals(name)) return "reward";
                return null;
            }
        );
    }

    private static Object createDummyRewardItemProxy(Class<?> interfaceClass) {
        if (interfaceClass == null || !interfaceClass.isInterface()) return null;
        return Proxy.newProxyInstance(
            interfaceClass.getClassLoader(),
            new Class<?>[]{interfaceClass},
            (proxy, method, args) -> {
                String name = method.getName();
                if ("getAmount".equals(name)) return 1;
                if ("getType".equals(name)) return "reward";
                return null;
            }
        );
    }

    private static Object findListenerInObject(Object root, String targetInterfaceName) {
        if (root == null) return null;
        Set<Object> visited = new HashSet<>();
        return searchFieldRecursive(root, targetInterfaceName, visited, 0);
    }

    private static Object searchFieldRecursive(Object current, String interfaceName, Set<Object> visited, int depth) {
        if (current == null || depth > 3 || !visited.add(current)) return null;

        for (Class<?> iface : current.getClass().getInterfaces()) {
            if (iface.getName().contains(interfaceName)) {
                return current;
            }
        }

        Class<?> clazz = current.getClass();
        while (clazz != null && clazz != Object.class) {
            for (Field field : clazz.getDeclaredFields()) {
                try {
                    field.setAccessible(true);
                    Object val = field.get(current);
                    if (val != null) {
                        for (Class<?> iface : val.getClass().getInterfaces()) {
                            if (iface.getName().contains(interfaceName)) {
                                return val;
                            }
                        }
                        if (depth < 2 && !isPrimitiveOrWrapper(val.getClass())) {
                            Object found = searchFieldRecursive(val, interfaceName, visited, depth + 1);
                            if (found != null) return found;
                        }
                    }
                } catch (Throwable ignored) {}
            }
            clazz = clazz.getSuperclass();
        }
        return null;
    }

    private static Object findListenerByMethod(Object root, String methodName) {
        if (root == null) return null;
        Class<?> clazz = root.getClass();
        while (clazz != null && clazz != Object.class) {
            for (Field field : clazz.getDeclaredFields()) {
                try {
                    field.setAccessible(true);
                    Object val = field.get(root);
                    if (val != null) {
                        for (Method m : val.getClass().getMethods()) {
                            if (m.getName().equals(methodName)) {
                                return val;
                            }
                        }
                    }
                } catch (Throwable ignored) {}
            }
            clazz = clazz.getSuperclass();
        }
        return null;
    }

    private static boolean isPrimitiveOrWrapper(Class<?> type) {
        return type.isPrimitive() ||
               type == String.class ||
               type == Integer.class ||
               type == Long.class ||
               type == Boolean.class ||
               type == Float.class ||
               type == Double.class;
    }
}

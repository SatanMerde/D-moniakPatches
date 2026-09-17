package com.dmoniak.patches.extension;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashSet;
import java.util.Set;

/**
 * Extension helper for Hungry Shark World Morphe Patch.
 * 
 * DISCLAIMER:
 * 100% AI Generated code for educational and research purposes only.
 * No liability accepted.
 */
public final class HungrySharkAdsRewardHelper {

    private static final String TAG = "D-moniakPatches";
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    private HungrySharkAdsRewardHelper() {}

    /**
     * Always returns true for ad availability / readiness checks.
     */
    public static boolean isAdReady() {
        Log.i(TAG, "Ad availability check intercepted: returning true");
        return true;
    }

    /**
     * Intercepts AppLovin MaxRewardedAd.showAd(...) calls.
     */
    public static void bypassMaxRewardedAd(Object maxRewardedAdInstance) {
        Log.i(TAG, "Bypassing AppLovin MaxRewardedAd.showAd...");
        if (maxRewardedAdInstance == null) return;

        MAIN_HANDLER.post(() -> {
            try {
                Object listener = findListenerInObject(maxRewardedAdInstance, "MaxRewardedAdListener");
                if (listener == null) {
                    listener = findListenerByMethod(maxRewardedAdInstance, "onUserRewarded");
                }

                if (listener != null) {
                    bypassAppLovinMaxReward(listener, null);
                } else {
                    Log.w(TAG, "MaxRewardedAdListener not found on instance");
                }
            } catch (Throwable t) {
                Log.e(TAG, "Error in bypassMaxRewardedAd", t);
            }
        });
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
     */
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

                // onUnityAdsShowStart
                try {
                    for (Method m : listenerClass.getMethods()) {
                        if ("onUnityAdsShowStart".equals(m.getName()) && m.getParameterTypes().length == 1) {
                            m.invoke(showListenerObj, placementId);
                            break;
                        }
                    }
                } catch (Throwable ignored) {}

                // onUnityAdsShowComplete(placementId, COMPLETED)
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
                        Log.i(TAG, "Invoked UnityAds onUnityAdsShowComplete with COMPLETED");
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
        Log.i(TAG, "Triggering AppLovin MAX rewarded callbacks...");
        if (listenerObj == null) return;

        MAIN_HANDLER.post(() -> {
            try {
                // onUserRewarded(MaxAd, MaxReward)
                for (Method m : listenerObj.getClass().getMethods()) {
                    if ("onUserRewarded".equals(m.getName())) {
                        Class<?>[] params = m.getParameterTypes();
                        if (params.length == 2) {
                            Object dummyReward = createDummyMaxRewardProxy(params[1]);
                            m.invoke(listenerObj, maxAdObj, dummyReward);
                            Log.i(TAG, "Invoked AppLovin onUserRewarded!");
                        }
                    }
                }

                // onAdHidden(MaxAd)
                for (Method m : listenerObj.getClass().getMethods()) {
                    if ("onAdHidden".equals(m.getName())) {
                        Class<?>[] params = m.getParameterTypes();
                        if (params.length == 1) {
                            m.invoke(listenerObj, maxAdObj);
                            Log.i(TAG, "Invoked AppLovin onAdHidden!");
                        }
                    }
                }
            } catch (Throwable t) {
                Log.e(TAG, "Error triggering AppLovin MAX callbacks", t);
            }
        });
    }

    /**
     * Intercepts IronSource Rewarded Video callbacks.
     */
    public static void bypassIronSourceReward(Object listenerObj, Object placementObj) {
        Log.i(TAG, "Triggering IronSource Rewarded Video callbacks...");
        if (listenerObj == null) return;

        MAIN_HANDLER.post(() -> {
            try {
                for (Method m : listenerObj.getClass().getMethods()) {
                    if ("onRewardedVideoAdRewarded".equals(m.getName())) {
                        m.invoke(listenerObj, placementObj);
                        Log.i(TAG, "Invoked IronSource onRewardedVideoAdRewarded!");
                    }
                }

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

    private static Object createDummyRewardItemProxy(Class<?> interfaceClass) {
        if (!interfaceClass.isInterface()) return null;
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
}

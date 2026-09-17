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
import org.json.JSONObject;

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
     * Intercepts AppLovin MAX Unity Plugin MaxUnityPlugin.loadRewardedAd(String).
     * Immediately notifies Unity with OnRewardedAdLoadedEvent so the game considers the ad loaded.
     */
    public static void bypassMaxUnityPluginLoad(String adUnitId) {
        Log.i(TAG, "bypassMaxUnityPluginLoad called for adUnitId: " + adUnitId);
        final String unitId = (adUnitId != null && !adUnitId.trim().isEmpty()) ? adUnitId.trim() : "rewardedAd";
        MAIN_HANDLER.post(() -> {
            try {
                JSONObject loadedJson = new JSONObject();
                loadedJson.put("name", "OnRewardedAdLoadedEvent");
                loadedJson.put("adUnitId", unitId);
                loadedJson.put("adFormat", "REWARDED");
                loadedJson.put("networkName", "AppLovin");
                sendMaxUnityEvent(loadedJson);
            } catch (Throwable t) {
                Log.e(TAG, "Error in bypassMaxUnityPluginLoad", t);
            }
        });
    }

    /**
     * Intercepts AppLovin MAX Unity Plugin MaxUnityPlugin.showRewardedAd(...)
     * and MaxUnityAdManager.showRewardedAd(...) calls.
     * Dispatches OnRewardedAdDisplayedEvent, OnRewardedAdReceivedRewardEvent, and OnRewardedAdHiddenEvent
     * directly back to Unity via JNI callback so the player receives their reward immediately.
     */
    public static void bypassMaxUnityPluginShow(String adUnitId, String placement, String customData) {
        Log.i(TAG, "bypassMaxUnityPluginShow called for adUnitId: " + adUnitId + ", placement: " + placement);
        final String unitId = (adUnitId != null && !adUnitId.trim().isEmpty()) ? adUnitId.trim() : "rewardedAd";
        final String plc = placement != null ? placement : "";

        MAIN_HANDLER.post(() -> {
            try {
                // 1. Displayed event
                JSONObject displayJson = new JSONObject();
                displayJson.put("name", "OnRewardedAdDisplayedEvent");
                displayJson.put("adUnitId", unitId);
                displayJson.put("adFormat", "REWARDED");
                displayJson.put("networkName", "AppLovin");
                displayJson.put("placement", plc);
                sendMaxUnityEvent(displayJson);

                // 2. Received Reward event
                JSONObject rewardJson = new JSONObject();
                rewardJson.put("name", "OnRewardedAdReceivedRewardEvent");
                rewardJson.put("adUnitId", unitId);
                rewardJson.put("adFormat", "REWARDED");
                rewardJson.put("networkName", "AppLovin");
                rewardJson.put("placement", plc);
                rewardJson.put("rewardLabel", "reward");
                rewardJson.put("rewardAmount", 1);
                sendMaxUnityEvent(rewardJson);

                // 3. Hidden event after 100ms
                MAIN_HANDLER.postDelayed(() -> {
                    try {
                        JSONObject hiddenJson = new JSONObject();
                        hiddenJson.put("name", "OnRewardedAdHiddenEvent");
                        hiddenJson.put("adUnitId", unitId);
                        hiddenJson.put("adFormat", "REWARDED");
                        hiddenJson.put("networkName", "AppLovin");
                        hiddenJson.put("placement", plc);
                        sendMaxUnityEvent(hiddenJson);
                        Log.i(TAG, "Completed MAX Unity rewarded ad bypass cycle!");
                    } catch (Throwable t) {
                        Log.e(TAG, "Error sending hidden event", t);
                    }
                }, 100);

            } catch (Throwable t) {
                Log.e(TAG, "Error in bypassMaxUnityPluginShow", t);
            }
        });
    }

    /**
     * Forwards an event JSONObject to Unity via AppLovin MAX Unity Plugin internal mechanisms.
     */
    public static void sendMaxUnityEvent(JSONObject eventProps) {
        if (eventProps == null) return;
        try {
            boolean sent = false;

            // Strategy 1: MaxUnityAdManager.forwardUnityEvent(JSONObject)
            try {
                Class<?> adManagerClass = Class.forName("com.applovin.mediation.unity.MaxUnityAdManager");
                for (Method m : adManagerClass.getDeclaredMethods()) {
                    if ("forwardUnityEvent".equals(m.getName()) && m.getParameterTypes().length == 1) {
                        m.setAccessible(true);
                        m.invoke(null, eventProps);
                        sent = true;
                        Log.i(TAG, "Sent MAX event via forwardUnityEvent: " + eventProps.optString("name"));
                        break;
                    }
                }
            } catch (Throwable t) {
                Log.d(TAG, "forwardUnityEvent reflection failed: " + t.getMessage());
            }

            // Strategy 2: backgroundCallback.onEvent(String)
            if (!sent) {
                try {
                    Class<?> adManagerClass = Class.forName("com.applovin.mediation.unity.MaxUnityAdManager");
                    for (Field f : adManagerClass.getDeclaredFields()) {
                        if ("backgroundCallback".equals(f.getName())) {
                            f.setAccessible(true);
                            Object callback = f.get(null);
                            if (callback != null) {
                                Method onEvent = callback.getClass().getMethod("onEvent", String.class);
                                onEvent.invoke(callback, eventProps.toString());
                                sent = true;
                                Log.i(TAG, "Sent MAX event via backgroundCallback.onEvent: " + eventProps.optString("name"));
                            }
                            break;
                        }
                    }
                } catch (Throwable t) {
                    Log.d(TAG, "backgroundCallback reflection failed: " + t.getMessage());
                }
            }

            if (!sent) {
                Log.w(TAG, "Could not send MAX event: " + eventProps.optString("name"));
            }
        } catch (Throwable t) {
            Log.e(TAG, "Error in sendMaxUnityEvent", t);
        }
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

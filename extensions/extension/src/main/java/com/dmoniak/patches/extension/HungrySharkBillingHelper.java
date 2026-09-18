package com.dmoniak.patches.extension;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.json.JSONObject;

/**
 * Extension helper for Hungry Shark World Morphe Patch - Free Shopping / In-App Billing Bypass.
 * 
 * Intercepts Google Play Billing Client and UnityPurchasing calls so that any
 * shop purchase (gems, coins, pearls, bundles) is immediately reported as successful
 * without requiring real-money payment.
 * 
 * DISCLAIMER:
 * 100% AI Generated code for educational and research purposes only.
 * No liability accepted.
 */
public final class HungrySharkBillingHelper {

    private static final String TAG = "D-moniakPatches";
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    private static volatile Object sSavedPurchasesUpdatedListener = null;
    private static volatile Object sSavedBillingClient = null;

    private HungrySharkBillingHelper() {}

    /**
     * Registers a PurchasesUpdatedListener instance captured from initialization
     * or GooglePlayPurchasing bridge.
     */
    public static void registerPurchasesUpdatedListener(Object listener) {
        if (listener == null) return;
        Log.i(TAG, "registerPurchasesUpdatedListener: " + listener.getClass().getName());
        sSavedPurchasesUpdatedListener = listener;
    }

    /**
     * Always returns true for billing readiness checks.
     */
    public static boolean isBillingReady() {
        Log.i(TAG, "isBillingReady intercepted: returning true");
        return true;
    }

    /**
     * Always returns true for receipt/signature verification checks.
     */
    public static boolean verifyPurchase(String base64PublicKey, String signedData, String signature) {
        Log.i(TAG, "verifyPurchase check intercepted: returning true");
        return true;
    }

    /**
     * Intercepts BillingClient.launchBillingFlow.
     * 
     * Constructs a simulated purchase for the requested SKU, fires onPurchasesUpdated
     * on the registered listener with BillingResponseCode.OK, and returns an OK BillingResult.
     */
    public static Object handleLaunchBillingFlow(Object billingClient, Activity activity, Object billingFlowParams) {
        Log.i(TAG, "handleLaunchBillingFlow intercepted!");
        if (billingClient != null) {
            sSavedBillingClient = billingClient;
        }

        // 1. Extract product SKU from billingFlowParams
        String sku = extractSkuFromParams(billingFlowParams);
        Log.i(TAG, "Extracted SKU to purchase: " + sku);

        // 2. Build OK BillingResult
        Object okResult = buildOkBillingResult();

        // 3. Build fake Purchase object
        String packageName = (activity != null) ? activity.getPackageName() : "com.ubisoft.hungrysharkworld";
        Object purchase = createFakePurchase(sku, packageName);

        // 4. Deliver purchase callback on Main Thread
        final List<Object> purchasesList = new ArrayList<>();
        if (purchase != null) {
            purchasesList.add(purchase);
        }

        deliverPurchasesUpdated(billingClient, okResult, purchasesList, sku);

        return okResult;
    }

    /**
     * Intercepts BillingClient.consumeAsync to immediately report consumable items as consumed.
     * This allows purchasing consumables (gems, coins, pearls) multiple times.
     */
    public static void handleConsumeAsync(Object billingClient, Object consumeParams, final Object listener) {
        Log.i(TAG, "handleConsumeAsync intercepted");
        final Object okResult = buildOkBillingResult();
        final String purchaseToken = extractPurchaseToken(consumeParams);

        if (listener == null) return;

        MAIN_HANDLER.post(new Runnable() {
            @Override
            public void run() {
                try {
                    for (Method m : listener.getClass().getMethods()) {
                        if ("onConsumeResponse".equals(m.getName()) && m.getParameterTypes().length == 2) {
                            m.invoke(listener, okResult, purchaseToken);
                            Log.i(TAG, "onConsumeResponse invoked successfully for token: " + purchaseToken);
                            return;
                        }
                    }
                } catch (Throwable t) {
                    Log.e(TAG, "Error invoking onConsumeResponse", t);
                }
            }
        });
    }

    /**
     * Intercepts BillingClient.acknowledgePurchase to immediately report purchases as acknowledged.
     */
    public static void handleAcknowledgePurchase(Object billingClient, Object acknowledgePurchaseParams, final Object listener) {
        Log.i(TAG, "handleAcknowledgePurchase intercepted");
        final Object okResult = buildOkBillingResult();

        if (listener == null) return;

        MAIN_HANDLER.post(new Runnable() {
            @Override
            public void run() {
                try {
                    for (Method m : listener.getClass().getMethods()) {
                        if ("onAcknowledgePurchaseResponse".equals(m.getName()) && m.getParameterTypes().length == 1) {
                            m.invoke(listener, okResult);
                            Log.i(TAG, "onAcknowledgePurchaseResponse invoked successfully");
                            return;
                        }
                    }
                } catch (Throwable t) {
                    Log.e(TAG, "Error invoking onAcknowledgePurchaseResponse", t);
                }
            }
        });
    }

    /**
     * Intercepts BillingClient.isFeatureSupported to report all features supported.
     */
    public static Object handleIsFeatureSupported(Object billingClient, String feature) {
        Log.i(TAG, "isFeatureSupported intercepted for feature: " + feature);
        return buildOkBillingResult();
    }

    // =========================================================================
    // Internal Helper Methods
    // =========================================================================

    private static String extractSkuFromParams(Object params) {
        if (params == null) return "com.ubisoft.hungrysharkworld.default_item";

        // Strategy A: Direct getter methods (getSku, zza, getProductId, etc.)
        try {
            for (Method m : params.getClass().getMethods()) {
                if (m.getParameterTypes().length == 0 && m.getReturnType() == String.class) {
                    String name = m.getName().toLowerCase();
                    if (name.contains("sku") || name.contains("product")) {
                        Object val = m.invoke(params);
                        if (val != null && !val.toString().trim().isEmpty()) {
                            return val.toString().trim();
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}

        // Strategy B: Nested ProductDetailsParams / SkuDetails list
        try {
            for (Method m : params.getClass().getMethods()) {
                if (m.getParameterTypes().length == 0 && List.class.isAssignableFrom(m.getReturnType())) {
                    Object listObj = m.invoke(params);
                    if (listObj instanceof List) {
                        List<?> list = (List<?>) listObj;
                        for (Object item : list) {
                            if (item != null) {
                                String itemSku = extractSkuFromObject(item);
                                if (itemSku != null && !itemSku.isEmpty()) {
                                    return itemSku;
                                }
                            }
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}

        // Strategy C: Inspect declared fields recursively
        try {
            for (Field f : params.getClass().getDeclaredFields()) {
                f.setAccessible(true);
                Object val = f.get(params);
                if (val instanceof String) {
                    String s = (String) val;
                    if (s.contains("hungryshark") || s.contains("gems") || s.contains("gold") || s.contains("coins") || s.contains("pack")) {
                        return s;
                    }
                } else if (val != null) {
                    String itemSku = extractSkuFromObject(val);
                    if (itemSku != null && !itemSku.isEmpty()) {
                        return itemSku;
                    }
                }
            }
        } catch (Throwable ignored) {}

        return "com.ubisoft.hungrysharkworld.shop_item";
    }

    private static String extractSkuFromObject(Object obj) {
        if (obj == null) return null;
        try {
            for (Method m : obj.getClass().getMethods()) {
                if (m.getParameterTypes().length == 0 && m.getReturnType() == String.class) {
                    String name = m.getName().toLowerCase();
                    if (name.contains("sku") || name.contains("productid") || name.contains("id")) {
                        Object res = m.invoke(obj);
                        if (res != null && !res.toString().trim().isEmpty()) {
                            return res.toString().trim();
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private static String extractPurchaseToken(Object consumeParams) {
        if (consumeParams == null) return "morphe_token_" + System.currentTimeMillis();
        try {
            for (Method m : consumeParams.getClass().getMethods()) {
                if ("getPurchaseToken".equals(m.getName()) && m.getParameterTypes().length == 0) {
                    Object token = m.invoke(consumeParams);
                    if (token != null) return token.toString();
                }
            }
        } catch (Throwable ignored) {}
        return "morphe_token_" + System.currentTimeMillis();
    }

    public static Object buildOkBillingResult() {
        try {
            Class<?> billingResultClass = Class.forName("com.android.billingclient.api.BillingResult");
            Method newBuilderMethod = billingResultClass.getMethod("newBuilder");
            Object builder = newBuilderMethod.invoke(null);
            Method setResponseCodeMethod = builder.getClass().getMethod("setResponseCode", int.class);
            setResponseCodeMethod.invoke(builder, 0); // 0 = BillingResponseCode.OK
            Method buildMethod = builder.getClass().getMethod("build");
            return buildMethod.invoke(builder);
        } catch (Throwable t) {
            Log.e(TAG, "Failed to build BillingResult via reflection", t);
            return null;
        }
    }

    private static Object createFakePurchase(String sku, String packageName) {
        try {
            long now = System.currentTimeMillis();
            long r1 = 1000 + (long) (Math.random() * 9000);
            long r2 = 1000 + (long) (Math.random() * 9000);
            long r3 = 1000 + (long) (Math.random() * 9000);
            long r4 = 10000 + (long) (Math.random() * 90000);
            String orderId = "GPA." + r1 + "-" + r2 + "-" + r3 + "-" + r4;

            JSONObject json = new JSONObject();
            json.put("orderId", orderId);
            json.put("packageName", packageName);
            json.put("productId", sku);
            json.put("purchaseTime", now);
            json.put("purchaseState", 1); // 1 = PURCHASED
            json.put("purchaseToken", "morphe_token_" + now);
            json.put("quantity", 1);
            json.put("acknowledged", false);

            Class<?> purchaseClass = Class.forName("com.android.billingclient.api.Purchase");
            Constructor<?> ctor = purchaseClass.getConstructor(String.class, String.class);
            return ctor.newInstance(json.toString(), "morphe_valid_signature");
        } catch (Throwable t) {
            Log.e(TAG, "Failed to create fake Purchase", t);
            return null;
        }
    }

    private static void deliverPurchasesUpdated(final Object billingClient, final Object billingResult, final List<Object> purchases, final String sku) {
        MAIN_HANDLER.post(new Runnable() {
            @Override
            public void run() {
                Object listener = sSavedPurchasesUpdatedListener;
                if (listener == null && billingClient != null) {
                    listener = findPurchasesUpdatedListener(billingClient);
                }

                if (listener != null) {
                    try {
                        Method targetMethod = null;
                        for (Method m : listener.getClass().getMethods()) {
                            if ("onPurchasesUpdated".equals(m.getName()) && m.getParameterTypes().length == 2) {
                                targetMethod = m;
                                break;
                            }
                        }
                        if (targetMethod != null) {
                            targetMethod.invoke(listener, billingResult, purchases);
                            Log.i(TAG, "Successfully fired onPurchasesUpdated on listener: " + listener.getClass().getName() + " for SKU: " + sku);
                            return;
                        }
                    } catch (Throwable t) {
                        Log.e(TAG, "Error invoking onPurchasesUpdated on listener", t);
                    }
                } else {
                    Log.w(TAG, "Could not find PurchasesUpdatedListener instance to notify");
                }
            }
        });
    }

    private static Object findPurchasesUpdatedListener(Object billingClient) {
        if (billingClient == null) return null;
        Set<Object> visited = new HashSet<>();
        return searchListenerRecursive(billingClient, 0, visited);
    }

    private static Object searchListenerRecursive(Object obj, int depth, Set<Object> visited) {
        if (obj == null || depth > 2 || visited.contains(obj)) return null;
        visited.add(obj);

        try {
            Class<?> listenerInterface = Class.forName("com.android.billingclient.api.PurchasesUpdatedListener");
            if (listenerInterface.isAssignableFrom(obj.getClass())) {
                return obj;
            }
        } catch (Throwable ignored) {}

        // Check declared fields
        Class<?> curr = obj.getClass();
        while (curr != null && curr != Object.class) {
            for (Field f : curr.getDeclaredFields()) {
                try {
                    f.setAccessible(true);
                    Object val = f.get(obj);
                    if (val != null) {
                        try {
                            Class<?> listenerInterface = Class.forName("com.android.billingclient.api.PurchasesUpdatedListener");
                            if (listenerInterface.isAssignableFrom(val.getClass())) {
                                return val;
                            }
                        } catch (Throwable ignored) {}

                        if (depth < 2) {
                            Object nested = searchListenerRecursive(val, depth + 1, visited);
                            if (nested != null) return nested;
                        }
                    }
                } catch (Throwable ignored) {}
            }
            curr = curr.getSuperclass();
        }
        return null;
    }
}

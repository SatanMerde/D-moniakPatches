package com.dmoniak.patches.extension;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Extension helper for Hungry Shark World Morphe Patch - Free Shopping / In-App Billing Bypass.
 * 
 * Intercepts Google Play Billing Client calls so that any shop purchase (gems, coins, pearls, bundles)
 * is immediately reported as successful without requiring real-money payment and without opening Google Play.
 * 
 * DISCLAIMER:
 * 100% AI Generated code for educational and research purposes only.
 * No liability accepted.
 */
public final class HungrySharkBillingHelper {

    private static final String TAG = "D-moniakPatches";
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());
    private static volatile Object sCachedOkBillingResult = null;

    private HungrySharkBillingHelper() {}

    /**
     * Intercepts BillingClient.launchBillingFlow.
     * 
     * Constructs a simulated purchase for the requested SKU, fires onPurchasesUpdated
     * on registered listeners with BillingResponseCode.OK, and returns an OK BillingResult.
     */
    public static Object handleLaunchBillingFlow(Object billingClient, Activity activity, Object billingFlowParams) {
        Log.i(TAG, "handleLaunchBillingFlow intercepted!");

        // 1. Extract product SKU from billingFlowParams
        final String sku = extractSkuFromParams(billingFlowParams);
        Log.i(TAG, "Extracted SKU to purchase: " + sku);

        // 2. Build OK BillingResult
        final Object okResult = buildOkBillingResult();

        // 3. Build fake Purchase object
        String packageName = (activity != null) ? activity.getPackageName() : "com.ubisoft.hungrysharkworld";
        Object purchase = createFakePurchase(sku, packageName);

        // 4. Deliver purchase callback on Main Thread
        final List<Object> purchasesList = new ArrayList<Object>();
        if (purchase != null) {
            purchasesList.add(purchase);
        }

        final Object finalClient = billingClient;
        final Activity finalActivity = activity;
        MAIN_HANDLER.post(new Runnable() {
            @Override
            public void run() {
                deliverPurchasesUpdated(finalClient, finalActivity, okResult, purchasesList, sku);
            }
        });

        return okResult;
    }

    private static void deliverPurchasesUpdated(Object billingClient, Activity activity, Object billingResult, List<Object> purchases, String sku) {
        Object listener = null;
        if (billingClient != null) {
            listener = findPurchasesUpdatedListener(billingClient);
        }
        if (listener == null && activity != null) {
            listener = findPurchasesUpdatedListener(activity);
        }

        if (listener == null) {
            Log.w(TAG, "Could not find PurchasesUpdatedListener instance to notify");
            return;
        }

        try {
            for (Method m : listener.getClass().getMethods()) {
                if ("onPurchasesUpdated".equals(m.getName()) && m.getParameterTypes().length == 2) {
                    m.invoke(listener, billingResult, purchases);
                    Log.i(TAG, "Successfully fired onPurchasesUpdated on: " + listener.getClass().getName() + " for SKU: " + sku);
                    return;
                }
            }
        } catch (Throwable t) {
            Log.e(TAG, "Error invoking onPurchasesUpdated on " + listener.getClass().getName(), t);
        }
    }

    private static Object findPurchasesUpdatedListener(Object root) {
        if (root == null) return null;
        Set<Object> visited = new HashSet<Object>();
        return searchListenerRecursive(root, 0, visited);
    }

    private static Object searchListenerRecursive(Object obj, int depth, Set<Object> visited) {
        if (obj == null || depth > 4 || visited.contains(obj)) return null;
        visited.add(obj);

        if (isPurchasesUpdatedListener(obj)) {
            return obj;
        }

        Class<?> curr = obj.getClass();
        while (curr != null && curr != Object.class) {
            for (Field f : curr.getDeclaredFields()) {
                try {
                    f.setAccessible(true);
                    Object val = f.get(obj);
                    if (val != null && !visited.contains(val)) {
                        if (isPurchasesUpdatedListener(val)) {
                            return val;
                        }
                        if (depth < 4 && !val.getClass().getName().startsWith("java.")) {
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

    private static boolean isPurchasesUpdatedListener(Object obj) {
        if (obj == null) return false;
        try {
            for (Method m : obj.getClass().getMethods()) {
                if ("onPurchasesUpdated".equals(m.getName()) && m.getParameterTypes().length == 2) {
                    return true;
                }
            }
        } catch (Throwable ignored) {}
        return false;
    }

    private static String extractSkuFromParams(Object params) {
        if (params == null) return "com.ubisoft.hungrysharkworld.gems_pack_1";

        try {
            String s = params.toString();
            if (s != null && !s.isEmpty()) {
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(?:productId|mProductId|sku|mSku)='?([a-zA-Z0-9_.-]+)'?");
                java.util.regex.Matcher matcher = pattern.matcher(s);
                if (matcher.find()) {
                    String candidate = matcher.group(1);
                    if (isValidSku(candidate)) {
                        Log.i(TAG, "Extracted SKU from toString: " + candidate);
                        return candidate;
                    }
                }
            }
        } catch (Throwable ignored) {}

        Set<Object> visited = new HashSet<Object>();
        String sku = inspectForSku(params, 0, visited);
        if (sku != null && isValidSku(sku)) {
            Log.i(TAG, "Extracted SKU via reflection: " + sku);
            return sku;
        }

        return "com.ubisoft.hungrysharkworld.gems_pack_1";
    }

    private static String inspectForSku(Object obj, int depth, Set<Object> visited) {
        if (obj == null || depth > 3 || visited.contains(obj)) return null;
        visited.add(obj);

        try {
            for (Method m : obj.getClass().getMethods()) {
                if (m.getParameterTypes().length == 0) {
                    Class<?> ret = m.getReturnType();
                    if (ret == String.class) {
                        String name = m.getName().toLowerCase();
                        if (name.contains("sku") || name.contains("productid") || name.contains("product_id")) {
                            Object val = m.invoke(obj);
                            if (val != null) {
                                String s = val.toString().trim();
                                if (isValidSku(s)) return s;
                            }
                        }
                    } else if (List.class.isAssignableFrom(ret)) {
                        Object listObj = m.invoke(obj);
                        if (listObj instanceof List) {
                            for (Object item : (List<?>) listObj) {
                                String itemSku = inspectForSku(item, depth + 1, visited);
                                if (itemSku != null) return itemSku;
                            }
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}

        Class<?> curr = obj.getClass();
        while (curr != null && curr != Object.class) {
            for (Field f : curr.getDeclaredFields()) {
                try {
                    f.setAccessible(true);
                    Object val = f.get(obj);
                    if (val instanceof String) {
                        String s = (String) val;
                        if (isValidSku(s)) {
                            String fName = f.getName().toLowerCase();
                            if (fName.contains("sku") || fName.contains("product") || fName.contains("id") ||
                                s.contains("hungryshark") || s.contains("gem") || s.contains("coin") || s.contains("pearl") || s.contains("pack")) {
                                return s;
                            }
                        }
                    } else if (val instanceof List) {
                        for (Object item : (List<?>) val) {
                            String itemSku = inspectForSku(item, depth + 1, visited);
                            if (itemSku != null) return itemSku;
                        }
                    } else if (val != null && depth < 3 && !val.getClass().getName().startsWith("java.")) {
                        String nested = inspectForSku(val, depth + 1, visited);
                        if (nested != null) return nested;
                    }
                } catch (Throwable ignored) {}
            }
            curr = curr.getSuperclass();
        }
        return null;
    }

    private static boolean isValidSku(String s) {
        if (s == null) return false;
        String trimmed = s.trim();
        if (trimmed.length() < 2 || trimmed.length() > 100) return false;
        String lower = trimmed.toLowerCase();
        if (lower.equals("null") || lower.equals("true") || lower.equals("false")) return false;
        if (lower.startsWith("com.android.billingclient") || lower.startsWith("android.app")) return false;
        return true;
    }

    public static Object buildOkBillingResult() {
        if (sCachedOkBillingResult != null) {
            return sCachedOkBillingResult;
        }
        try {
            Class<?> billingResultClass = Class.forName("com.android.billingclient.api.BillingResult");
            for (Method m : billingResultClass.getMethods()) {
                if ("newBuilder".equals(m.getName()) && m.getParameterTypes().length == 0) {
                    Object builder = m.invoke(null);
                    for (Method bm : builder.getClass().getMethods()) {
                        if ("setResponseCode".equals(bm.getName()) && bm.getParameterTypes().length == 1) {
                            bm.invoke(builder, 0); // 0 = OK
                        }
                    }
                    for (Method bm : builder.getClass().getMethods()) {
                        if ("build".equals(bm.getName()) && bm.getParameterTypes().length == 0) {
                            sCachedOkBillingResult = bm.invoke(builder);
                            return sCachedOkBillingResult;
                        }
                    }
                }
            }
        } catch (Throwable t) {
            Log.e(TAG, "Failed to build BillingResult via reflection", t);
        }
        return null;
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
            JSONArray productIds = new JSONArray();
            productIds.put(sku);
            json.put("productIds", productIds);
            json.put("purchaseTime", now);
            json.put("purchaseState", 1); // 1 = PURCHASED
            json.put("purchaseToken", "morphe_token_" + now);
            json.put("quantity", 1);
            json.put("acknowledged", false);
            json.put("autoRenewing", false);
            json.put("developerPayload", "");

            Class<?> purchaseClass = Class.forName("com.android.billingclient.api.Purchase");
            Constructor<?> ctor = null;
            for (Constructor<?> c : purchaseClass.getDeclaredConstructors()) {
                if (c.getParameterTypes().length == 2 &&
                    c.getParameterTypes()[0] == String.class &&
                    c.getParameterTypes()[1] == String.class) {
                    ctor = c;
                    break;
                }
            }
            if (ctor != null) {
                ctor.setAccessible(true);
                return ctor.newInstance(json.toString(), "morphe_valid_signature");
            }
        } catch (Throwable t) {
            Log.e(TAG, "Failed to create fake Purchase", t);
        }
        return null;
    }
}

package com.example.diplom_staff_application;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Connection;
import org.jsoup.Jsoup;

import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class OrderContext {
    private static final String ORDERS_URL = "https://mgxymxiehfsptuubuqfv.supabase.co/rest/v1/orders";
    private static final String ORDER_ITEMS_URL = "https://mgxymxiehfsptuubuqfv.supabase.co/rest/v1/order_items";
    private static final String PRODUCT_SIZE_URL = "https://mgxymxiehfsptuubuqfv.supabase.co/rest/v1/product_size";
    private static final String PRODUCTS_URL = "https://mgxymxiehfsptuubuqfv.supabase.co/rest/v1/products";
    private static final String SIZES_URL = "https://mgxymxiehfsptuubuqfv.supabase.co/rest/v1/sizes";
    private static final String TOKEN = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im1neHlteGllaGZzcHR1dWJ1cWZ2Iiwicm9sZSI6InNlcnZpY2Vfcm9sZSIsImlhdCI6MTc0NTIyNzY0NSwiZXhwIjoyMDYwODAzNjQ1fQ.LNqLc1o8I8eZUxYuFXknXZZhzN5kRh0eggmg5tItiM0";
    private static final String SECRET = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im1neHlteGllaGZzcHR1dWJ1cWZ2Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDUyMjc2NDUsImV4cCI6MjA2MDgwMzY0NX0.QXcy5Dpd4_b58-xfpvPAIgm9U8Pj6w62RW6p7NDUKyQ";

    public interface OrderCallback {
        void onSuccess(long orderId);
        void onError(String error);
    }

    public interface LoadOrdersCallback {
        void onSuccess(List<Order> orders);
        void onError(String error);
    }

    public interface LoadOrderItemsCallback {
        void onSuccess(List<CartItem> items, String status);
        void onError(String error);
    }

    public interface UpdateOrderCallback {
        void onSuccess();
        void onError(String error);
    }

    public static void loadOrderItems(long orderId, LoadOrderItemsCallback callback) {
        new LoadOrderItemsTask(orderId, callback).execute();
    }

    public static void updateOrderStatus(long orderId, String newStatus, UpdateOrderCallback callback) {
        new UpdateOrderStatusTask(orderId, newStatus, callback).execute();
    }

    private static class LoadOrderItemsTask extends AsyncTask<Void, Void, List<CartItem>> {
        private final long orderId;
        private final LoadOrderItemsCallback callback;
        private String error;
        private String orderStatus;

        LoadOrderItemsTask(long orderId, LoadOrderItemsCallback callback) {
            this.orderId = orderId;
            this.callback = callback;
        }

        @Override
        protected List<CartItem> doInBackground(Void... voids) {
            List<CartItem> items = new ArrayList<>();
            try {
                String orderFilter = "id=eq." + orderId + "&select=status";
                Log.d("LoadOrderItemsTask", "Fetching order status for orderId: " + orderId);
                Connection.Response orderResponse = Jsoup.connect(ORDERS_URL + "?" + orderFilter)
                        .header("Authorization", TOKEN)
                        .header("apikey", SECRET)
                        .ignoreContentType(true)
                        .method(Connection.Method.GET)
                        .execute();
                JSONArray orderArray = new JSONArray(orderResponse.body());
                Log.d("LoadOrderItemsTask", "Order response: " + orderResponse.body());
                if (orderArray.length() == 0) {
                    error = "Заказ не найден";
                    Log.e("LoadOrderItemsTask", error);
                    return null;
                }
                orderStatus = orderArray.getJSONObject(0).getString("status");
                Log.d("LoadOrderItemsTask", "Order status: " + orderStatus);
                String filter = "order_id=eq." + orderId + "&select=count,product_size_id";
                Log.d("LoadOrderItemsTask", "Fetching order items for orderId: " + orderId);
                Connection.Response response = Jsoup.connect(ORDER_ITEMS_URL + "?" + filter)
                        .header("Authorization", TOKEN)
                        .header("apikey", SECRET)
                        .ignoreContentType(true)
                        .method(Connection.Method.GET)
                        .execute();
                JSONArray itemsArray = new JSONArray(response.body());
                Log.d("LoadOrderItemsTask", "Order items response: " + response.body() + ", items count: " + itemsArray.length());
                for (int i = 0; i < itemsArray.length(); i++) {
                    JSONObject itemObj = itemsArray.getJSONObject(i);
                    int count = itemObj.getInt("count");
                    int productSizeId = itemObj.getInt("product_size_id");
                    Log.d("LoadOrderItemsTask", "Processing item: count=" + count + ", productSizeId=" + productSizeId);
                    String productSizeUrl = PRODUCT_SIZE_URL + "?id=eq." + productSizeId + "&select=id,product_id,size_id,count";
                    Connection.Response productSizeResponse = Jsoup.connect(productSizeUrl)
                            .header("Authorization", TOKEN)
                            .header("apikey", SECRET)
                            .ignoreContentType(true)
                            .method(Connection.Method.GET)
                            .execute();
                    JSONArray productSizeArray = new JSONArray(productSizeResponse.body());
                    Log.d("LoadOrderItemsTask", "Product size response: " + productSizeResponse.body());
                    if (productSizeArray.length() == 0) {
                        Log.w("LoadOrderItemsTask", "No product size found for productSizeId: " + productSizeId);
                        continue;
                    }
                    JSONObject productSizeObj = productSizeArray.getJSONObject(0);
                    int productId = productSizeObj.getInt("product_id");
                    int sizeId = productSizeObj.getInt("size_id");
                    int availableQuantity = productSizeObj.getInt("count");
                    String productUrl = PRODUCTS_URL + "?id=eq." + productId + "&select=id,name,price,description,category_id";
                    Connection.Response productResponse = Jsoup.connect(productUrl)
                            .header("Authorization", TOKEN)
                            .header("apikey", SECRET)
                            .ignoreContentType(true)
                            .method(Connection.Method.GET)
                            .execute();
                    JSONArray productArray = new JSONArray(productResponse.body());
                    Log.d("LoadOrderItemsTask", "Product response: " + productResponse.body());
                    Product product = null;
                    if (productArray.length() > 0) {
                        JSONObject productObj = productArray.getJSONObject(0);
                        product = new Product(
                                productObj.getInt("id"),
                                productObj.getString("name"),
                                productObj.getDouble("price"),
                                productObj.getString("description"),
                                productObj.getInt("category_id")
                        );
                    } else {
                        Log.w("LoadOrderItemsTask", "No product found for productId: " + productId);
                        continue;
                    }
                    String sizeUrl = SIZES_URL + "?id=eq." + sizeId + "&select=value";
                    Connection.Response sizeResponse = Jsoup.connect(sizeUrl)
                            .header("Authorization", TOKEN)
                            .header("apikey", SECRET)
                            .ignoreContentType(true)
                            .method(Connection.Method.GET)
                            .execute();
                    JSONArray sizeArray = new JSONArray(sizeResponse.body());
                    Log.d("LoadOrderItemsTask", "Size response: " + sizeResponse.body());
                    String sizeValue = "Unknown";
                    if (sizeArray.length() > 0) {
                        sizeValue = sizeArray.getJSONObject(0).getString("value");
                    }
                    items.add(new CartItem(null, product, count, sizeValue, availableQuantity, productSizeId));
                    Log.d("LoadOrderItemsTask", "Added item: product=" + product.getName() + ", count=" + count + ", size=" + sizeValue);
                }
                Log.d("LoadOrderItemsTask", "Total items loaded: " + items.size());
                return items;
            } catch (Exception e) {
                error = "Error: " + e.getMessage();
                Log.e("OrderError", error, e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(List<CartItem> items) {
            Log.d("LoadOrderItemsTask", "onPostExecute: items=" + (items != null ? items.size() : "null") + ", error=" + error);
            if (error != null || items == null) {
                callback.onError(error != null ? error : "Не удалось загрузить элементы заказа");
            } else {
                callback.onSuccess(items, orderStatus);
            }
        }
    }

    private static class UpdateOrderStatusTask extends AsyncTask<Void, Void, Boolean> {
        private final long orderId;
        private final String newStatus;
        private final UpdateOrderCallback callback;
        private String error;

        UpdateOrderStatusTask(long orderId, String newStatus, UpdateOrderCallback callback) {
            this.orderId = orderId;
            this.newStatus = newStatus;
            this.callback = callback;
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                JSONObject updateJson = new JSONObject();
                updateJson.put("status", newStatus);
                Connection.Response response = Jsoup.connect(ORDERS_URL + "?id=eq." + orderId)
                        .header("Authorization", TOKEN)
                        .header("apikey", SECRET)
                        .header("Content-Type", "application/json")
                        .header("Prefer", "return=minimal")
                        .requestBody(updateJson.toString())
                        .ignoreContentType(true)
                        .method(Connection.Method.PATCH)
                        .execute();
                return response.statusCode() == 204 || response.statusCode() == 200;
            } catch (Exception e) {
                error = "Error: " + e.getMessage();
                Log.e("OrderError", error, e);
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (error != null || !success) {
                callback.onError(error != null ? error : "Не удалось обновить статус заказа");
            } else {
                callback.onSuccess();
            }
        }
    }

    public static void loadStoreOrdersInProgress(long storeId, LoadOrdersCallback callback) {
        new LoadStoreOrdersInProgressTask(storeId, callback).execute();
    }

    private static class LoadStoreOrdersInProgressTask extends AsyncTask<Void, Void, List<Order>> {
        private final long storeId;
        private final LoadOrdersCallback callback;
        private String error;

        LoadStoreOrdersInProgressTask(long storeId, LoadOrdersCallback callback) {
            this.storeId = storeId;
            this.callback = callback;
        }

        @Override
        protected List<Order> doInBackground(Void... voids) {
            List<Order> orders = new ArrayList<>();
            try {
                String filter = "store_id=eq." + storeId +
                        "&status=eq.В сборке" +
                        "&select=*,order_items(count,product_size_id)";
                Connection.Response response = Jsoup.connect(ORDERS_URL + "?" + filter)
                        .header("Authorization", TOKEN)
                        .header("apikey", SECRET)
                        .ignoreContentType(true)
                        .method(Connection.Method.GET)
                        .execute();
                JSONArray ordersArray = new JSONArray(response.body());
                for (int i = 0; i < ordersArray.length(); i++) {
                    JSONObject orderJson = ordersArray.getJSONObject(i);
                    long id = orderJson.getLong("id");
                    String date = orderJson.getString("order_date");
                    String status = orderJson.getString("status");
                    double totalPrice = orderJson.getDouble("total_price");
                    int itemCount = 0;
                    if (orderJson.has("order_items")) {
                        JSONArray itemsArray = orderJson.getJSONArray("order_items");
                        for (int j = 0; j < itemsArray.length(); j++) {
                            itemCount += itemsArray.getJSONObject(j).getInt("count");
                        }
                    }
                    orders.add(new Order(id, date, status, totalPrice, itemCount));
                }
                return orders;
            } catch (Exception e) {
                error = "Error: " + e.getMessage();
                Log.e("OrderError", error, e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(List<Order> orders) {
            if (error != null || orders == null) {
                callback.onError(error != null ? error : "Не удалось загрузить заказы в сборке");
            } else {
                callback.onSuccess(orders);
            }
        }
    }

    public static void loadStoreOrdersByStatus(long storeId, String status, LoadOrdersCallback callback) {
        new LoadStoreOrdersByStatusTask(storeId, status, callback).execute();
    }

    private static class LoadStoreOrdersByStatusTask extends AsyncTask<Void, Void, List<Order>> {
        private final long storeId;
        private final String status;
        private final LoadOrdersCallback callback;
        private String error;

        LoadStoreOrdersByStatusTask(long storeId, String status, LoadOrdersCallback callback) {
            this.storeId = storeId;
            this.status = status;
            this.callback = callback;
        }

        @Override
        protected List<Order> doInBackground(Void... voids) {
            List<Order> orders = new ArrayList<>();
            try {
                String filter = "store_id=eq." + storeId +
                        "&status=eq." + URLEncoder.encode(status, "UTF-8") +
                        "&select=*,order_items(count,product_size_id)";
                Connection.Response response = Jsoup.connect(ORDERS_URL + "?" + filter)
                        .header("Authorization", TOKEN)
                        .header("apikey", SECRET)
                        .ignoreContentType(true)
                        .method(Connection.Method.GET)
                        .execute();
                JSONArray ordersArray = new JSONArray(response.body());
                for (int i = 0; i < ordersArray.length(); i++) {
                    JSONObject orderJson = ordersArray.getJSONObject(i);
                    long id = orderJson.getLong("id");
                    String date = orderJson.getString("order_date");
                    String status = orderJson.getString("status");
                    double totalPrice = orderJson.getDouble("total_price");
                    int itemCount = 0;
                    if (orderJson.has("order_items")) {
                        JSONArray itemsArray = orderJson.getJSONArray("order_items");
                        for (int j = 0; j < itemsArray.length(); j++) {
                            itemCount += itemsArray.getJSONObject(j).getInt("count");
                        }
                    }
                    orders.add(new Order(id, date, status, totalPrice, itemCount));
                }
                return orders;
            } catch (Exception e) {
                error = "Error: " + e.getMessage();
                Log.e("OrderError", error, e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(List<Order> orders) {
            if (error != null || orders == null) {
                callback.onError(error != null ? error : "Не удалось загрузить заказы");
            } else {
                callback.onSuccess(orders);
            }
        }
    }
}
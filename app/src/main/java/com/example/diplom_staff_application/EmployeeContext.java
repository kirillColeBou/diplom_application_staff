package com.example.diplom_staff_application;

import android.os.AsyncTask;
import android.util.Log;
import org.json.JSONArray;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import java.net.URLEncoder;

public class EmployeeContext {
    public static final String URL = "https://mgxymxiehfsptuubuqfv.supabase.co/rest/v1/employees";
    public static final String TOKEN = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im1neHlteGllaGZzcHR1dWJ1cWZ2Iiwicm9sZSI6InNlcnZpY2Vfcm9sZSIsImlhdCI6MTc0NTIyNzY0NSwiZXhwIjoyMDYwODAzNjQ1fQ.LNqLc1o8I8eZUxYuFXknXZZhzN5kRh0eggmg5tItiM0";
    public static final String SECRET = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im1neHlteGllaGZzcHR1dWJ1cWZ2Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDUyMjc2NDUsImV4cCI6MjA2MDgwMzY0NX0.QXcy5Dpd4_b58-xfpvPAIgm9U8Pj6w62RW6p7NDUKyQ";

    public interface Callback {
        void onSuccess(boolean employeeExists);
        void onError(String error);
    }

    public static void checkEmployeeCredentials(String loginOrEmailOrPhone, String password, Callback callback) {
        new CheckEmployeeTask(loginOrEmailOrPhone, password, callback).execute();
    }

    private static class CheckEmployeeTask extends AsyncTask<Void, Void, Boolean> {
        private final String loginOrEmailOrPhone;
        private final String password;
        private final Callback callback;
        private String error;

        CheckEmployeeTask(String loginOrEmailOrPhone, String password, Callback callback) {
            this.loginOrEmailOrPhone = loginOrEmailOrPhone;
            this.password = password;
            this.callback = callback;
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                String url = URL + "?or=" +
                        URLEncoder.encode("(login.eq." + loginOrEmailOrPhone +
                                ",email.eq." + loginOrEmailOrPhone +
                                ",phone_number.eq." + loginOrEmailOrPhone + ")", "UTF-8") +
                        "&password=eq." + URLEncoder.encode(password, "UTF-8");
                Document doc = Jsoup.connect(url)
                        .header("Authorization", TOKEN)
                        .header("apikey", SECRET)
                        .ignoreContentType(true)
                        .get();
                String response = doc.body().text();
                Log.d("Supabase", "Response: " + response);
                return new JSONArray(response).length() > 0;
            } catch (Exception e) {
                error = "Error: " + e.getMessage();
                Log.e("Supabase", "Request failed: " + error);
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean employeeExists) {
            if (error != null) {
                callback.onError(error);
            } else {
                callback.onSuccess(employeeExists);
            }
        }
    }

    public interface EmployeeInfoCallback {
        void onSuccess(long employeeId, long storeId);
        void onError(String error);
    }

    private static class EmployeeInfo {
        long id;
        long storeId;

        EmployeeInfo(long id, long storeId) {
            this.id = id;
            this.storeId = storeId;
        }
    }

    public static void getEmployeeInfo(String loginOrEmailOrPhone, final EmployeeInfoCallback callback) {
        new AsyncTask<Void, Void, EmployeeInfo>() {
            private String error;

            @Override
            protected EmployeeInfo doInBackground(Void... voids) {
                try {
                    String url = URL + "?or=" +
                            URLEncoder.encode("(login.eq." + loginOrEmailOrPhone +
                                    ",email.eq." + loginOrEmailOrPhone +
                                    ",phone_number.eq." + loginOrEmailOrPhone + ")") +
                            "&select=id,store_id";
                    Document doc = Jsoup.connect(url)
                            .header("Authorization", TOKEN)
                            .header("apikey", SECRET)
                            .ignoreContentType(true)
                            .get();
                    JSONArray jsonArray = new JSONArray(doc.body().text());
                    if (jsonArray.length() > 0) {
                        return new EmployeeInfo(
                                jsonArray.getJSONObject(0).getLong("id"),
                                jsonArray.getJSONObject(0).getLong("store_id")
                        );
                    }
                    return null;
                } catch (Exception e) {
                    error = e.getMessage();
                    return null;
                }
            }

            @Override
            protected void onPostExecute(EmployeeInfo employeeInfo) {
                if (error != null) {
                    callback.onError(error);
                } else if (employeeInfo != null) {
                    callback.onSuccess(employeeInfo.id, employeeInfo.storeId);
                } else {
                    callback.onError("Employee not found");
                }
            }
        }.execute();
    }
}
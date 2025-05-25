package com.example.diplom_staff_application;

import android.content.Context;
import android.content.SharedPreferences;

public class AuthUtils {
    private static final String PREFS_NAME = "employee_auth_prefs";
    private static final String KEY_LOGIN = "login";
    private static final String KEY_PASSWORD_HASH = "password_hash";
    private static final String KEY_EMPLOYEE_ID = "employee_id";
    private static final String KEY_STORE_ID = "store_id";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";

    public static void saveEmployeeCredentials(Context context, String login, String passwordHash,
                                               long employeeId, long storeId) {
        SharedPreferences sharedPref = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(KEY_LOGIN, login);
        editor.putString(KEY_PASSWORD_HASH, passwordHash);
        editor.putLong(KEY_EMPLOYEE_ID, employeeId);
        editor.putLong(KEY_STORE_ID, storeId);
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.apply();
    }

    public static boolean isEmployeeLoggedIn(Context context) {
        SharedPreferences sharedPref = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return sharedPref.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public static String[] getSavedCredentials(Context context) {
        SharedPreferences sharedPref = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String login = sharedPref.getString(KEY_LOGIN, null);
        String passwordHash = sharedPref.getString(KEY_PASSWORD_HASH, null);
        return (login != null && passwordHash != null) ?
                new String[]{login, passwordHash} : null;
    }

    public static long getCurrentStoreId(Context context) {
        SharedPreferences sharedPref = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return sharedPref.getLong(KEY_STORE_ID, -1);
    }

    public static void logout(Context context) {
        SharedPreferences sharedPref = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.clear();
        editor.apply();
    }
}
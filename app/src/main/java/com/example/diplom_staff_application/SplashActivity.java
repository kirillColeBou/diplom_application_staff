package com.example.diplom_staff_application;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (AuthUtils.isEmployeeLoggedIn(this)) {
            String[] credentials = AuthUtils.getSavedCredentials(this);
            if (credentials != null) {
                EmployeeContext.checkEmployeeCredentials(credentials[0], credentials[1],
                        new EmployeeContext.Callback() {
                            @Override
                            public void onSuccess(boolean employeeExists) {
                                runOnUiThread(() -> {
                                    if (employeeExists) {
                                        startActivity(new Intent(SplashActivity.this, MainActivity.class));
                                        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                                        finish();
                                    } else {
                                        AuthUtils.logout(SplashActivity.this);
                                        goToLogin();
                                    }
                                });
                            }

                            @Override
                            public void onError(String error) {
                                runOnUiThread(() -> {
                                    goToLogin();
                                });
                            }
                        });
                return;
            }
        }
        goToLogin();
    }

    private void goToLogin() {
        startActivity(new Intent(this, LoginActivity.class));
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        finish();
    }
}
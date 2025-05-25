package com.example.diplom_staff_application;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class LoginActivity extends AppCompatActivity {
    private EditText logins;
    private EditText passwords;
    private ImageView eyeIcon;
    private boolean isPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.login_activity);
        logins = findViewById(R.id.logins);
        passwords = findViewById(R.id.password);
        eyeIcon = findViewById(R.id.eyeIcon);
        eyeIcon.setOnClickListener(v -> togglePasswordVisibility());
    }

    private void togglePasswordVisibility() {
        if (isPasswordVisible) {
            passwords.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            eyeIcon.setImageResource(R.drawable.eye);
        } else {
            passwords.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            eyeIcon.setImageResource(R.drawable.eye_open);
        }
        passwords.setSelection(passwords.getText().length());
        isPasswordVisible = !isPasswordVisible;
    }

    public void onAuthorization(View view) {
        String loginOrEmailOrPhone = logins.getText().toString().trim();
        String password = passwords.getText().toString().trim();
        if (loginOrEmailOrPhone.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show();
            return;
        }
        String hashedPassword = md5(password);
        EmployeeContext.checkEmployeeCredentials(loginOrEmailOrPhone, hashedPassword, new EmployeeContext.Callback() {
            @Override
            public void onSuccess(boolean employeeExists) {
                runOnUiThread(() -> {
                    if (employeeExists) {
                        EmployeeContext.getEmployeeInfo(loginOrEmailOrPhone, new EmployeeContext.EmployeeInfoCallback() {
                            @Override
                            public void onSuccess(long employeeId, long storeId) {
                                AuthUtils.saveEmployeeCredentials(LoginActivity.this,
                                        loginOrEmailOrPhone,
                                        hashedPassword,
                                        employeeId,
                                        storeId);
                                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                                finish();
                            }

                            @Override
                            public void onError(String error) {
                                Toast.makeText(LoginActivity.this,
                                        "Ошибка получения данных сотрудника: " + error,
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        Toast.makeText(LoginActivity.this,
                                "Неверные учетные данные", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() ->
                        Toast.makeText(LoginActivity.this,
                                "Ошибка соединения: " + error, Toast.LENGTH_SHORT).show());
            }
        });
    }

    private String md5(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] hash = digest.digest(input.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm not found", e);
        }
    }

    public void onRestorePassword(View view){
        startActivity(new Intent(LoginActivity.this, RestorePasswordActivity.class));
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    public void onRegister(View view){
        startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }
}
package com.example.diplom_staff_application;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class RegisterActivity extends AppCompatActivity {
    EditText logins, emails, phone_numbers, passwords;
    private TextView addressStore;
    private int selectedStoreId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.register_activity);
        logins = findViewById(R.id.login);
        emails = findViewById(R.id.email);
        phone_numbers = findViewById(R.id.phone_number);
        passwords = findViewById(R.id.password);
        addressStore = findViewById(R.id.address_store);
    }

    public void onRegistration(View view) {
        String login = logins.getText().toString().trim();
        String email = emails.getText().toString().trim();
        String phone_number = phone_numbers.getText().toString().trim();
        String password = passwords.getText().toString().trim();
        if (validateInputs(login, email, phone_number, password)) {
            checkEmployeeAndProceed(login, email, phone_number, password);
        }
    }

    private boolean validateInputs(String login, String email, String phone, String password) {
        if (login.isEmpty() || email.isEmpty() || phone.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (login.length() < 4) {
            logins.setError("Логин должен содержать минимум 4 символа");
            logins.requestFocus();
            return false;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emails.setError("Введите корректный email");
            emails.requestFocus();
            return false;
        }
        String cleanedPhone = phone.replaceAll("[^0-9]", "");
        if (cleanedPhone.length() < 10) {
            phone_numbers.setError("Введите корректный номер телефона");
            phone_numbers.requestFocus();
            return false;
        }
        if (password.length() < 6) {
            passwords.setError("Пароль должен содержать минимум 6 символов");
            passwords.requestFocus();
            return false;
        }
        if (selectedStoreId == -1) {
            Toast.makeText(this, "Выберите магазин", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void checkEmployeeAndProceed(String login, String email, String phone, String password) {
        RegisterContext.checkEmployeeExists(login, email, phone, new RegisterContext.Callback() {
            @Override
            public void onSuccess(boolean isAvailable) {
                if (isAvailable) {
                    proceedToVerification(login, email, phone, password);
                } else {
                    showEmployeeExistsError();
                }
            }

            @Override
            public void onError(String error) {
                showRegistrationError(error);
            }
        });
    }

    private void proceedToVerification(String login, String email, String phone, String password) {
        Intent intent = new Intent(RegisterActivity.this, VerificationActivity.class);
        intent.putExtra("login", login);
        intent.putExtra("email", email);
        intent.putExtra("phone", phone);
        intent.putExtra("password", password);
        intent.putExtra("store_id", selectedStoreId);
        Log.d("store", "" + selectedStoreId);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        finish();
    }

    private void showEmployeeExistsError() {
        runOnUiThread(() ->
                Toast.makeText(RegisterActivity.this,
                        "Сотрудник с такими данными уже существует!",
                        Toast.LENGTH_SHORT).show());
    }

    private void showRegistrationError(String error) {
        runOnUiThread(() ->
                Toast.makeText(RegisterActivity.this,
                        "Ошибка: " + error, Toast.LENGTH_SHORT).show());
    }

    public void onBack(View view) {
        startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        finish();
    }

    public void onMap(View view) {
        startActivityForResult(new Intent(this, MapActivity.class), 1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK) {
            String storeAddress = data.getStringExtra("store_address");
            selectedStoreId = data.getIntExtra("store_id", -1);
            addressStore.setText(storeAddress);
        }
    }
}
package com.example.diplom_staff_application;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.Random;

public class VerificationActivity extends AppCompatActivity {
    private EditText[] codeInputs = new EditText[6];
    private TextView timerText;
    private TextView resendText;
    private TextView submitButton;
    private LinearLayout llTimer, llNewCode;
    private String generatedCode;
    private CountDownTimer countDownTimer;
    private boolean isTimerRunning = false;
    private boolean isErrorState = false;
    private String email, phone, password, login;
    private int storeId;
    private String mode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.verification_activity);
        initializeViews();
        getIntentData();
        setupCodeVerification();
    }

    private void getIntentData() {
        Intent intent = getIntent();
        login = intent.getStringExtra("login");
        email = intent.getStringExtra("email");
        phone = intent.getStringExtra("phone");
        password = intent.getStringExtra("password");
        storeId = intent.getIntExtra("store_id", -1);
        mode = intent.getStringExtra("mode");
    }

    private void proceedToPasswordReset() {
        Intent intent = new Intent(this, ForgotPasswordActivity.class);
        intent.putExtra("email", email);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        finish();
    }

    private void initializeViews() {
        timerText = findViewById(R.id.timer_text);
        resendText = findViewById(R.id.resend_text);
        submitButton = findViewById(R.id.submit_button);
        llTimer = findViewById(R.id.ll_timer);
        llNewCode = findViewById(R.id.ll_newcode);
        codeInputs[0] = findViewById(R.id.code_1);
        codeInputs[1] = findViewById(R.id.code_2);
        codeInputs[2] = findViewById(R.id.code_3);
        codeInputs[3] = findViewById(R.id.code_4);
        codeInputs[4] = findViewById(R.id.code_5);
        codeInputs[5] = findViewById(R.id.code_6);
    }

    private void setupCodeVerification() {
        generatedCode = generateRandomCode();
        sendVerificationCode(generatedCode);
        setupCodeInputListeners();
        startCountdownTimer(60000);
    }

    private String generateRandomCode() {
        return String.format("%06d", new Random().nextInt(999999));
    }

    private void sendVerificationCode(String code) {
        String subject = "Код подтверждения для сотрудника Sneaker Shop";
        String message = "Ваш код подтверждения: " + code + "\n\nВведите его в приложении.";
        new YandexMailSender().execute(
                email,
                subject,
                message
        );
    }

    private void setupCodeInputListeners() {
        for (int i = 0; i < codeInputs.length; i++) {
            final int currentIndex = i;
            codeInputs[i].addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void afterTextChanged(Editable s) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    handleCodeInputChange(currentIndex, s);
                }
            });
            codeInputs[i].setOnFocusChangeListener((v, hasFocus) -> {
                EditText et = (EditText)v;
                if (hasFocus && et.isEnabled()) {
                    et.setBackgroundResource(R.drawable.background_edittext_verification_select);
                } else {
                    et.setBackgroundResource(
                            isErrorState && et != codeInputs[0] ? R.drawable.background_edittext_verification_error
                                    : R.drawable.background_edittext_verification
                    );
                }
            });
        }
        resendText.setOnClickListener(v -> handleResendCode());
        submitButton.setOnClickListener(v -> {
            generatedCode = generateRandomCode();
            sendVerificationCode(generatedCode);
            resetAllInputs();
            startCountdownTimer(60000);
        });
    }

    private void handleCodeInputChange(int currentIndex, CharSequence s) {
        if (isErrorState) {
            clearErrorState();
        }
        if (s.length() == 1 && currentIndex < codeInputs.length - 1) {
            codeInputs[currentIndex].clearFocus();
            codeInputs[currentIndex + 1].setEnabled(true);
            codeInputs[currentIndex + 1].requestFocus();
        }
        if (isAllFieldsFilled()) {
            verifyCode();
        }
    }

    private boolean isAllFieldsFilled() {
        for (EditText input : codeInputs) {
            if (input.getText().toString().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private void verifyCode() {
        StringBuilder enteredCode = new StringBuilder();
        for (EditText input : codeInputs) {
            enteredCode.append(input.getText().toString());
        }
        if (enteredCode.toString().equals(generatedCode)) {
            if ("password_reset".equals(mode)) {
                proceedToPasswordReset();
            } else {
                registerEmployee();
            }
        } else {
            showCodeError();
        }
    }

    @SuppressLint("StaticFieldLeak")
    private void registerEmployee() {
        new AsyncTask<Void, Void, Boolean>() {
            private String error;
            private long employeeId;

            @Override
            protected Boolean doInBackground(Void... voids) {
                try {
                    if (storeId == -1) {
                        error = "Не выбран магазин";
                        return false;
                    }
                    String jsonBody = String.format(
                            "{\"login\":\"%s\",\"email\":\"%s\",\"phone_number\":\"%s\",\"password\":\"%s\",\"store_id\":%d}",
                            login, email, phone, md5(password), storeId
                    );
                    Document doc = Jsoup.connect(RegisterContext.URL)
                            .header("Authorization", RegisterContext.TOKEN)
                            .header("apikey", RegisterContext.SECRET)
                            .header("Content-Type", "application/json")
                            .header("Prefer", "return=minimal")
                            .requestBody(jsonBody)
                            .ignoreContentType(true)
                            .post();
                    String getEmployeeUrl = RegisterContext.URL + "?login=eq." + login + "&select=id,store_id";
                    Document employeeDoc = Jsoup.connect(getEmployeeUrl)
                            .header("Authorization", RegisterContext.TOKEN)
                            .header("apikey", RegisterContext.SECRET)
                            .ignoreContentType(true)
                            .get();
                    JSONArray jsonArray = new JSONArray(employeeDoc.body().text());
                    if (jsonArray.length() > 0) {
                        employeeId = jsonArray.getJSONObject(0).getLong("id");
                        return true;
                    }
                    return false;
                } catch (Exception e) {
                    error = e.getMessage();
                    return false;
                }
            }

            @Override
            protected void onPostExecute(Boolean success) {
                if (success) {
                    AuthUtils.saveEmployeeCredentials(VerificationActivity.this,
                            login, md5(password), employeeId, storeId);
                    proceedToMain();
                } else {
                    showRegistrationError(error != null ? error : "Ошибка регистрации");
                }
            }
        }.execute();
    }

    private String md5(String input) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] array = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : array) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 error", e);
        }
    }

    private void proceedToMain() {
        startActivity(new Intent(this, MainActivity.class));
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        finish();
    }

    private void showCodeError() {
        isErrorState = true;
        runOnUiThread(() -> {
            for (EditText input : codeInputs) {
                input.setText("");
                input.setEnabled(false);
            }
            codeInputs[0].setBackgroundResource(R.drawable.background_edittext_verification_select);
            for (int i = 1; i < codeInputs.length; i++) {
                codeInputs[i].setBackgroundResource(R.drawable.background_edittext_verification_error);
            }
            codeInputs[0].setEnabled(true);
            codeInputs[0].requestFocus();
        });
    }

    private void handleResendCode() {
        if (!isTimerRunning) {
            generatedCode = generateRandomCode();
            sendVerificationCode(generatedCode);
            resetAllInputs();
            startCountdownTimer(60000);
        }
    }

    private void clearErrorState() {
        isErrorState = false;
        runOnUiThread(() -> {
            for (EditText input : codeInputs) {
                input.setBackgroundResource(R.drawable.background_edittext_verification);
            }
        });
    }

    private void resetAllInputs() {
        runOnUiThread(() -> {
            isErrorState = false;
            for (EditText input : codeInputs) {
                input.setText("");
                input.setBackgroundResource(R.drawable.background_edittext_verification);
                input.setEnabled(false);
            }
            codeInputs[0].setEnabled(true);
            codeInputs[0].requestFocus();
        });
    }

    private void startCountdownTimer(long millisInFuture) {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        runOnUiThread(() -> {
            llTimer.setVisibility(View.VISIBLE);
            llNewCode.setVisibility(View.GONE);
            resendText.setEnabled(false);
            isTimerRunning = true;
            countDownTimer = new CountDownTimer(millisInFuture, 1000) {
                public void onTick(long millisUntilFinished) {
                    long seconds = millisUntilFinished / 1000;
                    timerText.setText(String.format("%02d:%02d", seconds / 60, seconds % 60));
                }

                public void onFinish() {
                    runOnUiThread(() -> {
                        timerText.setText("00:00");
                        llTimer.setVisibility(View.GONE);
                        llNewCode.setVisibility(View.VISIBLE);
                        isTimerRunning = false;
                    });
                }
            }.start();
        });
    }

    private void showRegistrationError(String error) {
        Toast.makeText(this, "Ошибка регистрации: " + error, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }

    public void onBack(View view){
        if("password_reset".equals(mode)){
            startActivity(new Intent(this, RestorePasswordActivity.class));
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
            finish();
        }
        else{
            startActivity(new Intent(this, RegisterActivity.class));
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
            finish();
        }
    }
}
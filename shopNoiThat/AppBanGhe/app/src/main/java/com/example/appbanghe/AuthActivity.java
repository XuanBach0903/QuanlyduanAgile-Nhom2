package com.example.appbanghe;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.appbanghe.network.ApiClient;
import com.example.appbanghe.network.SessionManager;
import com.example.appbanghe.network.dto.LoginRequest;
import com.example.appbanghe.network.dto.RegisterRequest;
import com.example.appbanghe.network.services.AccountService;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AuthActivity extends AppCompatActivity {

    private static final String REMEMBER_PREFS = "remember_login";
    private static final String KEY_REMEMBER = "remember";
    private static final String KEY_REMEMBER_EMAIL = "email";
    private static final String KEY_REMEMBER_PASSWORD = "password";

    private View tabLogin;
    private View tabRegister;
    private TextView tabLoginText;
    private TextView tabRegisterText;

    private View fullNameContainer;
    private TextInputEditText fullNameInput;

    private View phoneContainer;
    private TextInputEditText phoneInput;

    private TextInputEditText emailInput;
    private TextInputEditText passwordInput;

    private View rememberContainer;
    private CheckBox rememberCheckBox;

    private Button primaryButton;

    private boolean isLogin = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_auth);

        tabLogin = findViewById(R.id.tab_login);
        tabRegister = findViewById(R.id.tab_register);
        tabLoginText = findViewById(R.id.tab_login_text);
        tabRegisterText = findViewById(R.id.tab_register_text);

        fullNameContainer = findViewById(R.id.fullname_container);
        fullNameInput = findViewById(R.id.input_fullname);

        phoneContainer = findViewById(R.id.phone_container);
        phoneInput = findViewById(R.id.input_phone);

        emailInput = findViewById(R.id.input_email);
        passwordInput = findViewById(R.id.input_password);

        rememberContainer = findViewById(R.id.remember_container);
        rememberCheckBox = findViewById(R.id.checkbox_remember);

        restoreRememberedCredentials();

        primaryButton = findViewById(R.id.primary_button);

        tabLogin.setOnClickListener(v -> setMode(true));
        tabRegister.setOnClickListener(v -> setMode(false));

        primaryButton.setOnClickListener(v -> {
            Toast.makeText(AuthActivity.this, isLogin ? "Đang đăng nhập..." : "Đang đăng ký...", Toast.LENGTH_SHORT).show();
            if (isLogin) {
                if (!validateLogin()) {
                    Toast.makeText(AuthActivity.this, "Vui lòng kiểm tra lại email/mật khẩu", Toast.LENGTH_SHORT).show();
                    return;
                }

                doLogin();
            } else {
                if (!validateRegister()) {
                    Toast.makeText(AuthActivity.this, "Vui lòng kiểm tra lại thông tin đăng ký", Toast.LENGTH_SHORT).show();
                    return;
                }

                doRegister();
            }
        });

        setMode(true);
    }

    private void setMode(boolean login) {
        isLogin = login;

        tabLogin.setSelected(login);
        tabRegister.setSelected(!login);

        tabLoginText.setSelected(login);
        tabRegisterText.setSelected(!login);

        fullNameContainer.setVisibility(login ? View.GONE : View.VISIBLE);
        phoneContainer.setVisibility(login ? View.GONE : View.VISIBLE);
        if (rememberContainer != null) {
            rememberContainer.setVisibility(login ? View.VISIBLE : View.GONE);
        }
        primaryButton.setText(login ? "Đăng Nhập" : "Đăng Ký");

        if (login) {
            if (fullNameInput != null) {
                fullNameInput.setText(null);
            }
            if (phoneInput != null) {
                phoneInput.setText(null);
            }
        }
    }

    private boolean validateLogin() {
        clearErrors();

        boolean ok = true;

        String email = emailInput == null ? "" : String.valueOf(emailInput.getText()).trim();
        String password = passwordInput == null ? "" : String.valueOf(passwordInput.getText());

        if (!isValidEmail(email)) {
            if (emailInput != null) {
                emailInput.setError("Email không hợp lệ");
                emailInput.requestFocus();
            }
            ok = false;
        }

        if (password == null || password.length() < 3) {
            if (passwordInput != null) {
                passwordInput.setError("Mật khẩu phải có ít nhất 3 ký tự");
                if (ok) {
                    passwordInput.requestFocus();
                }
            }
            ok = false;
        }

        return ok;
    }

    private boolean validateRegister() {
        clearErrors();

        boolean ok = true;

        String fullName = fullNameInput == null ? "" : String.valueOf(fullNameInput.getText()).trim();
        String phone = phoneInput == null ? "" : String.valueOf(phoneInput.getText()).trim();
        String email = emailInput == null ? "" : String.valueOf(emailInput.getText()).trim();
        String password = passwordInput == null ? "" : String.valueOf(passwordInput.getText());

        if (fullName.length() < 2) {
            if (fullNameInput != null) {
                fullNameInput.setError("Tên phải có ít nhất 2 ký tự");
                fullNameInput.requestFocus();
            }
            ok = false;
        }

        if (!isValidEmail(email)) {
            if (emailInput != null) {
                emailInput.setError("Email không hợp lệ");
                if (ok) {
                    emailInput.requestFocus();
                }
            }
            ok = false;
        }

        if (phone.isEmpty()) {
            if (phoneInput != null) {
                phoneInput.setError("Vui lòng nhập số điện thoại");
                if (ok) {
                    phoneInput.requestFocus();
                }
            }
            ok = false;
        }

        if (password == null || password.length() < 3) {
            if (passwordInput != null) {
                passwordInput.setError("Mật khẩu phải có ít nhất 3 ký tự");
                if (ok) {
                    passwordInput.requestFocus();
                }
            }
            ok = false;
        }

        return ok;
    }

    private void clearErrors() {
        if (fullNameInput != null) {
            fullNameInput.setError(null);
        }
        if (phoneInput != null) {
            phoneInput.setError(null);
        }
        if (emailInput != null) {
            emailInput.setError(null);
        }
        if (passwordInput != null) {
            passwordInput.setError(null);
        }
    }

    private void doRegister() {
        String fullName = fullNameInput == null ? "" : String.valueOf(fullNameInput.getText()).trim();
        String phone = phoneInput == null ? "" : String.valueOf(phoneInput.getText()).trim();
        String email = emailInput == null ? "" : String.valueOf(emailInput.getText()).trim();
        String password = passwordInput == null ? "" : String.valueOf(passwordInput.getText());

        AccountService service = ApiClient.createService(this, AccountService.class);
        RegisterRequest body = new RegisterRequest(fullName, email, phone, password);
        service.dangKy(body).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (!response.isSuccessful()) {
                    Toast.makeText(AuthActivity.this, "Đăng ký thất bại: HTTP " + response.code(), Toast.LENGTH_SHORT).show();
                    return;
                }

                Toast.makeText(AuthActivity.this, "Đăng ký thành công", Toast.LENGTH_SHORT).show();
                if (passwordInput != null) {
                    passwordInput.setText(null);
                }
                setMode(true);
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Toast.makeText(AuthActivity.this, "Đăng ký lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void doLogin() {
        String email = emailInput == null ? "" : String.valueOf(emailInput.getText()).trim();
        String password = passwordInput == null ? "" : String.valueOf(passwordInput.getText());

        AccountService service = ApiClient.createService(this, AccountService.class);
        LoginRequest body = new LoginRequest(email, password);
        service.dangNhap(body).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (!response.isSuccessful()) {
                    Toast.makeText(AuthActivity.this, "Đăng nhập thất bại: HTTP " + response.code(), Toast.LENGTH_SHORT).show();
                    return;
                }

                JsonObject body = response.body();
                String token = extractToken(body);
                if (token != null && !token.trim().isEmpty()) {
                    new SessionManager(AuthActivity.this).setToken(token);
                }

                String userEmail = "";
                String userImgUrl = "";
                try {
                    if (body != null && body.has("user") && body.get("user").isJsonObject()) {
                        JsonObject u = body.getAsJsonObject("user");
                        if (u.has("email") && !u.get("email").isJsonNull()) {
                            userEmail = u.get("email").getAsString();
                        }
                        if (u.has("imgUrl") && !u.get("imgUrl").isJsonNull()) {
                            userImgUrl = u.get("imgUrl").getAsString();
                        }
                    }
                } catch (Exception ignored) {
                }

                SessionManager sm = new SessionManager(AuthActivity.this);
                sm.setUserEmail(userEmail);
                sm.setUserImgUrl(userImgUrl);

                if (rememberCheckBox != null && rememberCheckBox.isChecked()) {
                    persistRememberedCredentials(email, password, true);
                } else {
                    persistRememberedCredentials("", "", false);
                }

                Toast.makeText(AuthActivity.this, "Đăng nhập thành công", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(AuthActivity.this, MainActivity.class));
                finish();
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Toast.makeText(AuthActivity.this, "Đăng nhập lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void restoreRememberedCredentials() {
        SharedPreferences prefs = getSharedPreferences(REMEMBER_PREFS, MODE_PRIVATE);
        boolean remember = prefs.getBoolean(KEY_REMEMBER, false);
        if (rememberCheckBox != null) {
            rememberCheckBox.setChecked(remember);
        }
        if (remember) {
            String email = prefs.getString(KEY_REMEMBER_EMAIL, "");
            String password = prefs.getString(KEY_REMEMBER_PASSWORD, "");
            if (emailInput != null) {
                emailInput.setText(email);
            }
            if (passwordInput != null) {
                passwordInput.setText(password);
            }
        }
    }

    private void persistRememberedCredentials(String email, String password, boolean remember) {
        SharedPreferences prefs = getSharedPreferences(REMEMBER_PREFS, MODE_PRIVATE);
        prefs.edit()
                .putBoolean(KEY_REMEMBER, remember)
                .putString(KEY_REMEMBER_EMAIL, remember ? email : "")
                .putString(KEY_REMEMBER_PASSWORD, remember ? password : "")
                .apply();
    }

    private boolean isValidEmail(String email) {
        return email != null && email.contains("@") && email.contains(".");
    }

    private String extractToken(JsonObject body) {
        if (body == null) {
            return null;
        }

        if (body.has("token") && !body.get("token").isJsonNull()) {
            return body.get("token").getAsString();
        }
        if (body.has("accessToken") && !body.get("accessToken").isJsonNull()) {
            return body.get("accessToken").getAsString();
        }
        if (body.has("jwt") && !body.get("jwt").isJsonNull()) {
            return body.get("jwt").getAsString();
        }

        if (body.has("data") && body.get("data").isJsonObject()) {
            JsonObject data = body.getAsJsonObject("data");
            if (data.has("token") && !data.get("token").isJsonNull()) {
                return data.get("token").getAsString();
            }
            if (data.has("accessToken") && !data.get("accessToken").isJsonNull()) {
                return data.get("accessToken").getAsString();
            }
            if (data.has("jwt") && !data.get("jwt").isJsonNull()) {
                return data.get("jwt").getAsString();
            }
        }

        return null;
    }

    private boolean hasAtLeastNDigits(String value, int n) {
        if (value == null) {
            return false;
        }

        int count = 0;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c >= '0' && c <= '9') {
                count++;
                if (count >= n) {
                    return true;
                }
            }
        }
        return false;
    }
}

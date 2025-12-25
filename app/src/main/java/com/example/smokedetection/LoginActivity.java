package com.example.smokedetection;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.smokedetection.databinding.ActivityLoginBinding;

import org.json.JSONObject;

import java.io.IOException;
import java.util.regex.Pattern;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class LoginActivity extends AppCompatActivity {

    private static final String SUPABASE_URL = "https://gklovokybxmonmnuoflk.supabase.co";
    private static final String SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImdrbG92b2t5Ynhtb25tbnVvZmxrIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjUzNTY0NTcsImV4cCI6MjA4MDkzMjQ1N30.rgEFaf0M0WBKNbaintilKHILM3HS4Dnpb40wSbj_hZA";

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    // Password mạnh: có số + chữ hoa + ký tự đặc biệt, tối thiểu 6 ký tự
    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("^(?=.*[0-9])(?=.*[A-Z])(?=.*[!@#\\$%^&*]).{6,}$");

    private ActivityLoginBinding b;
    private SupabaseClient supabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        b = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        supabase = new SupabaseClient(SUPABASE_URL, SUPABASE_KEY);

        // Auto-login nếu có token
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        String savedToken = prefs.getString("auth_token", null);
        if (savedToken != null && !savedToken.trim().isEmpty()) {
            goToDashboard();
            return;
        }

        b.btnLogin.setOnClickListener(v -> loginUser());
        b.btnRegister.setOnClickListener(v -> registerUser());
        b.btnGuest.setOnClickListener(v -> loginGuest());

        setLoading(false);
    }

    private void loginUser() {
        String email = safeText(b.etEmail.getText() != null ? b.etEmail.getText().toString() : "");
        String password = safeText(b.etPassword.getText() != null ? b.etPassword.getText().toString() : "");

        if (!EMAIL_PATTERN.matcher(email).matches()) {
            toast("Wrong email format");
            return;
        }

        if (password.length() < 6) {
            toast("Password must be at least 6 characters");
            return;
        }

        setLoading(true);

        supabase.signIn(email, password, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    setLoading(false);
                    toast("Connection error");
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String responseBody = (response.body() != null) ? response.body().string() : "";

                runOnUiThread(() -> {
                    setLoading(false);

                    if (response.isSuccessful()) {
                        try {
                            JSONObject json = new JSONObject(responseBody);
                            String accessToken = json.optString("access_token", "");

                            if (accessToken.isEmpty()) {
                                toast("Login success but token missing");
                                return;
                            }

                            getSharedPreferences("AppPrefs", MODE_PRIVATE)
                                    .edit()
                                    .putString("auth_token", accessToken)
                                    .apply();

                            toast("Login success!");
                            goToDashboard();
                        } catch (Exception ex) {
                            toast("Error parsing token");
                        }
                    } else {
                        // Nếu muốn debug chi tiết:
                        // toast("Login failed: " + response.code());
                        toast("Login failed");
                    }
                });
            }
        });
    }

    private void registerUser() {
        String email = safeText(b.etEmail.getText() != null ? b.etEmail.getText().toString() : "");
        String password = safeText(b.etPassword.getText() != null ? b.etPassword.getText().toString() : "");

        if (!EMAIL_PATTERN.matcher(email).matches()) {
            toast("Wrong email format");
            return;
        }

        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            toast("Password must have 1 uppercase, 1 number, 1 symbol, min 6 chars");
            return;
        }

        setLoading(true);

        supabase.signUp(email, password, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    setLoading(false);
                    toast("Connection error");
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String body = (response.body() != null) ? response.body().string() : "";

                runOnUiThread(() -> {
                    setLoading(false);

                    if (response.isSuccessful()) {
                        toast("Registered successfully!");
                        // tự login sau khi đăng ký
                        loginUser();
                    } else {
                        try {
                            JSONObject json = new JSONObject(body);
                            String message = json.optString("message", "");
                            if (message.toLowerCase().contains("already")) {
                                toast("Email existed");
                            } else {
                                toast("Registration failed");
                            }
                        } catch (Exception ex) {
                            toast("Registration failed");
                        }
                    }
                });
            }
        });
    }

    private void loginGuest() {
        getSharedPreferences("AppPrefs", MODE_PRIVATE)
                .edit()
                .clear()
                .apply();

        toast("Welcome Guest");
        goToDashboard();
    }

    private void setLoading(boolean loading) {
        // nếu layout bạn không có progress thì sẽ crash
        // nên đảm bảo activity_login.xml có view id="@+id/progress"
        b.progress.setVisibility(loading ? View.VISIBLE : View.GONE);

        b.btnLogin.setEnabled(!loading);
        b.btnRegister.setEnabled(!loading);
        b.btnGuest.setEnabled(!loading);

        b.etEmail.setEnabled(!loading);
        b.etPassword.setEnabled(!loading);
    }

    // ✅ FIX CRASH: chuyển qua DashboardActivity thay vì MainActivity
    private void goToDashboard() {
        Intent i = new Intent(LoginActivity.this, DashboardActivity.class);
        startActivity(i);
        finish();
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private String safeText(String s) {
        return s == null ? "" : s.trim();
    }
}

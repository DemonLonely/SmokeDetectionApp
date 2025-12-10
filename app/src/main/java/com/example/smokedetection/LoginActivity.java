package com.example.smokedetection;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

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
    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("^(?=.*[A-Z])(?=.*[!@#\\$%^&*]).+$");

    private EditText inputEmail, inputPassword;
    private Button btnLogin, btnRegister, btnGuest;
    private SupabaseClient supabase;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        supabase = new SupabaseClient(SUPABASE_URL, SUPABASE_KEY);

        // Check if already log in
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        String savedToken = prefs.getString("auth_token", null);
        if (savedToken != null) {
            goToMain();
        }

        inputEmail = findViewById(R.id.inputEmail);
        inputPassword = findViewById(R.id.inputPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);
        btnGuest = findViewById(R.id.btnGuest);

        btnLogin.setOnClickListener(v -> loginUser());
        btnRegister.setOnClickListener(v -> registerUser());
        btnGuest.setOnClickListener(v -> loginGuest());
    }

    private void loginUser() {
        String email = inputEmail.getText().toString().trim();
        String password = inputPassword.getText().toString().trim();

        if (email.isEmpty() || password.length() < 6) {
            Toast.makeText(this, "Email or password incorrect", Toast.LENGTH_SHORT).show();
            return;
        }

        setButtonsEnabled(false);

        supabase.signIn(email, password, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    setButtonsEnabled(true);
                    Toast.makeText(LoginActivity.this, "Connection error", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String responseBody = response.body().string();
                runOnUiThread(() -> {
                    setButtonsEnabled(true);
                    if (response.isSuccessful()) {
                        try {
                            // Extract the Token
                            JSONObject json = new JSONObject(responseBody);
                            String accessToken = json.getString("access_token");

                            // Save Token to phone memory
                            getSharedPreferences("AppPrefs", MODE_PRIVATE)
                                    .edit().putString("auth_token", accessToken).apply();

                            Toast.makeText(LoginActivity.this, "Login success!", Toast.LENGTH_SHORT).show();
                            goToMain();
                        } catch (Exception e) {
                            Toast.makeText(LoginActivity.this, "Error parsing token", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(LoginActivity.this, "Login failed", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void registerUser() {
        String email = inputEmail.getText().toString().trim();
        String password = inputPassword.getText().toString().trim();

        // Check email format
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            Toast.makeText(this, "Wrong email format", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check valid password
        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            Toast.makeText(this, "Password must have at least 1 uppercase, 1 number, and 1 symbol", Toast.LENGTH_LONG).show();
            return;
        }

        setButtonsEnabled(false);

        supabase.signUp(email, password, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    setButtonsEnabled(true);
                    Toast.makeText(LoginActivity.this, "Connection error", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String body = response.body().string();
                runOnUiThread(() -> {
                    setButtonsEnabled(true);
                    if (response.isSuccessful()) {
                        Toast.makeText(LoginActivity.this, "Registered successfully!", Toast.LENGTH_LONG).show();
                        loginUser();
                    } else {
                        // Check if email already exists
                        try {
                            JSONObject json = new JSONObject(body);
                            if (json.has("message") && json.getString("message").contains("already registered")) {
                                Toast.makeText(LoginActivity.this, "Email existed", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(LoginActivity.this, "Registration failed", Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
                            Toast.makeText(LoginActivity.this, "Registration failed", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });
    }

    private void loginGuest() {
        // Clear old tokens (Guest has no token)
        getSharedPreferences("AppPrefs", MODE_PRIVATE).edit().clear().apply();
        Toast.makeText(this, "Welcome Guest", Toast.LENGTH_SHORT).show();
        goToMain();
    }

    private void setButtonsEnabled(boolean enabled) {
        btnLogin.setEnabled(enabled);
        btnRegister.setEnabled(enabled);
        btnGuest.setEnabled(enabled);
    }

    private void goToMain(){
        startActivity(new Intent(LoginActivity.this, MainActivity.class));
        finish();
    }
}
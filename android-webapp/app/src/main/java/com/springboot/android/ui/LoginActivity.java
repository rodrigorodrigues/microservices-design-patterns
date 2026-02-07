package com.springboot.android.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.browser.customtabs.CustomTabsIntent;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.springboot.android.BuildConfig;
import com.springboot.android.R;
import com.springboot.android.api.ApiClient;
import com.springboot.android.api.AuthService;
import com.springboot.android.model.LoginRequest;
import com.springboot.android.model.LoginResponse;
import com.springboot.android.util.SessionManager;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etUsername;
    private TextInputEditText etPassword;
    private MaterialButton btnLogin;
    private MaterialButton btnGoogleLogin;
    private ProgressBar progressBar;
    private SessionManager sessionManager;
    private AuthService authService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        sessionManager = new SessionManager(this);
        authService = ApiClient.getClient().create(AuthService.class);

        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnGoogleLogin = findViewById(R.id.btnGoogleLogin);
        progressBar = findViewById(R.id.progressBar);

        btnLogin.setOnClickListener(v -> performLogin());
        btnGoogleLogin.setOnClickListener(v -> performGoogleLogin());
    }

    private void performLogin() {
        String username = etUsername.getText() != null ? etUsername.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter username and password", Toast.LENGTH_SHORT).show();
            return;
        }

        btnLogin.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);

        // First, fetch CSRF token
        authService.getCsrfToken().enqueue(new Callback<com.springboot.android.model.CsrfToken>() {
            @Override
            public void onResponse(Call<com.springboot.android.model.CsrfToken> call, Response<com.springboot.android.model.CsrfToken> response) {
                if (response.isSuccessful() && response.body() != null) {
                    com.springboot.android.model.CsrfToken csrfToken = response.body();
                    sessionManager.saveCsrfToken(csrfToken.getToken(), csrfToken.getHeaderName());

                    // Now perform login with CSRF token stored
                    performLoginWithCsrf(username, password);
                } else {
                    btnLogin.setEnabled(true);
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(LoginActivity.this, "Failed to get CSRF token", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<com.springboot.android.model.CsrfToken> call, Throwable t) {
                btnLogin.setEnabled(true);
                progressBar.setVisibility(View.GONE);
                Toast.makeText(LoginActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void performLoginWithCsrf(String username, String password) {
        authService.login(username, password).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                btnLogin.setEnabled(true);
                progressBar.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse loginResponse = response.body();
                    sessionManager.saveAuthToken(loginResponse.getTokenValue());
                    sessionManager.saveUser(username);

                    Toast.makeText(LoginActivity.this, "Login successful", Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(LoginActivity.this, DashboardActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(LoginActivity.this,
                            "Login failed: " + response.message(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                btnLogin.setEnabled(true);
                progressBar.setVisibility(View.GONE);
                Toast.makeText(LoginActivity.this,
                        "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void performGoogleLogin() {
        // Open OAuth2 authorization URL in Chrome Custom Tab
        String oauth2Url = BuildConfig.BASE_URL + "oauth2/authorization/google";

        CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
        CustomTabsIntent customTabsIntent = builder.build();
        customTabsIntent.launchUrl(this, Uri.parse(oauth2Url));
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Check if user is authenticated after OAuth2 redirect
        // This handles the case where Chrome Custom Tab closes and returns to LoginActivity
        if (sessionManager.isLoggedIn()) {
            android.util.Log.d("LoginActivity", "User is logged in, redirecting to dashboard");
            Intent intent = new Intent(this, DashboardActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finishAffinity(); // Close this and any parent activities
        }
    }
}

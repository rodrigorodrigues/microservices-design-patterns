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
import com.springboot.android.model.AccountInfo;
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
    private boolean isCheckingAuth = false;

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

        // Check if user is already authenticated when login page loads
        checkIfAlreadyAuthenticated();
    }

    private void checkIfAlreadyAuthenticated() {
        // Always check authentication on login page load by calling /api/authenticatedUser
        isCheckingAuth = true;
        progressBar.setVisibility(View.VISIBLE);
        btnLogin.setEnabled(false);
        btnGoogleLogin.setEnabled(false);

        android.util.Log.d("LoginActivity", "Checking if user is already authenticated");

        // Call /api/authenticatedUser to check authentication status
        // This endpoint returns OAuth2AccessToken in body and JWT token in Authorization header
        authService.getAuthenticatedUser().enqueue(new Callback<AccountInfo>() {
            @Override
            public void onResponse(Call<AccountInfo> call, Response<AccountInfo> response) {
                isCheckingAuth = false;
                progressBar.setVisibility(View.GONE);
                btnLogin.setEnabled(true);
                btnGoogleLogin.setEnabled(true);

                if (response.isSuccessful()) {
                    // Extract JWT token from Authorization header
                    String authHeader = response.headers().get("Authorization");
                    if (authHeader != null && authHeader.startsWith("Bearer ")) {
                        String token = authHeader.substring(7); // Remove "Bearer " prefix
                        sessionManager.saveAuthToken(token);
                        android.util.Log.d("LoginActivity", "JWT token extracted and saved from header");
                    }

                    android.util.Log.d("LoginActivity", "User is already authenticated, redirecting to dashboard");

                    // Redirect to dashboard (dashboard will call /api/account to load user info)
                    Intent intent = new Intent(LoginActivity.this, DashboardActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                } else {
                    // Token is invalid or user not authenticated, clear token and show login form
                    android.util.Log.d("LoginActivity", "User not authenticated (status: " + response.code() + "), showing login form");
                    sessionManager.clearAuthToken();
                }
            }

            @Override
            public void onFailure(Call<AccountInfo> call, Throwable t) {
                isCheckingAuth = false;
                progressBar.setVisibility(View.GONE);
                btnLogin.setEnabled(true);
                btnGoogleLogin.setEnabled(true);

                // Network error or not authenticated, clear token and show login form
                android.util.Log.d("LoginActivity", "Auth check failed: " + t.getMessage() + ", showing login form");
                sessionManager.clearAuthToken();
            }
        });
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
        // Skip if we're already checking authentication in onCreate
        if (!isCheckingAuth && sessionManager.isLoggedIn()) {
            android.util.Log.d("LoginActivity", "User is logged in on resume, verifying authentication");
            checkIfAlreadyAuthenticated();
        }
    }
}

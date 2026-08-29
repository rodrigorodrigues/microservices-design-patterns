package com.springboot.android.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.GetPublicKeyCredentialOption;
import androidx.credentials.PublicKeyCredential;
import androidx.credentials.exceptions.GetCredentialException;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.springboot.android.BuildConfig;
import com.springboot.android.R;
import com.springboot.android.api.ApiClient;
import com.springboot.android.api.AuthService;
import com.springboot.android.api.PasskeyService;
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
    private MaterialButton btnPasskeyLogin;
    private ProgressBar progressBar;
    private SessionManager sessionManager;
    private AuthService authService;
    private PasskeyService passkeyService;
    private CredentialManager credentialManager;
    private boolean isCheckingAuth = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        sessionManager = new SessionManager(this);
        authService = ApiClient.getClient().create(AuthService.class);
        passkeyService = ApiClient.getClient().create(PasskeyService.class);
        credentialManager = CredentialManager.create(this);

        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnGoogleLogin = findViewById(R.id.btnGoogleLogin);
        btnPasskeyLogin = findViewById(R.id.btnPasskeyLogin);
        progressBar = findViewById(R.id.progressBar);

        btnLogin.setOnClickListener(v -> performLogin());
        btnGoogleLogin.setOnClickListener(v -> performGoogleLogin());
        btnPasskeyLogin.setOnClickListener(v -> performPasskeyLogin());

        // Check if user is already authenticated when login page loads
        checkIfAlreadyAuthenticated();
    }

    private void checkIfAlreadyAuthenticated() {
        // Always check authentication on login page load by calling /api/authenticatedUser
        isCheckingAuth = true;
        progressBar.setVisibility(View.VISIBLE);
        btnLogin.setEnabled(false);
        btnGoogleLogin.setEnabled(false);
        btnPasskeyLogin.setEnabled(false);

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
                btnPasskeyLogin.setEnabled(true);

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
                btnPasskeyLogin.setEnabled(true);

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

    private void performPasskeyLogin() {
        progressBar.setVisibility(View.VISIBLE);
        btnLogin.setEnabled(false);
        btnGoogleLogin.setEnabled(false);
        btnPasskeyLogin.setEnabled(false);

        // Fetch a fresh CSRF token first, matching react-webapp's Login.js handlePasskey() -
        // the webauthn/** endpoints are CSRF-protected and a stale/missing token 403s.
        authService.getCsrfToken().enqueue(new Callback<com.springboot.android.model.CsrfToken>() {
            @Override
            public void onResponse(Call<com.springboot.android.model.CsrfToken> call, Response<com.springboot.android.model.CsrfToken> response) {
                fetchAuthenticateOptions();
            }

            @Override
            public void onFailure(Call<com.springboot.android.model.CsrfToken> call, Throwable t) {
                finishPasskeyLoginWithError("Error: " + t.getMessage());
            }
        });
    }

    private void fetchAuthenticateOptions() {
        passkeyService.getAuthenticateOptions().enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    getPasskeyAssertion(response.body());
                } else {
                    finishPasskeyLoginWithError("Failed to get passkey challenge");
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                finishPasskeyLoginWithError("Error: " + t.getMessage());
            }
        });
    }

    private void getPasskeyAssertion(JsonObject challengeOptions) {
        GetPublicKeyCredentialOption option = new GetPublicKeyCredentialOption(challengeOptions.toString());
        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(option)
                .build();

        credentialManager.getCredentialAsync(
                this,
                request,
                null,
                getMainExecutor(),
                new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                    @Override
                    public void onResult(GetCredentialResponse result) {
                        PublicKeyCredential credential = (PublicKeyCredential) result.getCredential();
                        JsonObject assertion = JsonParser.parseString(credential.getAuthenticationResponseJson()).getAsJsonObject();
                        sendPasskeyAssertion(assertion);
                    }

                    @Override
                    public void onError(GetCredentialException e) {
                        finishPasskeyLoginWithError("Passkey sign-in failed: " + e.getMessage());
                    }
                });
    }

    private void sendPasskeyAssertion(JsonObject assertion) {
        passkeyService.loginWithPasskey(assertion).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    checkIfAlreadyAuthenticated();
                } else {
                    finishPasskeyLoginWithError("Passkey sign-in failed");
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                finishPasskeyLoginWithError("Error: " + t.getMessage());
            }
        });
    }

    private void finishPasskeyLoginWithError(String message) {
        progressBar.setVisibility(View.GONE);
        btnLogin.setEnabled(true);
        btnGoogleLogin.setEnabled(true);
        btnPasskeyLogin.setEnabled(true);
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void performGoogleLogin() {
        // Runs the flow in an in-app WebView (not a Chrome Custom Tab) so the resulting
        // session cookie can be imported into ApiClient - webauthn/** endpoints need it.
        startActivity(new Intent(this, GoogleLoginWebViewActivity.class));
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

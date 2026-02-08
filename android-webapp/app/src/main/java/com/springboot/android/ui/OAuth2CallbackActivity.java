package com.springboot.android.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.springboot.android.api.ApiClient;
import com.springboot.android.api.AuthService;
import com.springboot.android.model.AccountInfo;
import com.springboot.android.util.SessionManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OAuth2CallbackActivity extends AppCompatActivity {

    private SessionManager sessionManager;
    private AuthService authService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        android.util.Log.d("OAuth2Callback", "OAuth2CallbackActivity started");

        Intent intent = getIntent();
        Uri data = intent.getData();

        android.util.Log.d("OAuth2Callback", "Intent data: " + data);
        android.util.Log.d("OAuth2Callback", "Intent action: " + intent.getAction());

        sessionManager = new SessionManager(this);
        authService = ApiClient.getClient().create(AuthService.class);

        // Extract token from URL query parameters
        if (data != null) {
            String token = data.getQueryParameter("token");
            String username = data.getQueryParameter("username");
            String email = data.getQueryParameter("email");

            android.util.Log.d("OAuth2Callback", "Token received: " + (token != null ? "Yes" : "No"));
            android.util.Log.d("OAuth2Callback", "Username: " + username);
            android.util.Log.d("OAuth2Callback", "Email: " + email);

            if (token != null) {
                // Save JWT token first so it can be used in subsequent API calls
                sessionManager.saveAuthToken(token);
                android.util.Log.d("OAuth2Callback", "JWT token saved, verifying authentication");

                // Call /api/authenticatedUser to verify the token and get full user details
                verifyAuthentication(username);
            } else {
                android.util.Log.e("OAuth2Callback", "Missing token in URL");
                Toast.makeText(OAuth2CallbackActivity.this,
                        "Authentication failed: Missing token", Toast.LENGTH_SHORT).show();
                navigateToLogin();
            }
        } else {
            android.util.Log.e("OAuth2Callback", "No data in intent");
            Toast.makeText(OAuth2CallbackActivity.this,
                    "Authentication failed: No data", Toast.LENGTH_SHORT).show();
            navigateToLogin();
        }
    }

    private void verifyAuthentication(final String username) {
        // Use /api/account to verify authentication and get full user details
        authService.getAccount().enqueue(new Callback<AccountInfo>() {
            @Override
            public void onResponse(Call<AccountInfo> call, Response<AccountInfo> response) {
                if (response.isSuccessful() && response.body() != null) {
                    AccountInfo account = response.body();

                    // Save user info
                    sessionManager.saveUser(account.getLogin());

                    android.util.Log.d("OAuth2Callback", "Authentication verified successfully");

                    Toast.makeText(OAuth2CallbackActivity.this,
                            "Welcome " + (username != null ? username : account.getFullName()),
                            Toast.LENGTH_SHORT).show();

                    // Navigate to dashboard home, clearing all previous activities
                    Intent dashboardIntent = new Intent(OAuth2CallbackActivity.this, DashboardActivity.class);
                    dashboardIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(dashboardIntent);

                    // Finish this activity and all parent activities
                    finishAffinity();
                } else {
                    android.util.Log.e("OAuth2Callback", "Authentication verification failed with code: " + response.code());

                    // Clear the invalid token
                    sessionManager.clearAuthToken();

                    Toast.makeText(OAuth2CallbackActivity.this,
                            "Authentication verification failed: " + response.code(), Toast.LENGTH_SHORT).show();
                    navigateToLogin();
                }
            }

            @Override
            public void onFailure(Call<AccountInfo> call, Throwable t) {
                android.util.Log.e("OAuth2Callback", "Failed to verify authentication", t);

                // Clear the invalid token
                sessionManager.clearAuthToken();

                Toast.makeText(OAuth2CallbackActivity.this,
                        "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                navigateToLogin();
            }
        });
    }

    private void navigateToLogin() {
        Intent loginIntent = new Intent(OAuth2CallbackActivity.this, LoginActivity.class);
        loginIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(loginIntent);
        finish();
    }
}

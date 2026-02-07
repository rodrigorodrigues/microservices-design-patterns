package com.springboot.android.ui;

import android.content.Intent;
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
        android.util.Log.d("OAuth2Callback", "Intent data: " + getIntent().getData());
        android.util.Log.d("OAuth2Callback", "Intent action: " + getIntent().getAction());

        sessionManager = new SessionManager(this);
        authService = ApiClient.getClient().create(AuthService.class);

        // After OAuth2 login, the server sets a session cookie
        // Fetch authenticated user info to verify authentication and get JWT token
        fetchAuthenticatedUser();
    }

    private void fetchAuthenticatedUser() {
        authService.getAuthenticatedUser().enqueue(new Callback<AccountInfo>() {
            @Override
            public void onResponse(Call<AccountInfo> call, Response<AccountInfo> response) {
                if (response.isSuccessful() && response.body() != null) {
                    AccountInfo account = response.body();

                    // Save user info
                    sessionManager.saveUser(account.getLogin());

                    // Extract JWT token from Authorization header
                    String authHeader = response.headers().get("Authorization");
                    if (authHeader != null && authHeader.startsWith("Bearer ")) {
                        String token = authHeader.substring(7); // Remove "Bearer " prefix
                        sessionManager.saveAuthToken(token);
                        android.util.Log.d("OAuth2Callback", "JWT token saved successfully");
                    } else {
                        // Fallback: use session cookie authentication
                        sessionManager.saveAuthToken("oauth2-session-authenticated");
                        android.util.Log.d("OAuth2Callback", "Using session cookie authentication");
                    }

                    android.util.Log.d("OAuth2Callback", "Authentication successful, navigating to dashboard");

                    Toast.makeText(OAuth2CallbackActivity.this,
                            "Welcome " + account.getFullName(), Toast.LENGTH_SHORT).show();

                    // Navigate to dashboard home, clearing all previous activities
                    Intent intent = new Intent(OAuth2CallbackActivity.this, DashboardActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);

                    // Finish this activity and all parent activities
                    finishAffinity();
                } else {
                    android.util.Log.e("OAuth2Callback", "Authentication failed with code: " + response.code());
                    Toast.makeText(OAuth2CallbackActivity.this,
                            "Authentication failed: " + response.code(), Toast.LENGTH_SHORT).show();

                    // Navigate back to login
                    Intent intent = new Intent(OAuth2CallbackActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                }
            }

            @Override
            public void onFailure(Call<AccountInfo> call, Throwable t) {
                android.util.Log.e("OAuth2Callback", "Failed to fetch authenticated user info", t);
                Toast.makeText(OAuth2CallbackActivity.this,
                        "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();

                // Navigate back to login
                Intent intent = new Intent(OAuth2CallbackActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        });
    }
}

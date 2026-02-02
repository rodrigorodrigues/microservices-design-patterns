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

        sessionManager = new SessionManager(this);
        authService = ApiClient.getClient().create(AuthService.class);

        // After OAuth2 login, the server sets a session cookie
        // Fetch user account info to verify authentication and get JWT token
        fetchAccountInfo();
    }

    private void fetchAccountInfo() {
        authService.getAccount().enqueue(new Callback<AccountInfo>() {
            @Override
            public void onResponse(Call<AccountInfo> call, Response<AccountInfo> response) {
                if (response.isSuccessful() && response.body() != null) {
                    AccountInfo account = response.body();

                    // Save user info
                    sessionManager.saveUser(account.getLogin());

                    // For OAuth2, we rely on session cookies, not JWT
                    // Mark as authenticated
                    sessionManager.saveAuthToken("oauth2-authenticated");

                    Toast.makeText(OAuth2CallbackActivity.this,
                            "Welcome " + account.getFullName(), Toast.LENGTH_SHORT).show();

                    // Navigate to dashboard
                    Intent intent = new Intent(OAuth2CallbackActivity.this, DashboardActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(OAuth2CallbackActivity.this,
                            "Authentication failed", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onFailure(Call<AccountInfo> call, Throwable t) {
                Toast.makeText(OAuth2CallbackActivity.this,
                        "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }
}

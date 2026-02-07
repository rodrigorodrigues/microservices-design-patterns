package com.springboot.android.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.springboot.android.util.SessionManager;

public class OAuth2CallbackActivity extends AppCompatActivity {

    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        android.util.Log.d("OAuth2Callback", "OAuth2CallbackActivity started");

        Intent intent = getIntent();
        Uri data = intent.getData();

        android.util.Log.d("OAuth2Callback", "Intent data: " + data);
        android.util.Log.d("OAuth2Callback", "Intent action: " + intent.getAction());

        sessionManager = new SessionManager(this);

        // Extract token and user info from URL query parameters
        if (data != null) {
            String token = data.getQueryParameter("token");
            String username = data.getQueryParameter("username");
            String email = data.getQueryParameter("email");

            android.util.Log.d("OAuth2Callback", "Token received: " + (token != null ? "Yes" : "No"));
            android.util.Log.d("OAuth2Callback", "Username: " + username);
            android.util.Log.d("OAuth2Callback", "Email: " + email);

            if (token != null && username != null && email != null) {
                // Save authentication info
                sessionManager.saveAuthToken(token);
                sessionManager.saveUser(email);

                android.util.Log.d("OAuth2Callback", "Authentication successful, navigating to dashboard");

                Toast.makeText(OAuth2CallbackActivity.this,
                        "Welcome " + username, Toast.LENGTH_SHORT).show();

                // Navigate to dashboard home, clearing all previous activities
                Intent dashboardIntent = new Intent(OAuth2CallbackActivity.this, DashboardActivity.class);
                dashboardIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(dashboardIntent);

                // Finish this activity and all parent activities
                finishAffinity();
            } else {
                android.util.Log.e("OAuth2Callback", "Missing token or user info in URL");
                Toast.makeText(OAuth2CallbackActivity.this,
                        "Authentication failed: Missing token", Toast.LENGTH_SHORT).show();

                // Navigate back to login
                Intent loginIntent = new Intent(OAuth2CallbackActivity.this, LoginActivity.class);
                loginIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(loginIntent);
                finish();
            }
        } else {
            android.util.Log.e("OAuth2Callback", "No data in intent");
            Toast.makeText(OAuth2CallbackActivity.this,
                    "Authentication failed: No data", Toast.LENGTH_SHORT).show();

            // Navigate back to login
            Intent loginIntent = new Intent(OAuth2CallbackActivity.this, LoginActivity.class);
            loginIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(loginIntent);
            finish();
        }
    }
}

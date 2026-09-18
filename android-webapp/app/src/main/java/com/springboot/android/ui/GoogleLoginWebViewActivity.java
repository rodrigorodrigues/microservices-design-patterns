package com.springboot.android.ui;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.springboot.android.BuildConfig;
import com.springboot.android.R;
import com.springboot.android.api.ApiClient;
import com.springboot.android.api.AuthService;
import com.springboot.android.model.AccountInfo;
import com.springboot.android.util.SessionManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Runs the Google OAuth2 login flow in an in-app WebView, rather than a Chrome Custom Tab,
 * so the resulting SESSIONID/XSRF-TOKEN cookies (visible to android.webkit.CookieManager)
 * can be imported into ApiClient's cookie jar. webauthn/** endpoints require a
 * session-authenticated principal - the JWT this flow also returns is not enough for those.
 */
public class GoogleLoginWebViewActivity extends AppCompatActivity {
    private static final String CALLBACK_SCHEME = "spendingbetter";
    private static final String CALLBACK_HOST = "oauth2callback";

    private WebView webView;
    private ProgressBar progressBar;
    private SessionManager sessionManager;
    private AuthService authService;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_google_login);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        sessionManager = new SessionManager(this);
        authService = ApiClient.getClient().create(AuthService.class);

        webView = findViewById(R.id.webView);
        progressBar = findViewById(R.id.progressBar);

        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri url = request.getUrl();
                if (CALLBACK_SCHEME.equals(url.getScheme()) && CALLBACK_HOST.equals(url.getHost())) {
                    handleOAuth2Callback(url);
                    return true;
                }
                return false;
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                progressBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                progressBar.setVisibility(View.GONE);
            }
        });

        webView.loadUrl(BuildConfig.BASE_URL + "oauth2/authorization/google");
    }

    private void handleOAuth2Callback(Uri url) {
        String token = url.getQueryParameter("token");
        String username = url.getQueryParameter("username");

        if (token == null) {
            Toast.makeText(this, "Authentication failed: missing token", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        ApiClient.importCookiesFromWebView(this, BuildConfig.BASE_URL);

        sessionManager.saveAuthToken(token);
        verifyAuthentication(username);
    }

    private void verifyAuthentication(String username) {
        authService.getAccount().enqueue(new Callback<AccountInfo>() {
            @Override
            public void onResponse(Call<AccountInfo> call, Response<AccountInfo> response) {
                if (response.isSuccessful() && response.body() != null) {
                    AccountInfo account = response.body();
                    sessionManager.saveUser(account.getLogin());

                    Toast.makeText(GoogleLoginWebViewActivity.this,
                            "Welcome " + (username != null ? username : account.getFullName()),
                            Toast.LENGTH_SHORT).show();

                    Intent dashboardIntent = new Intent(GoogleLoginWebViewActivity.this, DashboardActivity.class);
                    dashboardIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(dashboardIntent);
                    finishAffinity();
                } else {
                    sessionManager.clearAuthToken();
                    Toast.makeText(GoogleLoginWebViewActivity.this,
                            "Authentication verification failed: " + response.code(), Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onFailure(Call<AccountInfo> call, Throwable t) {
                sessionManager.clearAuthToken();
                Toast.makeText(GoogleLoginWebViewActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}

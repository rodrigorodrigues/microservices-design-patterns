package com.springboot.android.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.springboot.android.R;
import com.springboot.android.api.ApiClient;
import com.springboot.android.api.AuthService;
import com.springboot.android.model.AccountInfo;
import com.springboot.android.util.SessionManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DashboardActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private TextView tvFullName, tvEmail, tvLogin, tvAuthorities, tvStatus, tvLanguage;
    private ProgressBar progressBar;
    private SessionManager sessionManager;
    private AuthService authService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        sessionManager = new SessionManager(this);
        authService = ApiClient.getClient().create(AuthService.class);

        // Check if user is logged in
        if (!sessionManager.isLoggedIn()) {
            startLoginActivity();
            return;
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.app_name, R.string.app_name
        );
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        tvFullName = findViewById(R.id.tvFullName);
        tvEmail = findViewById(R.id.tvEmail);
        tvLogin = findViewById(R.id.tvLogin);
        tvAuthorities = findViewById(R.id.tvAuthorities);
        tvStatus = findViewById(R.id.tvStatus);
        tvLanguage = findViewById(R.id.tvLanguage);
        progressBar = findViewById(R.id.progressBar);

        loadAccountInfo();
    }

    private void loadAccountInfo() {
        progressBar.setVisibility(View.VISIBLE);
        authService.getAccount().enqueue(new Callback<AccountInfo>() {
            @Override
            public void onResponse(Call<AccountInfo> call, Response<AccountInfo> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    AccountInfo account = response.body();
                    displayAccountInfo(account);
                    updateNavHeader(account);
                } else {
                    Toast.makeText(DashboardActivity.this, "Failed to load account info", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<AccountInfo> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(DashboardActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayAccountInfo(AccountInfo account) {
        tvFullName.setText("Name: " + (account.getFullName() != null ? account.getFullName() : "N/A"));
        tvEmail.setText("Email: " + (account.getEmail() != null ? account.getEmail() : "N/A"));
        tvLogin.setText("Login: " + (account.getLogin() != null ? account.getLogin() : "N/A"));

        if (account.getAuthorities() != null && !account.getAuthorities().isEmpty()) {
            tvAuthorities.setText(String.join(", ", account.getAuthorities()));
        } else {
            tvAuthorities.setText("No authorities");
        }

        tvStatus.setText("Status: " + (account.isActivated() ? "Active" : "Inactive"));
        tvLanguage.setText("Language: " + (account.getLangKey() != null ? account.getLangKey().toUpperCase() : "N/A"));
    }

    private void updateNavHeader(AccountInfo account) {
        View headerView = navigationView.getHeaderView(0);
        TextView tvHeaderName = headerView.findViewById(R.id.tvHeaderName);
        TextView tvHeaderEmail = headerView.findViewById(R.id.tvHeaderEmail);

        tvHeaderName.setText(account.getFullName() != null ? account.getFullName() : account.getLogin());
        tvHeaderEmail.setText(account.getEmail() != null ? account.getEmail() : "");
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_dashboard) {
            // Already on dashboard
        } else if (id == R.id.nav_companies) {
            startActivity(new Intent(this, CompanyListActivity.class));
        } else if (id == R.id.nav_persons) {
            startActivity(new Intent(this, PersonListActivity.class));
        } else if (id == R.id.nav_products) {
            startActivity(new Intent(this, ProductListActivity.class));
        } else if (id == R.id.nav_users) {
            startActivity(new Intent(this, UserListActivity.class));
        } else if (id == R.id.nav_warehouses) {
            startActivity(new Intent(this, WarehouseListActivity.class));
        } else if (id == R.id.nav_stocks) {
            startActivity(new Intent(this, StockListActivity.class));
        } else if (id == R.id.nav_posts) {
            startActivity(new Intent(this, PostListActivity.class));
        } else if (id == R.id.nav_tasks) {
            startActivity(new Intent(this, TaskListActivity.class));
        } else if (id == R.id.nav_logout) {
            logout();
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void logout() {
        sessionManager.logout();
        startLoginActivity();
    }

    private void startLoginActivity() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}

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
import com.springboot.android.util.PermissionHelper;
import com.springboot.android.util.SessionManager;

import java.util.List;

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
                    // Save authorities to session
                    if (account.getAuthorities() != null) {
                        sessionManager.saveAuthorities(account.getAuthorities());
                    }
                    displayAccountInfo(account);
                    updateNavHeader(account);
                    configureMenuItemsVisibility(account.getAuthorities());
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

    private void configureMenuItemsVisibility(List<String> authorities) {
        if (authorities == null || navigationView == null) {
            return;
        }

        MenuItem navCompanies = navigationView.getMenu().findItem(R.id.nav_companies);
        MenuItem navPersons = navigationView.getMenu().findItem(R.id.nav_persons);
        MenuItem navProducts = navigationView.getMenu().findItem(R.id.nav_products);
        MenuItem navUsers = navigationView.getMenu().findItem(R.id.nav_users);
        MenuItem navWarehouses = navigationView.getMenu().findItem(R.id.nav_warehouses);
        MenuItem navStocks = navigationView.getMenu().findItem(R.id.nav_stocks);
        MenuItem navPosts = navigationView.getMenu().findItem(R.id.nav_posts);
        MenuItem navTasks = navigationView.getMenu().findItem(R.id.nav_tasks);
        MenuItem navCategories = navigationView.getMenu().findItem(R.id.nav_categories);
        MenuItem navIngredients = navigationView.getMenu().findItem(R.id.nav_ingredients);
        MenuItem navRecipes = navigationView.getMenu().findItem(R.id.nav_recipes);

        // Check permissions and disable menu items accordingly
        if (navCompanies != null) {
            boolean hasAccess = PermissionHelper.hasAnyPermission(authorities,
                "ROLE_COMPANY_READ", "ROLE_COMPANY_CREATE", "ROLE_COMPANY_SAVE", "ROLE_COMPANY_DELETE");
            navCompanies.setEnabled(hasAccess);
        }

        if (navPersons != null) {
            boolean hasAccess = PermissionHelper.hasAnyPermission(authorities,
                "ROLE_PERSON_READ", "ROLE_PERSON_CREATE", "ROLE_PERSON_SAVE", "ROLE_PERSON_DELETE");
            navPersons.setEnabled(hasAccess);
        }

        if (navProducts != null) {
            boolean hasAccess = PermissionHelper.hasAnyPermission(authorities,
                "ROLE_PRODUCT_READ", "ROLE_PRODUCT_CREATE", "ROLE_PRODUCT_SAVE", "ROLE_PRODUCT_DELETE");
            navProducts.setEnabled(hasAccess);
        }

        // Users menu is only enabled for ROLE_ADMIN
        if (navUsers != null) {
            boolean isAdmin = authorities.contains("ROLE_ADMIN");
            navUsers.setEnabled(isAdmin);
        }

        if (navWarehouses != null) {
            boolean hasAccess = PermissionHelper.hasAnyPermission(authorities,
                "ROLE_WAREHOUSE_READ", "ROLE_WAREHOUSE_CREATE", "ROLE_WAREHOUSE_SAVE", "ROLE_WAREHOUSE_DELETE");
            navWarehouses.setEnabled(hasAccess);
        }

        if (navStocks != null) {
            boolean hasAccess = PermissionHelper.hasAnyPermission(authorities,
                "ROLE_STOCK_READ", "ROLE_STOCK_CREATE", "ROLE_STOCK_SAVE", "ROLE_STOCK_DELETE");
            navStocks.setEnabled(hasAccess);
        }

        if (navPosts != null) {
            boolean hasAccess = PermissionHelper.hasAnyPermission(authorities,
                "ROLE_POST_READ", "ROLE_POST_CREATE", "ROLE_POST_SAVE", "ROLE_POST_DELETE");
            navPosts.setEnabled(hasAccess);
        }

        if (navTasks != null) {
            boolean hasAccess = PermissionHelper.hasAnyPermission(authorities,
                "ROLE_TASK_READ", "ROLE_TASK_CREATE", "ROLE_TASK_SAVE", "ROLE_TASK_DELETE");
            navTasks.setEnabled(hasAccess);
        }

        if (navCategories != null) {
            boolean hasAccess = PermissionHelper.hasAnyPermission(authorities,
                "ROLE_CATEGORY_READ", "ROLE_CATEGORY_CREATE", "ROLE_CATEGORY_SAVE", "ROLE_CATEGORY_DELETE");
            navCategories.setEnabled(hasAccess);
        }

        if (navIngredients != null) {
            boolean hasAccess = PermissionHelper.hasAnyPermission(authorities,
                "ROLE_INGREDIENT_READ", "ROLE_INGREDIENT_CREATE", "ROLE_INGREDIENT_SAVE", "ROLE_INGREDIENT_DELETE");
            navIngredients.setEnabled(hasAccess);
        }

        if (navRecipes != null) {
            boolean hasAccess = PermissionHelper.hasAnyPermission(authorities,
                "ROLE_RECIPE_READ", "ROLE_RECIPE_CREATE", "ROLE_RECIPE_SAVE", "ROLE_RECIPE_DELETE");
            navRecipes.setEnabled(hasAccess);
        }
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
        } else if (id == R.id.nav_categories) {
            startActivity(new Intent(this, WeekMenuCategoryListActivity.class));
        } else if (id == R.id.nav_ingredients) {
            startActivity(new Intent(this, IngredientListActivity.class));
        } else if (id == R.id.nav_recipes) {
            startActivity(new Intent(this, RecipeListActivity.class));
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

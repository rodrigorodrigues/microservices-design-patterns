package com.springboot.android.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.springboot.android.R;
import com.springboot.android.api.ApiClient;
import com.springboot.android.api.CompanyService;
import com.springboot.android.api.PersonService;
import com.springboot.android.api.ProductService;
import com.springboot.android.api.StockService;
import com.springboot.android.api.UserService;
import com.springboot.android.api.WarehouseService;
import com.springboot.android.model.Company;
import com.springboot.android.model.PageResponse;
import com.springboot.android.model.Person;
import com.springboot.android.model.Product;
import com.springboot.android.model.Stock;
import com.springboot.android.model.User;
import com.springboot.android.model.Warehouse;
import com.springboot.android.util.PermissionHelper;
import com.springboot.android.util.SessionManager;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private SessionManager sessionManager;
    private SwipeRefreshLayout swipeRefresh;
    private CompanyService companyService;
    private PersonService personService;
    private ProductService productService;
    private UserService userService;
    private WarehouseService warehouseService;
    private StockService stockService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sessionManager = new SessionManager(this);

        // Check if user is logged in
        if (!sessionManager.isLoggedIn()) {
            startLoginActivity();
            return;
        }

        // Initialize services
        companyService = ApiClient.getClient().create(CompanyService.class);
        personService = ApiClient.getClient().create(PersonService.class);
        productService = ApiClient.getClient().create(ProductService.class);
        userService = ApiClient.getClient().create(UserService.class);
        warehouseService = ApiClient.getClient().create(WarehouseService.class);
        stockService = ApiClient.getClient().create(StockService.class);

        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Dashboard");
            }
        }

        swipeRefresh = findViewById(R.id.swipeRefresh);
        swipeRefresh.setOnRefreshListener(() -> {
            loadData();
            swipeRefresh.setRefreshing(false);
        });

        MaterialCardView cardCompanies = findViewById(R.id.cardCompanies);
        MaterialCardView cardPersons = findViewById(R.id.cardPersons);
        MaterialCardView cardProducts = findViewById(R.id.cardProducts);
        MaterialCardView cardUsers = findViewById(R.id.cardUsers);
        MaterialCardView cardWarehouses = findViewById(R.id.cardWarehouses);
        MaterialCardView cardStocks = findViewById(R.id.cardStocks);
        MaterialCardView cardCategories = findViewById(R.id.cardCategories);
        MaterialCardView cardIngredients = findViewById(R.id.cardIngredients);
        MaterialCardView cardRecipes = findViewById(R.id.cardRecipes);

        // Configure card states based on permissions
        List<String> authorities = sessionManager.getAuthorities();
        configureCardPermissions(cardCompanies, cardPersons, cardProducts, cardUsers,
                                cardWarehouses, cardStocks, cardCategories, cardIngredients, cardRecipes, authorities);

        cardCompanies.setOnClickListener(v -> {
            if (cardCompanies.isEnabled()) {
                Intent intent = new Intent(MainActivity.this, CompanyListActivity.class);
                startActivity(intent);
            }
        });

        cardPersons.setOnClickListener(v -> {
            if (cardPersons.isEnabled()) {
                Intent intent = new Intent(MainActivity.this, PersonListActivity.class);
                startActivity(intent);
            }
        });

        cardProducts.setOnClickListener(v -> {
            if (cardProducts.isEnabled()) {
                Intent intent = new Intent(MainActivity.this, ProductListActivity.class);
                startActivity(intent);
            }
        });

        cardUsers.setOnClickListener(v -> {
            if (cardUsers.isEnabled()) {
                Intent intent = new Intent(MainActivity.this, UserListActivity.class);
                startActivity(intent);
            }
        });

        cardWarehouses.setOnClickListener(v -> {
            if (cardWarehouses.isEnabled()) {
                Intent intent = new Intent(MainActivity.this, WarehouseListActivity.class);
                startActivity(intent);
            }
        });

        cardStocks.setOnClickListener(v -> {
            if (cardStocks.isEnabled()) {
                Intent intent = new Intent(MainActivity.this, StockListActivity.class);
                startActivity(intent);
            }
        });

        cardCategories.setOnClickListener(v -> {
            if (cardCategories.isEnabled()) {
                Intent intent = new Intent(MainActivity.this, WeekMenuCategoryListActivity.class);
                startActivity(intent);
            }
        });

        cardIngredients.setOnClickListener(v -> {
            if (cardIngredients.isEnabled()) {
                Intent intent = new Intent(MainActivity.this, IngredientListActivity.class);
                startActivity(intent);
            }
        });

        cardRecipes.setOnClickListener(v -> {
            if (cardRecipes.isEnabled()) {
                Intent intent = new Intent(MainActivity.this, RecipeListActivity.class);
                startActivity(intent);
            }
        });

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(view ->
                Toast.makeText(this, "Add new item", Toast.LENGTH_SHORT).show());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);

        // Configure menu items based on permissions
        List<String> authorities = sessionManager.getAuthorities();
        configureMenuItemsPermissions(menu, authorities);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_logout) {
            sessionManager.logout();
            startLoginActivity();
            return true;
        } else if (id == R.id.action_categories) {
            Intent intent = new Intent(this, WeekMenuCategoryListActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_ingredients) {
            Intent intent = new Intent(this, IngredientListActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_recipes) {
            Intent intent = new Intent(this, RecipeListActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void configureCardPermissions(MaterialCardView cardCompanies, MaterialCardView cardPersons,
                                          MaterialCardView cardProducts, MaterialCardView cardUsers,
                                          MaterialCardView cardWarehouses, MaterialCardView cardStocks,
                                          MaterialCardView cardCategories, MaterialCardView cardIngredients,
                                          MaterialCardView cardRecipes, List<String> authorities) {
        if (authorities == null) {
            return;
        }

        // Companies
        boolean hasCompanyAccess = PermissionHelper.hasAnyPermission(authorities,
            "ROLE_COMPANY_READ", "ROLE_COMPANY_CREATE", "ROLE_COMPANY_SAVE", "ROLE_COMPANY_DELETE");
        cardCompanies.setEnabled(hasCompanyAccess);
        cardCompanies.setAlpha(hasCompanyAccess ? 1.0f : 0.5f);

        // Persons
        boolean hasPersonAccess = PermissionHelper.hasAnyPermission(authorities,
            "ROLE_PERSON_READ", "ROLE_PERSON_CREATE", "ROLE_PERSON_SAVE", "ROLE_PERSON_DELETE");
        cardPersons.setEnabled(hasPersonAccess);
        cardPersons.setAlpha(hasPersonAccess ? 1.0f : 0.5f);

        // Products
        boolean hasProductAccess = PermissionHelper.hasAnyPermission(authorities,
            "ROLE_PRODUCT_READ", "ROLE_PRODUCT_CREATE", "ROLE_PRODUCT_SAVE", "ROLE_PRODUCT_DELETE");
        cardProducts.setEnabled(hasProductAccess);
        cardProducts.setAlpha(hasProductAccess ? 1.0f : 0.5f);

        // Users - only for ROLE_ADMIN
        boolean isAdmin = authorities.contains("ROLE_ADMIN");
        cardUsers.setEnabled(isAdmin);
        cardUsers.setAlpha(isAdmin ? 1.0f : 0.5f);

        // Warehouses
        boolean hasWarehouseAccess = PermissionHelper.hasAnyPermission(authorities,
            "ROLE_WAREHOUSE_READ", "ROLE_WAREHOUSE_CREATE", "ROLE_WAREHOUSE_SAVE", "ROLE_WAREHOUSE_DELETE");
        cardWarehouses.setEnabled(hasWarehouseAccess);
        cardWarehouses.setAlpha(hasWarehouseAccess ? 1.0f : 0.5f);

        // Stocks
        boolean hasStockAccess = PermissionHelper.hasAnyPermission(authorities,
            "ROLE_STOCK_READ", "ROLE_STOCK_CREATE", "ROLE_STOCK_SAVE", "ROLE_STOCK_DELETE");
        cardStocks.setEnabled(hasStockAccess);
        cardStocks.setAlpha(hasStockAccess ? 1.0f : 0.5f);

        // Categories
        boolean hasCategoryAccess = PermissionHelper.hasAnyPermission(authorities,
            "ROLE_CATEGORY_READ", "ROLE_CATEGORY_CREATE", "ROLE_CATEGORY_SAVE", "ROLE_CATEGORY_DELETE");
        cardCategories.setEnabled(hasCategoryAccess);
        cardCategories.setAlpha(hasCategoryAccess ? 1.0f : 0.5f);

        // Ingredients
        boolean hasIngredientAccess = PermissionHelper.hasAnyPermission(authorities,
            "ROLE_INGREDIENT_READ", "ROLE_INGREDIENT_CREATE", "ROLE_INGREDIENT_SAVE", "ROLE_INGREDIENT_DELETE");
        cardIngredients.setEnabled(hasIngredientAccess);
        cardIngredients.setAlpha(hasIngredientAccess ? 1.0f : 0.5f);

        // Recipes
        boolean hasRecipeAccess = PermissionHelper.hasAnyPermission(authorities,
            "ROLE_RECIPE_READ", "ROLE_RECIPE_CREATE", "ROLE_RECIPE_SAVE", "ROLE_RECIPE_DELETE");
        cardRecipes.setEnabled(hasRecipeAccess);
        cardRecipes.setAlpha(hasRecipeAccess ? 1.0f : 0.5f);
    }

    private void configureMenuItemsPermissions(Menu menu, List<String> authorities) {
        if (authorities == null || menu == null) {
            return;
        }

        MenuItem actionCategories = menu.findItem(R.id.action_categories);
        MenuItem actionIngredients = menu.findItem(R.id.action_ingredients);
        MenuItem actionRecipes = menu.findItem(R.id.action_recipes);

        if (actionCategories != null) {
            boolean hasAccess = PermissionHelper.hasAnyPermission(authorities,
                "ROLE_CATEGORY_READ", "ROLE_CATEGORY_CREATE", "ROLE_CATEGORY_SAVE", "ROLE_CATEGORY_DELETE");
            actionCategories.setEnabled(hasAccess);
        }

        if (actionIngredients != null) {
            boolean hasAccess = PermissionHelper.hasAnyPermission(authorities,
                "ROLE_INGREDIENT_READ", "ROLE_INGREDIENT_CREATE", "ROLE_INGREDIENT_SAVE", "ROLE_INGREDIENT_DELETE");
            actionIngredients.setEnabled(hasAccess);
        }

        if (actionRecipes != null) {
            boolean hasAccess = PermissionHelper.hasAnyPermission(authorities,
                "ROLE_RECIPE_READ", "ROLE_RECIPE_CREATE", "ROLE_RECIPE_SAVE", "ROLE_RECIPE_DELETE");
            actionRecipes.setEnabled(hasAccess);
        }
    }

    private void loadData() {
        // Load data from API
        Toast.makeText(this, "Loading data...", Toast.LENGTH_SHORT).show();
    }

    private void loadCompanies() {
        Log.d(TAG, "Loading companies...");
        companyService.getCompanies(0, 20, "").enqueue(new Callback<PageResponse<Company>>() {
            @Override
            public void onResponse(Call<PageResponse<Company>> call, Response<PageResponse<Company>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    PageResponse<Company> pageResponse = response.body();
                    StringBuilder sb = new StringBuilder("Companies:\n");
                    for (Company company : pageResponse.getContent()) {
                        sb.append("- ").append(company.getName()).append("\n");
                    }
                    Toast.makeText(MainActivity.this, sb.toString(), Toast.LENGTH_LONG).show();
                    Log.d(TAG, "Loaded " + pageResponse.getContent().size() + " companies");
                } else {
                    Toast.makeText(MainActivity.this, "Failed to load companies: " + response.message(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Failed to load companies: " + response.code() + " " + response.message());
                }
            }

            @Override
            public void onFailure(Call<PageResponse<Company>> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error loading companies", t);
            }
        });
    }

    private void loadPersons() {
        Log.d(TAG, "Loading persons...");
        personService.getPersons(0, 20, "").enqueue(new Callback<PageResponse<Person>>() {
            @Override
            public void onResponse(Call<PageResponse<Person>> call, Response<PageResponse<Person>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    PageResponse<Person> pageResponse = response.body();
                    StringBuilder sb = new StringBuilder("Persons:\n");
                    for (Person person : pageResponse.getContent()) {
                        sb.append("- ").append(person.getFullName()).append("\n");
                    }
                    Toast.makeText(MainActivity.this, sb.toString(), Toast.LENGTH_LONG).show();
                    Log.d(TAG, "Loaded " + pageResponse.getContent().size() + " persons");
                } else {
                    Toast.makeText(MainActivity.this, "Failed to load persons: " + response.message(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Failed to load persons: " + response.code() + " " + response.message());
                }
            }

            @Override
            public void onFailure(Call<PageResponse<Person>> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error loading persons", t);
            }
        });
    }

    private void loadProducts() {
        Log.d(TAG, "Loading products...");
        productService.getProducts(0, 20, "").enqueue(new Callback<PageResponse<Product>>() {
            @Override
            public void onResponse(Call<PageResponse<Product>> call, Response<PageResponse<Product>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    PageResponse<Product> pageResponse = response.body();
                    StringBuilder sb = new StringBuilder("Products:\n");
                    for (Product product : pageResponse.getContent()) {
                        sb.append("- ").append(product.getName()).append("\n");
                    }
                    Toast.makeText(MainActivity.this, sb.toString(), Toast.LENGTH_LONG).show();
                    Log.d(TAG, "Loaded " + pageResponse.getContent().size() + " products");
                } else {
                    Toast.makeText(MainActivity.this, "Failed to load products: " + response.message(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Failed to load products: " + response.code() + " " + response.message());
                }
            }

            @Override
            public void onFailure(Call<PageResponse<Product>> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error loading products", t);
            }
        });
    }

    private void loadUsers() {
        Log.d(TAG, "Loading users...");
        userService.getUsers(0, 20, "").enqueue(new Callback<PageResponse<User>>() {
            @Override
            public void onResponse(Call<PageResponse<User>> call, Response<PageResponse<User>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    PageResponse<User> pageResponse = response.body();
                    StringBuilder sb = new StringBuilder("Users:\n");
                    for (User user : pageResponse.getContent()) {
                        sb.append("- ").append(user.getUsername());
                        if (user.getEmail() != null) {
                            sb.append(" (").append(user.getEmail()).append(")");
                        }
                        sb.append("\n");
                    }
                    Toast.makeText(MainActivity.this, sb.toString(), Toast.LENGTH_LONG).show();
                    Log.d(TAG, "Loaded " + pageResponse.getContent().size() + " users");
                } else {
                    Toast.makeText(MainActivity.this, "Failed to load users: " + response.message(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Failed to load users: " + response.code() + " " + response.message());
                }
            }

            @Override
            public void onFailure(Call<PageResponse<User>> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error loading users", t);
            }
        });
    }

    private void loadWarehouses() {
        Log.d(TAG, "Loading warehouses...");
        warehouseService.getWarehouses(0, 20, "").enqueue(new Callback<PageResponse<Warehouse>>() {
            @Override
            public void onResponse(Call<PageResponse<Warehouse>> call, Response<PageResponse<Warehouse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    java.util.List<Warehouse> warehouses = response.body().getContent();
                    StringBuilder sb = new StringBuilder("Warehouses:\n");
                    for (Warehouse warehouse : warehouses) {
                        sb.append("- ").append(warehouse.getName())
                          .append(" (Qty: ").append(warehouse.getQuantity())
                          .append(", Price: ").append(warehouse.getPrice())
                          .append(" ").append(warehouse.getCurrency())
                          .append(")\n");
                    }
                    Toast.makeText(MainActivity.this, sb.toString(), Toast.LENGTH_LONG).show();
                    Log.d(TAG, "Loaded " + warehouses.size() + " warehouses");
                } else {
                    Toast.makeText(MainActivity.this, "Failed to load warehouses: " + response.message(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Failed to load warehouses: " + response.code() + " " + response.message());
                }
            }

            @Override
            public void onFailure(Call<PageResponse<Warehouse>> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error loading warehouses", t);
            }
        });
    }

    private void loadStocks() {
        Log.d(TAG, "Loading stocks...");
        stockService.getStocks(0, 20, "").enqueue(new Callback<PageResponse<Stock>>() {
            @Override
            public void onResponse(Call<PageResponse<Stock>> call, Response<PageResponse<Stock>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    java.util.List<Stock> stocks = response.body().getContent();
                    StringBuilder sb = new StringBuilder("Stocks:\n");
                    for (Stock stock : stocks) {
                        sb.append("- ").append(stock.getName())
                          .append(" (Qty: ").append(stock.getQuantity())
                          .append(", Price: ").append(stock.getPrice())
                          .append(" ").append(stock.getCurrency())
                          .append(")\n");
                    }
                    Toast.makeText(MainActivity.this, sb.toString(), Toast.LENGTH_LONG).show();
                    Log.d(TAG, "Loaded " + stocks.size() + " stocks");
                } else {
                    Toast.makeText(MainActivity.this, "Failed to load stocks: " + response.message(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Failed to load stocks: " + response.code() + " " + response.message());
                }
            }

            @Override
            public void onFailure(Call<PageResponse<Stock>> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error loading stocks", t);
            }
        });
    }

    private void startLoginActivity() {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }
}

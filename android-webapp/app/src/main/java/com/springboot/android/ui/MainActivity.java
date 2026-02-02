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
import com.springboot.android.util.SessionManager;

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

        cardCompanies.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CompanyListActivity.class);
            startActivity(intent);
        });

        cardPersons.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, PersonListActivity.class);
            startActivity(intent);
        });

        cardProducts.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ProductListActivity.class);
            startActivity(intent);
        });

        cardUsers.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, UserListActivity.class);
            startActivity(intent);
        });

        cardWarehouses.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, WarehouseListActivity.class);
            startActivity(intent);
        });

        cardStocks.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, StockListActivity.class);
            startActivity(intent);
        });

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(view ->
                Toast.makeText(this, "Add new item", Toast.LENGTH_SHORT).show());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_logout) {
            sessionManager.logout();
            startLoginActivity();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void loadData() {
        // Load data from API
        Toast.makeText(this, "Loading data...", Toast.LENGTH_SHORT).show();
    }

    private void loadCompanies() {
        Log.d(TAG, "Loading companies...");
        companyService.getCompanies(0, 20).enqueue(new Callback<PageResponse<Company>>() {
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
        personService.getPersons(0, 20).enqueue(new Callback<PageResponse<Person>>() {
            @Override
            public void onResponse(Call<PageResponse<Person>> call, Response<PageResponse<Person>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    PageResponse<Person> pageResponse = response.body();
                    StringBuilder sb = new StringBuilder("Persons:\n");
                    for (Person person : pageResponse.getContent()) {
                        sb.append("- ").append(person.getFirstName()).append("\n");
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
        productService.getProducts(0, 20).enqueue(new Callback<PageResponse<Product>>() {
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
        userService.getUsers(0, 20).enqueue(new Callback<PageResponse<User>>() {
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
        warehouseService.getWarehouses(0, 20).enqueue(new Callback<java.util.List<Warehouse>>() {
            @Override
            public void onResponse(Call<java.util.List<Warehouse>> call, Response<java.util.List<Warehouse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    java.util.List<Warehouse> warehouses = response.body();
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
            public void onFailure(Call<java.util.List<Warehouse>> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error loading warehouses", t);
            }
        });
    }

    private void loadStocks() {
        Log.d(TAG, "Loading stocks...");
        stockService.getStocks(0, 20).enqueue(new Callback<java.util.List<Stock>>() {
            @Override
            public void onResponse(Call<java.util.List<Stock>> call, Response<java.util.List<Stock>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    java.util.List<Stock> stocks = response.body();
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
            public void onFailure(Call<java.util.List<Stock>> call, Throwable t) {
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

package com.springboot.android.ui;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.springboot.android.R;
import com.springboot.android.api.ApiClient;
import com.springboot.android.api.ProductService;
import com.springboot.android.model.PageResponse;
import com.springboot.android.model.Product;
import com.springboot.android.util.PaginationHelper;
import com.springboot.android.util.PermissionHelper;
import com.springboot.android.util.SessionManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProductListActivity extends AppCompatActivity {
    private static final String TAG = "ProductListActivity";
    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefresh;
    private TextInputEditText etSearch;
    private MaterialButton btnSearch;
    private ProductService productService;
    private ProductAdapter adapter;
    private PaginationHelper paginationHelper;
    private SessionManager sessionManager;
    private List<String> authorities;
    private int currentPage = 0;
    private String searchQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_list);

        sessionManager = new SessionManager(this);
        authorities = sessionManager.getAuthorities();
        productService = ApiClient.getClient().create(ProductService.class);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Initialize search components
        etSearch = findViewById(R.id.etSearch);
        btnSearch = findViewById(R.id.btnSearch);

        btnSearch.setOnClickListener(v -> performSearch());

        // Allow search on "Enter" key press
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
                performSearch();
                return true;
            }
            return false;
        });

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new ProductAdapter(new ArrayList<>(), this::onEditProduct, this::onDeleteProduct);
        adapter.setPermissions(
            PermissionHelper.hasProductSaveAccess(authorities),
            PermissionHelper.hasProductDeleteAccess(authorities)
        );
        recyclerView.setAdapter(adapter);

        swipeRefresh = findViewById(R.id.swipeRefresh);
        swipeRefresh.setOnRefreshListener(this::loadProducts);

        FloatingActionButton fabAdd = findViewById(R.id.fabAdd);
        if (PermissionHelper.hasProductCreateAccess(authorities)) {
            fabAdd.setVisibility(View.VISIBLE);
            fabAdd.setOnClickListener(v -> {
                Intent intent = new Intent(this, ProductFormActivity.class);
                startActivity(intent);
            });
        } else {
            fabAdd.setVisibility(View.GONE);
        }

        View paginationView = findViewById(R.id.pagination);
        paginationHelper = new PaginationHelper(paginationView, this::onPageChange);
    }

    private void performSearch() {
        searchQuery = etSearch.getText() != null ? etSearch.getText().toString().trim() : "";
        currentPage = 0; // Reset to first page when searching
        loadProducts();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadProducts();
    }

    private void loadProducts() {
        swipeRefresh.setRefreshing(true);
        productService.getProducts(currentPage, 10, searchQuery).enqueue(new Callback<PageResponse<Product>>() {
            @Override
            public void onResponse(Call<PageResponse<Product>> call, Response<PageResponse<Product>> response) {
                swipeRefresh.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null) {
                    PageResponse<Product> pageResponse = response.body();
                    adapter.updateData(pageResponse.getContent());
                    paginationHelper.updatePagination(
                        pageResponse.getNumber(),
                        pageResponse.getTotalPages(),
                        pageResponse.getTotalElements()
                    );
                    Log.d(TAG, "Loaded " + pageResponse.getContent().size() + " products");
                } else {
                    Toast.makeText(ProductListActivity.this, "Failed to load products", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<PageResponse<Product>> call, Throwable t) {
                swipeRefresh.setRefreshing(false);
                Toast.makeText(ProductListActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void onEditProduct(Product product) {
        if (!PermissionHelper.hasProductSaveAccess(authorities)) {
            Toast.makeText(this, "You don't have permission to edit products", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, ProductFormActivity.class);
        intent.putExtra("product_id", product.getId());
        intent.putExtra("product_name", product.getName());
        intent.putExtra("product_description", product.getDescription());
        intent.putExtra("product_price", product.getPrice());
        intent.putExtra("product_quantity", product.getQuantity());
        startActivity(intent);
    }

    private void onDeleteProduct(Product product) {
        if (!PermissionHelper.hasProductDeleteAccess(authorities)) {
            Toast.makeText(this, "You don't have permission to delete products", Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(this)
            .setTitle("Delete Product")
            .setMessage("Are you sure you want to delete " + product.getName() + "?")
            .setPositiveButton("Delete", (dialog, which) -> {
                productService.deleteProduct(product.getId()).enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(ProductListActivity.this, "Deleted successfully", Toast.LENGTH_SHORT).show();
                            loadProducts();
                        } else {
                            Toast.makeText(ProductListActivity.this, "Failed to delete", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        Toast.makeText(ProductListActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void onPageChange(int page) {
        currentPage = page;
        loadProducts();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}

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
import com.springboot.android.api.WarehouseService;
import com.springboot.android.model.PageResponse;
import com.springboot.android.model.Warehouse;
import com.springboot.android.util.PaginationHelper;
import com.springboot.android.util.PermissionHelper;
import com.springboot.android.util.SessionManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WarehouseListActivity extends AppCompatActivity {
    private static final String TAG = "WarehouseListActivity";
    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefresh;
    private WarehouseService warehouseService;
    private WarehouseAdapter adapter;
    private PaginationHelper paginationHelper;
    private SessionManager sessionManager;
    private List<String> authorities;
    private int currentPage = 0;
    private TextInputEditText etSearch;
    private MaterialButton btnSearch;
    private String searchQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_warehouse_list);

        sessionManager = new SessionManager(this);
        authorities = sessionManager.getAuthorities();
        warehouseService = ApiClient.getClient().create(WarehouseService.class);

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

        adapter = new WarehouseAdapter(new ArrayList<>(), this::onEditWarehouse, this::onDeleteWarehouse);
        adapter.setPermissions(
            PermissionHelper.hasWarehouseSaveAccess(authorities),
            PermissionHelper.hasWarehouseDeleteAccess(authorities)
        );
        recyclerView.setAdapter(adapter);

        swipeRefresh = findViewById(R.id.swipeRefresh);
        swipeRefresh.setOnRefreshListener(this::loadWarehouses);

        FloatingActionButton fabAdd = findViewById(R.id.fabAdd);
        if (PermissionHelper.hasWarehouseCreateAccess(authorities)) {
            fabAdd.setVisibility(View.VISIBLE);
            fabAdd.setOnClickListener(v -> {
                Intent intent = new Intent(this, WarehouseFormActivity.class);
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
        loadWarehouses();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadWarehouses();
    }

    private void loadWarehouses() {
        swipeRefresh.setRefreshing(true);
        warehouseService.getWarehouses(currentPage, 10, searchQuery).enqueue(new Callback<PageResponse<Warehouse>>() {
            @Override
            public void onResponse(Call<PageResponse<Warehouse>> call, Response<PageResponse<Warehouse>> response) {
                swipeRefresh.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null) {
                    PageResponse<Warehouse> pageResponse = response.body();
                    adapter.updateData(pageResponse.getContent());
                    paginationHelper.updatePagination(
                        pageResponse.getNumber(),
                        pageResponse.getTotalPages(),
                        pageResponse.getTotalElements()
                    );
                    Log.d(TAG, "Loaded " + pageResponse.getContent().size() + " warehouses");
                } else {
                    Toast.makeText(WarehouseListActivity.this, "Failed to load warehouses", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<PageResponse<Warehouse>> call, Throwable t) {
                swipeRefresh.setRefreshing(false);
                Toast.makeText(WarehouseListActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void onEditWarehouse(Warehouse warehouse) {
        if (!PermissionHelper.hasWarehouseSaveAccess(authorities)) {
            Toast.makeText(this, "You don't have permission to edit warehouses", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, WarehouseFormActivity.class);
        intent.putExtra("warehouse_id", warehouse.getId());
        intent.putExtra("warehouse_name", warehouse.getName());
        intent.putExtra("warehouse_quantity", warehouse.getQuantity());
        intent.putExtra("warehouse_category", warehouse.getCategory());
        intent.putExtra("warehouse_price", warehouse.getPrice());
        intent.putExtra("warehouse_currency", warehouse.getCurrency());
        startActivity(intent);
    }

    private void onDeleteWarehouse(Warehouse warehouse) {
        if (!PermissionHelper.hasWarehouseDeleteAccess(authorities)) {
            Toast.makeText(this, "You don't have permission to delete warehouses", Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(this)
            .setTitle("Delete Warehouse")
            .setMessage("Are you sure you want to delete " + warehouse.getName() + "?")
            .setPositiveButton("Delete", (dialog, which) -> {
                warehouseService.deleteWarehouse(warehouse.getId()).enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(WarehouseListActivity.this, "Deleted successfully", Toast.LENGTH_SHORT).show();
                            loadWarehouses();
                        } else {
                            Toast.makeText(WarehouseListActivity.this, "Failed to delete", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        Toast.makeText(WarehouseListActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void onPageChange(int page) {
        currentPage = page;
        loadWarehouses();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}

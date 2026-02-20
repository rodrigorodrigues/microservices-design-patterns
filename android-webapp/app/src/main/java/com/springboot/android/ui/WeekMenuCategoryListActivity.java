package com.springboot.android.ui;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.springboot.android.R;
import com.springboot.android.api.ApiClient;
import com.springboot.android.api.WeekMenuCategoryService;
import com.springboot.android.model.Category;
import com.springboot.android.model.PageResponse;
import com.springboot.android.util.PermissionHelper;
import com.springboot.android.util.SessionManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WeekMenuCategoryListActivity extends AppCompatActivity {
    private static final String TAG = "WeekMenuCategoryList";
    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefresh;
    private WeekMenuCategoryService categoryService;
    private WeekMenuCategoryAdapter adapter;
    private SessionManager sessionManager;
    private List<String> authorities;
    private int currentPage = 0;
    private static final int PAGE_SIZE = 20;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_week_menu_category_list);

        sessionManager = new SessionManager(this);
        authorities = sessionManager.getAuthorities();
        categoryService = ApiClient.getClient().create(WeekMenuCategoryService.class);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new WeekMenuCategoryAdapter(new ArrayList<>(), this::onEditCategory, this::onDeleteCategory);
        adapter.setPermissions(
            PermissionHelper.hasCategorySaveAccess(authorities),
            PermissionHelper.hasCategoryDeleteAccess(authorities)
        );
        recyclerView.setAdapter(adapter);

        swipeRefresh = findViewById(R.id.swipeRefresh);
        swipeRefresh.setOnRefreshListener(this::loadCategories);

        FloatingActionButton fabAdd = findViewById(R.id.fabAdd);
        if (PermissionHelper.hasCategoryCreateAccess(authorities)) {
            fabAdd.setVisibility(FloatingActionButton.VISIBLE);
            fabAdd.setOnClickListener(v -> {
                Intent intent = new Intent(this, CategoryFormActivity.class);
                startActivity(intent);
            });
        } else {
            fabAdd.setVisibility(FloatingActionButton.GONE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCategories();
    }

    private void loadCategories() {
        swipeRefresh.setRefreshing(true);
        categoryService.getCategories(currentPage, PAGE_SIZE).enqueue(new Callback<PageResponse<Category>>() {
            @Override
            public void onResponse(Call<PageResponse<Category>> call, Response<PageResponse<Category>> response) {
                swipeRefresh.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null) {
                    PageResponse<Category> pageResponse = response.body();
                    adapter.updateData(pageResponse.getContent());
                    Log.d(TAG, "Loaded " + pageResponse.getContent().size() + " categories, page " + pageResponse.getNumber() + " of " + pageResponse.getTotalPages());
                } else {
                    Toast.makeText(WeekMenuCategoryListActivity.this, "Failed to load categories", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<PageResponse<Category>> call, Throwable t) {
                swipeRefresh.setRefreshing(false);
                Toast.makeText(WeekMenuCategoryListActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error loading categories", t);
            }
        });
    }

    private void onEditCategory(Category category) {
        if (!PermissionHelper.hasCategorySaveAccess(authorities)) {
            Toast.makeText(this, "You don't have permission to edit categories", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, CategoryFormActivity.class);
        intent.putExtra("category_id", category.getId());
        intent.putExtra("category_name", category.getName());
        startActivity(intent);
    }

    private void onDeleteCategory(Category category) {
        if (!PermissionHelper.hasCategoryDeleteAccess(authorities)) {
            Toast.makeText(this, "You don't have permission to delete categories", Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(this)
            .setTitle("Delete Category")
            .setMessage("Are you sure you want to delete " + category.getName() + "?")
            .setPositiveButton("Delete", (dialog, which) -> {
                categoryService.deleteCategory(category.getId()).enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(WeekMenuCategoryListActivity.this, "Deleted successfully", Toast.LENGTH_SHORT).show();
                            loadCategories();
                        } else {
                            Toast.makeText(WeekMenuCategoryListActivity.this, "Failed to delete", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        Toast.makeText(WeekMenuCategoryListActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}

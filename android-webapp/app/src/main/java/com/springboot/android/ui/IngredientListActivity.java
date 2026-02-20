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
import com.springboot.android.api.IngredientService;
import com.springboot.android.model.Ingredient;
import com.springboot.android.model.PageResponse;
import com.springboot.android.util.PermissionHelper;
import com.springboot.android.util.SessionManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class IngredientListActivity extends AppCompatActivity {
    private static final String TAG = "IngredientListActivity";
    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefresh;
    private IngredientService ingredientService;
    private IngredientAdapter adapter;
    private SessionManager sessionManager;
    private List<String> authorities;
    private int currentPage = 0;
    private static final int PAGE_SIZE = 20;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ingredient_list);

        sessionManager = new SessionManager(this);
        authorities = sessionManager.getAuthorities();
        ingredientService = ApiClient.getClient().create(IngredientService.class);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new IngredientAdapter(new ArrayList<>(), this::onEditIngredient, this::onDeleteIngredient);
        adapter.setPermissions(
            PermissionHelper.hasIngredientSaveAccess(authorities),
            PermissionHelper.hasIngredientDeleteAccess(authorities)
        );
        recyclerView.setAdapter(adapter);

        swipeRefresh = findViewById(R.id.swipeRefresh);
        swipeRefresh.setOnRefreshListener(this::loadIngredients);

        FloatingActionButton fabAdd = findViewById(R.id.fabAdd);
        if (PermissionHelper.hasIngredientCreateAccess(authorities)) {
            fabAdd.setVisibility(FloatingActionButton.VISIBLE);
            fabAdd.setOnClickListener(v -> {
                Intent intent = new Intent(this, IngredientFormActivity.class);
                startActivity(intent);
            });
        } else {
            fabAdd.setVisibility(FloatingActionButton.GONE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadIngredients();
    }

    private void loadIngredients() {
        swipeRefresh.setRefreshing(true);
        ingredientService.getIngredients(currentPage, PAGE_SIZE).enqueue(new Callback<PageResponse<Ingredient>>() {
            @Override
            public void onResponse(Call<PageResponse<Ingredient>> call, Response<PageResponse<Ingredient>> response) {
                swipeRefresh.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null) {
                    PageResponse<Ingredient> pageResponse = response.body();
                    adapter.updateData(pageResponse.getContent());
                    Log.d(TAG, "Loaded " + pageResponse.getContent().size() + " ingredients, page " + pageResponse.getNumber() + " of " + pageResponse.getTotalPages());
                } else {
                    Toast.makeText(IngredientListActivity.this, "Failed to load ingredients", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<PageResponse<Ingredient>> call, Throwable t) {
                swipeRefresh.setRefreshing(false);
                Toast.makeText(IngredientListActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error loading ingredients", t);
            }
        });
    }

    private void onEditIngredient(Ingredient ingredient) {
        if (!PermissionHelper.hasIngredientSaveAccess(authorities)) {
            Toast.makeText(this, "You don't have permission to edit ingredients", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, IngredientFormActivity.class);
        intent.putExtra("ingredient_id", ingredient.getId());
        intent.putExtra("ingredient_name", ingredient.getName());
        intent.putExtra("ingredient_category_name", ingredient.getCategoryName());
        startActivity(intent);
    }

    private void onDeleteIngredient(Ingredient ingredient) {
        if (!PermissionHelper.hasIngredientDeleteAccess(authorities)) {
            Toast.makeText(this, "You don't have permission to delete ingredients", Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(this)
            .setTitle("Delete Ingredient")
            .setMessage("Are you sure you want to delete " + ingredient.getName() + "?")
            .setPositiveButton("Delete", (dialog, which) -> {
                ingredientService.deleteIngredient(ingredient.getId()).enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(IngredientListActivity.this, "Deleted successfully", Toast.LENGTH_SHORT).show();
                            loadIngredients();
                        } else {
                            Toast.makeText(IngredientListActivity.this, "Failed to delete", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        Toast.makeText(IngredientListActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
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

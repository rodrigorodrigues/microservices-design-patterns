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
import com.springboot.android.api.RecipeService;
import com.springboot.android.model.PageResponse;
import com.springboot.android.model.Recipe;
import com.springboot.android.util.PermissionHelper;
import com.springboot.android.util.SessionManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RecipeListActivity extends AppCompatActivity {
    private static final String TAG = "RecipeListActivity";
    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefresh;
    private RecipeService recipeService;
    private RecipeAdapter adapter;
    private SessionManager sessionManager;
    private List<String> authorities;
    private int currentPage = 0;
    private static final int PAGE_SIZE = 20;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipe_list);

        sessionManager = new SessionManager(this);
        authorities = sessionManager.getAuthorities();
        recipeService = ApiClient.getClient().create(RecipeService.class);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new RecipeAdapter(new ArrayList<>(), this::onEditRecipe, this::onDeleteRecipe);
        adapter.setPermissions(
            PermissionHelper.hasRecipeSaveAccess(authorities),
            PermissionHelper.hasRecipeDeleteAccess(authorities)
        );
        recyclerView.setAdapter(adapter);

        swipeRefresh = findViewById(R.id.swipeRefresh);
        swipeRefresh.setOnRefreshListener(this::loadRecipes);

        FloatingActionButton fabAdd = findViewById(R.id.fabAdd);
        if (PermissionHelper.hasRecipeCreateAccess(authorities)) {
            fabAdd.setVisibility(FloatingActionButton.VISIBLE);
            fabAdd.setOnClickListener(v -> {
                Intent intent = new Intent(this, RecipeFormActivity.class);
                startActivity(intent);
            });
        } else {
            fabAdd.setVisibility(FloatingActionButton.GONE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadRecipes();
    }

    private void loadRecipes() {
        swipeRefresh.setRefreshing(true);
        recipeService.getRecipes(currentPage, PAGE_SIZE).enqueue(new Callback<PageResponse<Recipe>>() {
            @Override
            public void onResponse(Call<PageResponse<Recipe>> call, Response<PageResponse<Recipe>> response) {
                swipeRefresh.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null) {
                    PageResponse<Recipe> pageResponse = response.body();
                    adapter.updateData(pageResponse.getContent());
                    Log.d(TAG, "Loaded " + pageResponse.getContent().size() + " recipes, page " + pageResponse.getNumber() + " of " + pageResponse.getTotalPages());
                } else {
                    Toast.makeText(RecipeListActivity.this, "Failed to load recipes", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<PageResponse<Recipe>> call, Throwable t) {
                swipeRefresh.setRefreshing(false);
                Toast.makeText(RecipeListActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error loading recipes", t);
            }
        });
    }

    private void onEditRecipe(Recipe recipe) {
        if (!PermissionHelper.hasRecipeSaveAccess(authorities)) {
            Toast.makeText(this, "You don't have permission to edit recipes", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, RecipeFormActivity.class);
        intent.putExtra("recipe_id", recipe.getId());
        intent.putExtra("recipe_name", recipe.getName());
        intent.putExtra("recipe_description", recipe.getDescription());
        startActivity(intent);
    }

    private void onDeleteRecipe(Recipe recipe) {
        if (!PermissionHelper.hasRecipeDeleteAccess(authorities)) {
            Toast.makeText(this, "You don't have permission to delete recipes", Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(this)
            .setTitle("Delete Recipe")
            .setMessage("Are you sure you want to delete " + recipe.getName() + "?")
            .setPositiveButton("Delete", (dialog, which) -> {
                recipeService.deleteRecipe(recipe.getId()).enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(RecipeListActivity.this, "Deleted successfully", Toast.LENGTH_SHORT).show();
                            loadRecipes();
                        } else {
                            Toast.makeText(RecipeListActivity.this, "Failed to delete", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        Toast.makeText(RecipeListActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
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

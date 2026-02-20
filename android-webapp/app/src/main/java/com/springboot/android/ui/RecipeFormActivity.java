package com.springboot.android.ui;

import android.os.Bundle;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.springboot.android.R;
import com.springboot.android.api.ApiClient;
import com.springboot.android.api.RecipeService;
import com.springboot.android.model.Recipe;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RecipeFormActivity extends AppCompatActivity {
    private TextInputEditText etName, etDescription;
    private MaterialButton btnSave;
    private ProgressBar progressBar;
    private RecipeService recipeService;
    private String recipeId;
    private boolean isEditMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipe_form);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        recipeService = ApiClient.getClient().create(RecipeService.class);

        etName = findViewById(R.id.etName);
        etDescription = findViewById(R.id.etDescription);
        btnSave = findViewById(R.id.btnSave);
        progressBar = findViewById(R.id.progressBar);

        // Check if editing existing recipe
        recipeId = getIntent().getStringExtra("recipe_id");
        isEditMode = recipeId != null;

        if (isEditMode) {
            getSupportActionBar().setTitle("Edit Recipe");
            loadRecipeData();
        } else {
            getSupportActionBar().setTitle("Add Recipe");
        }

        btnSave.setOnClickListener(v -> saveRecipe());
    }

    private void loadRecipeData() {
        String name = getIntent().getStringExtra("recipe_name");
        String description = getIntent().getStringExtra("recipe_description");

        if (name != null) {
            etName.setText(name);
        }
        if (description != null) {
            etDescription.setText(description);
        }
    }

    private void saveRecipe() {
        String name = etName.getText().toString().trim();
        String description = etDescription.getText().toString().trim();

        if (name.isEmpty()) {
            Toast.makeText(this, "Name is required", Toast.LENGTH_SHORT).show();
            return;
        }

        Recipe recipe = new Recipe();
        recipe.setName(name);
        recipe.setDescription(description);

        progressBar.setVisibility(ProgressBar.VISIBLE);
        btnSave.setEnabled(false);

        Call<Recipe> call;
        if (isEditMode) {
            recipe.setId(recipeId);
            call = recipeService.updateRecipe(recipeId, recipe);
        } else {
            call = recipeService.createRecipe(recipe);
        }

        call.enqueue(new Callback<Recipe>() {
            @Override
            public void onResponse(Call<Recipe> call, Response<Recipe> response) {
                progressBar.setVisibility(ProgressBar.GONE);
                btnSave.setEnabled(true);

                if (response.isSuccessful()) {
                    Toast.makeText(RecipeFormActivity.this,
                        isEditMode ? "Recipe updated successfully" : "Recipe created successfully",
                        Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(RecipeFormActivity.this, "Failed to save recipe", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Recipe> call, Throwable t) {
                progressBar.setVisibility(ProgressBar.GONE);
                btnSave.setEnabled(true);
                Toast.makeText(RecipeFormActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}

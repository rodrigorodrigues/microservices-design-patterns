package com.springboot.android.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.springboot.android.R;
import com.springboot.android.api.ApiClient;
import com.springboot.android.api.IngredientService;
import com.springboot.android.model.Ingredient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class IngredientFormActivity extends AppCompatActivity {
    private TextInputEditText etName, etCategoryName;
    private MaterialButton btnSave;
    private ProgressBar progressBar;
    private IngredientService ingredientService;
    private String ingredientId;
    private boolean isEditMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ingredient_form);

        ingredientService = ApiClient.getClient().create(IngredientService.class);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        etName = findViewById(R.id.etName);
        etCategoryName = findViewById(R.id.etCategoryName);
        btnSave = findViewById(R.id.btnSave);
        progressBar = findViewById(R.id.progressBar);

        // Check if editing existing ingredient
        ingredientId = getIntent().getStringExtra("ingredient_id");
        isEditMode = ingredientId != null;

        if (isEditMode) {
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Edit Ingredient");
            }
            loadIngredientData();
        } else {
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Add Ingredient");
            }
        }

        btnSave.setOnClickListener(v -> saveIngredient());
    }

    private void loadIngredientData() {
        if (etName != null) etName.setText(getIntent().getStringExtra("ingredient_name"));
        if (etCategoryName != null) etCategoryName.setText(getIntent().getStringExtra("ingredient_category_name"));
    }

    private void saveIngredient() {
        String name = etName.getText() != null ? etName.getText().toString().trim() : "";
        String categoryName = etCategoryName.getText() != null ? etCategoryName.getText().toString().trim() : "";

        if (name.isEmpty()) {
            Toast.makeText(this, "Name is required", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSave.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);

        Ingredient ingredient = new Ingredient();
        ingredient.setName(name);
        ingredient.setCategoryName(categoryName);

        Call<Ingredient> call;
        if (isEditMode) {
            ingredient.setId(ingredientId);
            call = ingredientService.updateIngredient(ingredientId, ingredient);
        } else {
            call = ingredientService.createIngredient(ingredient);
        }

        call.enqueue(new Callback<Ingredient>() {
            @Override
            public void onResponse(Call<Ingredient> call, Response<Ingredient> response) {
                btnSave.setEnabled(true);
                progressBar.setVisibility(View.GONE);

                if (response.isSuccessful()) {
                    Toast.makeText(IngredientFormActivity.this,
                            isEditMode ? "Ingredient updated successfully" : "Ingredient created successfully",
                            Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(IngredientFormActivity.this,
                            "Failed to save ingredient", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Ingredient> call, Throwable t) {
                btnSave.setEnabled(true);
                progressBar.setVisibility(View.GONE);
                Toast.makeText(IngredientFormActivity.this,
                        "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}

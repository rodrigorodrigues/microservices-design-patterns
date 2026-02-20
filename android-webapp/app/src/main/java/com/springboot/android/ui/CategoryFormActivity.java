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
import com.springboot.android.api.WeekMenuCategoryService;
import com.springboot.android.model.Category;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CategoryFormActivity extends AppCompatActivity {
    private TextInputEditText etName;
    private MaterialButton btnSave;
    private ProgressBar progressBar;
    private WeekMenuCategoryService categoryService;
    private String categoryId;
    private boolean isEditMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_form);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        categoryService = ApiClient.getClient().create(WeekMenuCategoryService.class);

        etName = findViewById(R.id.etName);
        btnSave = findViewById(R.id.btnSave);
        progressBar = findViewById(R.id.progressBar);

        // Check if editing existing category
        categoryId = getIntent().getStringExtra("category_id");
        isEditMode = categoryId != null;

        if (isEditMode) {
            getSupportActionBar().setTitle("Edit Category");
            loadCategoryData();
        } else {
            getSupportActionBar().setTitle("Add Category");
        }

        btnSave.setOnClickListener(v -> saveCategory());
    }

    private void loadCategoryData() {
        String name = getIntent().getStringExtra("category_name");

        if (name != null) {
            etName.setText(name);
        }
    }

    private void saveCategory() {
        String name = etName.getText().toString().trim();

        if (name.isEmpty()) {
            Toast.makeText(this, "Name is required", Toast.LENGTH_SHORT).show();
            return;
        }

        Category category = new Category();
        category.setName(name);

        progressBar.setVisibility(ProgressBar.VISIBLE);
        btnSave.setEnabled(false);

        Call<Category> call;
        if (isEditMode) {
            category.setId(categoryId);
            call = categoryService.updateCategory(categoryId, category);
        } else {
            call = categoryService.createCategory(category);
        }

        call.enqueue(new Callback<Category>() {
            @Override
            public void onResponse(Call<Category> call, Response<Category> response) {
                progressBar.setVisibility(ProgressBar.GONE);
                btnSave.setEnabled(true);

                if (response.isSuccessful()) {
                    Toast.makeText(CategoryFormActivity.this,
                        isEditMode ? "Category updated successfully" : "Category created successfully",
                        Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(CategoryFormActivity.this, "Failed to save category", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Category> call, Throwable t) {
                progressBar.setVisibility(ProgressBar.GONE);
                btnSave.setEnabled(true);
                Toast.makeText(CategoryFormActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}

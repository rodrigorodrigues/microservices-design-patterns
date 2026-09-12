package com.springboot.android.ui;

import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.springboot.android.R;
import com.springboot.android.api.ApiClient;
import com.springboot.android.api.ProductService;
import com.springboot.android.api.WeekMenuCategoryService;
import com.springboot.android.model.Category;
import com.springboot.android.model.CategoryProduct;
import com.springboot.android.model.PageResponse;
import com.springboot.android.model.Product;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CategoryFormActivity extends AppCompatActivity {
    private TextInputEditText etName;
    private LinearLayout productsContainer;
    private MaterialButton btnSave;
    private ProgressBar progressBar;
    private WeekMenuCategoryService categoryService;
    private ProductService productService;
    private String categoryId;
    private boolean isEditMode;
    private final Map<String, CheckBox> productCheckBoxes = new HashMap<>();
    private List<String> existingProductNames = new ArrayList<>();

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
        productService = ApiClient.getClient().create(ProductService.class);

        etName = findViewById(R.id.etName);
        productsContainer = findViewById(R.id.productsContainer);
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
        loadProducts();
    }

    private void loadCategoryData() {
        String name = getIntent().getStringExtra("category_name");

        if (name != null) {
            etName.setText(name);
        }

        List<String> passedNames = getIntent().getStringArrayListExtra("category_product_names");
        if (passedNames != null) {
            existingProductNames = passedNames;
        }
    }

    private void loadProducts() {
        productService.getProducts(0, 100, null).enqueue(new Callback<PageResponse<Product>>() {
            @Override
            public void onResponse(Call<PageResponse<Product>> call, Response<PageResponse<Product>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    createProductCheckboxes(response.body().getContent());
                } else {
                    Toast.makeText(CategoryFormActivity.this, "Failed to load products", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<PageResponse<Product>> call, Throwable t) {
                Toast.makeText(CategoryFormActivity.this, "Error loading products: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void createProductCheckboxes(List<Product> products) {
        productsContainer.removeAllViews();
        productCheckBoxes.clear();

        for (Product product : products) {
            CheckBox checkBox = new CheckBox(this);
            checkBox.setText(product.getName());
            checkBox.setChecked(existingProductNames.contains(product.getName()));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            checkBox.setLayoutParams(params);
            productsContainer.addView(checkBox);
            productCheckBoxes.put(product.getName(), checkBox);
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

        List<CategoryProduct> selectedProducts = new ArrayList<>();
        for (Map.Entry<String, CheckBox> entry : productCheckBoxes.entrySet()) {
            if (entry.getValue().isChecked()) {
                CategoryProduct categoryProduct = new CategoryProduct();
                categoryProduct.setName(entry.getKey());
                categoryProduct.setQuantity(1);
                categoryProduct.setCompleted(false);
                selectedProducts.add(categoryProduct);
            }
        }
        category.setProducts(selectedProducts);

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

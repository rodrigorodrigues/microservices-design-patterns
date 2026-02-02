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
import com.springboot.android.api.WarehouseService;
import com.springboot.android.model.Warehouse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WarehouseFormActivity extends AppCompatActivity {
    private TextInputEditText etName, etQuantity, etCategory, etPrice, etCurrency;
    private MaterialButton btnSave;
    private ProgressBar progressBar;
    private WarehouseService warehouseService;
    private String warehouseId;
    private boolean isEditMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_warehouse_form);

        warehouseService = ApiClient.getClient().create(WarehouseService.class);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        etName = findViewById(R.id.etName);
        etQuantity = findViewById(R.id.etQuantity);
        etCategory = findViewById(R.id.etCategory);
        etPrice = findViewById(R.id.etPrice);
        etCurrency = findViewById(R.id.etCurrency);
        btnSave = findViewById(R.id.btnSave);
        progressBar = findViewById(R.id.progressBar);

        // Check if editing existing warehouse
        warehouseId = getIntent().getStringExtra("warehouse_id");
        isEditMode = warehouseId != null;

        if (isEditMode) {
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Edit Warehouse");
            }
            loadWarehouseData();
        } else {
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Add Warehouse");
            }
        }

        btnSave.setOnClickListener(v -> saveWarehouse());
    }

    private void loadWarehouseData() {
        if (etName != null) etName.setText(getIntent().getStringExtra("warehouse_name"));
        if (etCategory != null) etCategory.setText(getIntent().getStringExtra("warehouse_category"));
        if (etCurrency != null) etCurrency.setText(getIntent().getStringExtra("warehouse_currency"));

        int quantity = getIntent().getIntExtra("warehouse_quantity", 0);
        if (etQuantity != null) etQuantity.setText(String.valueOf(quantity));

        double price = getIntent().getDoubleExtra("warehouse_price", 0.0);
        if (etPrice != null) etPrice.setText(String.valueOf(price));
    }

    private void saveWarehouse() {
        String name = etName.getText() != null ? etName.getText().toString().trim() : "";
        String category = etCategory.getText() != null ? etCategory.getText().toString().trim() : "";
        String currency = etCurrency.getText() != null ? etCurrency.getText().toString().trim() : "";
        String quantityStr = etQuantity.getText() != null ? etQuantity.getText().toString().trim() : "";
        String priceStr = etPrice.getText() != null ? etPrice.getText().toString().trim() : "";

        if (name.isEmpty()) {
            Toast.makeText(this, "Name is required", Toast.LENGTH_SHORT).show();
            return;
        }

        int quantity = 0;
        if (!quantityStr.isEmpty()) {
            try {
                quantity = Integer.parseInt(quantityStr);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid quantity", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        double price = 0.0;
        if (!priceStr.isEmpty()) {
            try {
                price = Double.parseDouble(priceStr);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid price", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        btnSave.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);

        Warehouse warehouse = new Warehouse();
        warehouse.setName(name);
        warehouse.setQuantity(quantity);
        warehouse.setCategory(category);
        warehouse.setPrice(price);
        warehouse.setCurrency(currency);

        Call<Warehouse> call = isEditMode ?
            warehouseService.updateWarehouse(warehouseId, warehouse) :
            warehouseService.createWarehouse(warehouse);

        call.enqueue(new Callback<Warehouse>() {
            @Override
            public void onResponse(Call<Warehouse> call, Response<Warehouse> response) {
                btnSave.setEnabled(true);
                progressBar.setVisibility(View.GONE);

                if (response.isSuccessful()) {
                    Toast.makeText(WarehouseFormActivity.this,
                        isEditMode ? "Updated successfully" : "Created successfully",
                        Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(WarehouseFormActivity.this,
                        "Failed: " + response.message(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Warehouse> call, Throwable t) {
                btnSave.setEnabled(true);
                progressBar.setVisibility(View.GONE);
                Toast.makeText(WarehouseFormActivity.this,
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

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
import com.springboot.android.api.CompanyService;
import com.springboot.android.model.Company;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CompanyFormActivity extends AppCompatActivity {
    private TextInputEditText etName, etEmail, etPhone, etAddress;
    private MaterialButton btnSave;
    private ProgressBar progressBar;
    private CompanyService companyService;
    private String companyId;
    private boolean isEditMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_company_form);

        companyService = ApiClient.getClient().create(CompanyService.class);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        etAddress = findViewById(R.id.etAddress);
        btnSave = findViewById(R.id.btnSave);
        progressBar = findViewById(R.id.progressBar);

        // Check if editing existing company
        companyId = getIntent().getStringExtra("company_id");
        isEditMode = companyId != null;

        if (isEditMode) {
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Edit Company");
            }
            loadCompanyData();
        } else {
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Add Company");
            }
        }

        btnSave.setOnClickListener(v -> saveCompany());
    }

    private void loadCompanyData() {
        if (etName != null) etName.setText(getIntent().getStringExtra("company_name"));
        if (etEmail != null) etEmail.setText(getIntent().getStringExtra("company_email"));
        if (etPhone != null) etPhone.setText(getIntent().getStringExtra("company_phone"));
        if (etAddress != null) etAddress.setText(getIntent().getStringExtra("company_address"));
    }

    private void saveCompany() {
        String name = etName.getText() != null ? etName.getText().toString().trim() : "";
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String phone = etPhone.getText() != null ? etPhone.getText().toString().trim() : "";
        String address = etAddress.getText() != null ? etAddress.getText().toString().trim() : "";

        if (name.isEmpty()) {
            Toast.makeText(this, "Name is required", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSave.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);

        Company company = new Company();
        company.setName(name);
        company.setEmail(email);
        company.setPhone(phone);
        company.setAddress(address);

        Call<Company> call = isEditMode ?
            companyService.updateCompany(companyId, company) :
            companyService.createCompany(company);

        call.enqueue(new Callback<Company>() {
            @Override
            public void onResponse(Call<Company> call, Response<Company> response) {
                btnSave.setEnabled(true);
                progressBar.setVisibility(View.GONE);

                if (response.isSuccessful()) {
                    Toast.makeText(CompanyFormActivity.this,
                        isEditMode ? "Updated successfully" : "Created successfully",
                        Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(CompanyFormActivity.this,
                        "Failed: " + response.message(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Company> call, Throwable t) {
                btnSave.setEnabled(true);
                progressBar.setVisibility(View.GONE);
                Toast.makeText(CompanyFormActivity.this,
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

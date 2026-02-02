package com.springboot.android.ui;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.springboot.android.R;
import com.springboot.android.api.ApiClient;
import com.springboot.android.api.CompanyService;
import com.springboot.android.model.Company;
import com.springboot.android.model.PageResponse;
import com.springboot.android.util.PaginationHelper;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CompanyListActivity extends AppCompatActivity {
    private static final String TAG = "CompanyListActivity";
    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefresh;
    private CompanyService companyService;
    private CompanyAdapter adapter;
    private PaginationHelper paginationHelper;
    private int currentPage = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_company_list);

        companyService = ApiClient.getClient().create(CompanyService.class);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new CompanyAdapter(new ArrayList<>(), this::onEditCompany, this::onDeleteCompany);
        recyclerView.setAdapter(adapter);

        swipeRefresh = findViewById(R.id.swipeRefresh);
        swipeRefresh.setOnRefreshListener(this::loadCompanies);

        FloatingActionButton fabAdd = findViewById(R.id.fabAdd);
        fabAdd.setOnClickListener(v -> {
            Intent intent = new Intent(this, CompanyFormActivity.class);
            startActivity(intent);
        });

        View paginationView = findViewById(R.id.pagination);
        paginationHelper = new PaginationHelper(paginationView, this::onPageChange);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCompanies();
    }

    private void loadCompanies() {
        swipeRefresh.setRefreshing(true);
        companyService.getCompanies(currentPage, 10).enqueue(new Callback<PageResponse<Company>>() {
            @Override
            public void onResponse(Call<PageResponse<Company>> call, Response<PageResponse<Company>> response) {
                swipeRefresh.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null) {
                    PageResponse<Company> pageResponse = response.body();
                    adapter.updateData(pageResponse.getContent());
                    paginationHelper.updatePagination(
                        pageResponse.getNumber(),
                        pageResponse.getTotalPages(),
                        pageResponse.getTotalElements()
                    );
                    Log.d(TAG, "Loaded " + pageResponse.getContent().size() + " companies");
                } else {
                    Toast.makeText(CompanyListActivity.this, "Failed to load companies", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<PageResponse<Company>> call, Throwable t) {
                swipeRefresh.setRefreshing(false);
                Toast.makeText(CompanyListActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void onEditCompany(Company company) {
        Intent intent = new Intent(this, CompanyFormActivity.class);
        intent.putExtra("company_id", company.getId());
        intent.putExtra("company_name", company.getName());
        intent.putExtra("company_email", company.getEmail());
        intent.putExtra("company_phone", company.getPhone());
        intent.putExtra("company_address", company.getAddress());
        startActivity(intent);
    }

    private void onDeleteCompany(Company company) {
        new AlertDialog.Builder(this)
            .setTitle("Delete Company")
            .setMessage("Are you sure you want to delete " + company.getName() + "?")
            .setPositiveButton("Delete", (dialog, which) -> {
                companyService.deleteCompany(company.getId()).enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(CompanyListActivity.this, "Deleted successfully", Toast.LENGTH_SHORT).show();
                            loadCompanies();
                        } else {
                            Toast.makeText(CompanyListActivity.this, "Failed to delete", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        Toast.makeText(CompanyListActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void onPageChange(int page) {
        currentPage = page;
        loadCompanies();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}

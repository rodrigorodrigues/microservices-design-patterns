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
import com.springboot.android.api.StockService;
import com.springboot.android.model.PageResponse;
import com.springboot.android.model.Stock;
import com.springboot.android.util.PaginationHelper;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StockListActivity extends AppCompatActivity {
    private static final String TAG = "StockListActivity";
    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefresh;
    private StockService stockService;
    private StockAdapter adapter;
    private PaginationHelper paginationHelper;
    private int currentPage = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stock_list);

        stockService = ApiClient.getClient().create(StockService.class);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new StockAdapter(new ArrayList<>(), this::onEditStock, this::onDeleteStock);
        recyclerView.setAdapter(adapter);

        swipeRefresh = findViewById(R.id.swipeRefresh);
        swipeRefresh.setOnRefreshListener(this::loadStocks);

        FloatingActionButton fabAdd = findViewById(R.id.fabAdd);
        fabAdd.setOnClickListener(v -> {
            Intent intent = new Intent(this, StockFormActivity.class);
            startActivity(intent);
        });

        View paginationView = findViewById(R.id.pagination);
        paginationHelper = new PaginationHelper(paginationView, this::onPageChange);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadStocks();
    }

    private void loadStocks() {
        swipeRefresh.setRefreshing(true);
        stockService.getStocks(currentPage, 10).enqueue(new Callback<PageResponse<Stock>>() {
            @Override
            public void onResponse(Call<PageResponse<Stock>> call, Response<PageResponse<Stock>> response) {
                swipeRefresh.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null) {
                    PageResponse<Stock> pageResponse = response.body();
                    adapter.updateData(pageResponse.getContent());
                    paginationHelper.updatePagination(
                        pageResponse.getNumber(),
                        pageResponse.getTotalPages(),
                        pageResponse.getTotalElements()
                    );
                    Log.d(TAG, "Loaded " + pageResponse.getContent().size() + " stocks");
                } else {
                    Toast.makeText(StockListActivity.this, "Failed to load stocks", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<PageResponse<Stock>> call, Throwable t) {
                swipeRefresh.setRefreshing(false);
                Toast.makeText(StockListActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void onEditStock(Stock stock) {
        Intent intent = new Intent(this, StockFormActivity.class);
        intent.putExtra("stock_id", stock.getId());
        intent.putExtra("stock_name", stock.getName());
        intent.putExtra("stock_quantity", stock.getQuantity());
        intent.putExtra("stock_category", stock.getCategory());
        intent.putExtra("stock_price", stock.getPrice());
        intent.putExtra("stock_currency", stock.getCurrency());
        startActivity(intent);
    }

    private void onDeleteStock(Stock stock) {
        new AlertDialog.Builder(this)
            .setTitle("Delete Stock")
            .setMessage("Are you sure you want to delete " + stock.getName() + "?")
            .setPositiveButton("Delete", (dialog, which) -> {
                stockService.deleteStock(stock.getId()).enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(StockListActivity.this, "Deleted successfully", Toast.LENGTH_SHORT).show();
                            loadStocks();
                        } else {
                            Toast.makeText(StockListActivity.this, "Failed to delete", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        Toast.makeText(StockListActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void onPageChange(int page) {
        currentPage = page;
        loadStocks();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}

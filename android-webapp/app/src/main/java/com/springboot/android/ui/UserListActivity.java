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
import com.springboot.android.api.UserService;
import com.springboot.android.model.PageResponse;
import com.springboot.android.model.User;
import com.springboot.android.util.PaginationHelper;

import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UserListActivity extends AppCompatActivity {
    private static final String TAG = "UserListActivity";
    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefresh;
    private UserService userService;
    private UserAdapter adapter;
    private PaginationHelper paginationHelper;
    private int currentPage = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_list);

        userService = ApiClient.getClient().create(UserService.class);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new UserAdapter(new ArrayList<>(), this::onEditUser, this::onDeleteUser);
        recyclerView.setAdapter(adapter);

        swipeRefresh = findViewById(R.id.swipeRefresh);
        swipeRefresh.setOnRefreshListener(this::loadUsers);

        FloatingActionButton fabAdd = findViewById(R.id.fabAdd);
        fabAdd.setOnClickListener(v -> {
            Intent intent = new Intent(this, UserFormActivity.class);
            startActivity(intent);
        });

        View paginationView = findViewById(R.id.pagination);
        paginationHelper = new PaginationHelper(paginationView, this::onPageChange);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUsers();
    }

    private void loadUsers() {
        swipeRefresh.setRefreshing(true);
        userService.getUsers(currentPage, 10).enqueue(new Callback<PageResponse<User>>() {
            @Override
            public void onResponse(Call<PageResponse<User>> call, Response<PageResponse<User>> response) {
                swipeRefresh.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null) {
                    PageResponse<User> pageResponse = response.body();
                    adapter.updateData(pageResponse.getContent());
                    paginationHelper.updatePagination(
                        pageResponse.getNumber(),
                        pageResponse.getTotalPages(),
                        pageResponse.getTotalElements()
                    );
                    Log.d(TAG, "Loaded " + pageResponse.getContent().size() + " users");
                } else {
                    Toast.makeText(UserListActivity.this, "Failed to load users", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<PageResponse<User>> call, Throwable t) {
                swipeRefresh.setRefreshing(false);
                Toast.makeText(UserListActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void onEditUser(User user) {
        Intent intent = new Intent(this, UserFormActivity.class);
        intent.putExtra("user_id", user.getId());
        intent.putExtra("user_username", user.getUsername());
        intent.putExtra("user_email", user.getEmail());
        intent.putExtra("user_full_name", user.getFullName());
        intent.putExtra("user_activated", user.isActivated());
        startActivity(intent);
    }

    private void onDeleteUser(User user) {
        new AlertDialog.Builder(this)
            .setTitle("Delete User")
            .setMessage("Are you sure you want to delete " + user.getUsername() + "?")
            .setPositiveButton("Delete", (dialog, which) -> {
                userService.deleteUser(user.getId()).enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(UserListActivity.this, "Deleted successfully", Toast.LENGTH_SHORT).show();
                            loadUsers();
                        } else {
                            Toast.makeText(UserListActivity.this, "Failed to delete", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        Toast.makeText(UserListActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void onPageChange(int page) {
        currentPage = page;
        loadUsers();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}

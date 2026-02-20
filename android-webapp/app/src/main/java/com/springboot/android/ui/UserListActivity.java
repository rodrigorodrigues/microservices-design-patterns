package com.springboot.android.ui;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.springboot.android.R;
import com.springboot.android.api.ApiClient;
import com.springboot.android.api.UserService;
import com.springboot.android.model.PageResponse;
import com.springboot.android.model.User;
import com.springboot.android.util.PaginationHelper;
import com.springboot.android.util.PermissionHelper;
import com.springboot.android.util.SessionManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UserListActivity extends AppCompatActivity {
    private static final String TAG = "UserListActivity";
    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefresh;
    private TextInputEditText etSearch;
    private MaterialButton btnSearch;
    private UserService userService;
    private UserAdapter adapter;
    private PaginationHelper paginationHelper;
    private SessionManager sessionManager;
    private List<String> authorities;
    private int currentPage = 0;
    private String searchQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_list);

        sessionManager = new SessionManager(this);
        authorities = sessionManager.getAuthorities();
        userService = ApiClient.getClient().create(UserService.class);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Initialize search components
        etSearch = findViewById(R.id.etSearch);
        btnSearch = findViewById(R.id.btnSearch);

        btnSearch.setOnClickListener(v -> performSearch());

        // Allow search on "Enter" key press
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
                performSearch();
                return true;
            }
            return false;
        });

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new UserAdapter(new ArrayList<>(), this::onEditUser, this::onDeleteUser);
        adapter.setPermissions(
            PermissionHelper.hasUserSaveAccess(authorities),
            PermissionHelper.hasUserDeleteAccess(authorities)
        );
        recyclerView.setAdapter(adapter);

        swipeRefresh = findViewById(R.id.swipeRefresh);
        swipeRefresh.setOnRefreshListener(this::loadUsers);

        FloatingActionButton fabAdd = findViewById(R.id.fabAdd);
        if (PermissionHelper.hasUserCreateAccess(authorities)) {
            fabAdd.setVisibility(View.VISIBLE);
            fabAdd.setOnClickListener(v -> {
                Intent intent = new Intent(this, UserFormActivity.class);
                startActivity(intent);
            });
        } else {
            fabAdd.setVisibility(View.GONE);
        }

        View paginationView = findViewById(R.id.pagination);
        paginationHelper = new PaginationHelper(paginationView, this::onPageChange);
    }

    private void performSearch() {
        searchQuery = etSearch.getText() != null ? etSearch.getText().toString().trim() : "";
        currentPage = 0; // Reset to first page when searching
        loadUsers();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUsers();
    }

    private void loadUsers() {
        swipeRefresh.setRefreshing(true);
        userService.getUsers(currentPage, 10, searchQuery).enqueue(new Callback<PageResponse<User>>() {
            @Override
            public void onResponse(Call<PageResponse<User>> call, Response<PageResponse<User>> response) {
                swipeRefresh.setRefreshing(false);
                Log.d(TAG, "Response code: " + response.code());
                if (response.isSuccessful() && response.body() != null) {
                    PageResponse<User> pageResponse = response.body();
                    if (pageResponse.getContent() != null) {
                        Log.d(TAG, "Loaded " + pageResponse.getContent().size() + " users out of " + pageResponse.getTotalElements());
                        adapter.updateData(pageResponse.getContent());
                        paginationHelper.updatePagination(
                            pageResponse.getNumber(),
                            pageResponse.getTotalPages(),
                            pageResponse.getTotalElements()
                        );
                    } else {
                        Log.e(TAG, "Content is null in PageResponse");
                        Toast.makeText(UserListActivity.this, "No users data in response", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.e(TAG, "Request failed - Code: " + response.code() + ", Message: " + response.message());
                    if (response.code() == 401) {
                        Toast.makeText(UserListActivity.this, "Unauthorized - Please login again", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(UserListActivity.this, "Failed to load users: " + response.code(), Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<PageResponse<User>> call, Throwable t) {
                swipeRefresh.setRefreshing(false);
                Log.e(TAG, "Request failed with exception", t);
                Toast.makeText(UserListActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void onEditUser(User user) {
        if (!PermissionHelper.hasUserSaveAccess(authorities)) {
            Toast.makeText(this, "You don't have permission to edit users", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, UserFormActivity.class);
        intent.putExtra("user_id", user.getId());
        intent.putExtra("user_username", user.getUsername());
        intent.putExtra("user_email", user.getEmail());
        intent.putExtra("user_full_name", user.getFullName());
        intent.putExtra("user_activated", user.isActivated());
        startActivity(intent);
    }

    private void onDeleteUser(User user) {
        if (!PermissionHelper.hasUserDeleteAccess(authorities)) {
            Toast.makeText(this, "You don't have permission to delete users", Toast.LENGTH_SHORT).show();
            return;
        }
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

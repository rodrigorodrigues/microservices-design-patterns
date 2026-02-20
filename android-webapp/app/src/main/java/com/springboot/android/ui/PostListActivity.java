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
import com.springboot.android.api.PostService;
import com.springboot.android.model.Post;
import com.springboot.android.model.PageResponse;
import com.springboot.android.util.PaginationHelper;
import com.springboot.android.util.PermissionHelper;
import com.springboot.android.util.SessionManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PostListActivity extends AppCompatActivity {
    private static final String TAG = "PostListActivity";
    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefresh;
    private TextInputEditText etSearch;
    private MaterialButton btnSearch;
    private PostService postService;
    private PostAdapter adapter;
    private PaginationHelper paginationHelper;
    private SessionManager sessionManager;
    private List<String> authorities;
    private int currentPage = 0;
    private String searchQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_list);

        sessionManager = new SessionManager(this);
        authorities = sessionManager.getAuthorities();
        postService = ApiClient.getClient().create(PostService.class);

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

        adapter = new PostAdapter(new ArrayList<>(), this::onEditPost, this::onDeletePost);
        adapter.setPermissions(
            PermissionHelper.hasPostSaveAccess(authorities),
            PermissionHelper.hasPostDeleteAccess(authorities)
        );
        recyclerView.setAdapter(adapter);

        swipeRefresh = findViewById(R.id.swipeRefresh);
        swipeRefresh.setOnRefreshListener(this::loadPosts);

        FloatingActionButton fabAdd = findViewById(R.id.fabAdd);
        if (PermissionHelper.hasPostCreateAccess(authorities)) {
            fabAdd.setVisibility(View.VISIBLE);
            fabAdd.setOnClickListener(v -> {
                Intent intent = new Intent(this, PostFormActivity.class);
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
        loadPosts();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPosts();
    }

    private void loadPosts() {
        swipeRefresh.setRefreshing(true);
        postService.getPosts(currentPage, 10, searchQuery).enqueue(new Callback<PageResponse<Post>>() {
            @Override
            public void onResponse(Call<PageResponse<Post>> call, Response<PageResponse<Post>> response) {
                swipeRefresh.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null) {
                    PageResponse<Post> pageResponse = response.body();
                    adapter.updateData(pageResponse.getContent());
                    paginationHelper.updatePagination(
                        pageResponse.getNumber(),
                        pageResponse.getTotalPages(),
                        pageResponse.getTotalElements()
                    );
                    Log.d(TAG, "Loaded " + pageResponse.getContent().size() + " posts");
                } else {
                    Toast.makeText(PostListActivity.this, "Failed to load posts", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<PageResponse<Post>> call, Throwable t) {
                swipeRefresh.setRefreshing(false);
                Toast.makeText(PostListActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void onEditPost(Post post) {
        if (!PermissionHelper.hasPostSaveAccess(authorities)) {
            Toast.makeText(this, "You don't have permission to edit posts", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, PostFormActivity.class);
        intent.putExtra("post_id", post.getId());
        intent.putExtra("post_name", post.getName());
        startActivity(intent);
    }

    private void onDeletePost(Post post) {
        if (!PermissionHelper.hasPostDeleteAccess(authorities)) {
            Toast.makeText(this, "You don't have permission to delete posts", Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(this)
            .setTitle("Delete Post")
            .setMessage("Are you sure you want to delete " + post.getName() + "?")
            .setPositiveButton("Delete", (dialog, which) -> {
                postService.deletePost(post.getId()).enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(PostListActivity.this, "Deleted successfully", Toast.LENGTH_SHORT).show();
                            loadPosts();
                        } else {
                            Toast.makeText(PostListActivity.this, "Failed to delete", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        Toast.makeText(PostListActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void onPageChange(int page) {
        currentPage = page;
        loadPosts();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}

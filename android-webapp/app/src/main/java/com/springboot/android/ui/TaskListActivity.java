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
import com.springboot.android.api.TaskService;
import com.springboot.android.model.Task;
import com.springboot.android.model.PageResponse;
import com.springboot.android.util.PaginationHelper;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TaskListActivity extends AppCompatActivity {
    private static final String TAG = "TaskListActivity";
    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefresh;
    private TextInputEditText etSearch;
    private MaterialButton btnSearch;
    private TaskService taskService;
    private TaskAdapter adapter;
    private PaginationHelper paginationHelper;
    private int currentPage = 0;
    private String searchQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_list);

        taskService = ApiClient.getClient().create(TaskService.class);

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

        adapter = new TaskAdapter(new ArrayList<>(), this::onEditTask, this::onDeleteTask);
        recyclerView.setAdapter(adapter);

        swipeRefresh = findViewById(R.id.swipeRefresh);
        swipeRefresh.setOnRefreshListener(this::loadTasks);

        FloatingActionButton fabAdd = findViewById(R.id.fabAdd);
        fabAdd.setOnClickListener(v -> {
            Intent intent = new Intent(this, TaskFormActivity.class);
            startActivity(intent);
        });

        View paginationView = findViewById(R.id.pagination);
        paginationHelper = new PaginationHelper(paginationView, this::onPageChange);
    }

    private void performSearch() {
        searchQuery = etSearch.getText() != null ? etSearch.getText().toString().trim() : "";
        currentPage = 0; // Reset to first page when searching
        loadTasks();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadTasks();
    }

    private void loadTasks() {
        swipeRefresh.setRefreshing(true);
        taskService.getTasks(currentPage, 10, searchQuery).enqueue(new Callback<PageResponse<Task>>() {
            @Override
            public void onResponse(Call<PageResponse<Task>> call, Response<PageResponse<Task>> response) {
                swipeRefresh.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null) {
                    PageResponse<Task> pageResponse = response.body();
                    adapter.updateData(pageResponse.getContent());
                    paginationHelper.updatePagination(
                        pageResponse.getNumber(),
                        pageResponse.getTotalPages(),
                        pageResponse.getTotalElements()
                    );
                    Log.d(TAG, "Loaded " + pageResponse.getContent().size() + " tasks");
                } else {
                    Toast.makeText(TaskListActivity.this, "Failed to load tasks", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<PageResponse<Task>> call, Throwable t) {
                swipeRefresh.setRefreshing(false);
                Toast.makeText(TaskListActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void onEditTask(Task task) {
        Intent intent = new Intent(this, TaskFormActivity.class);
        intent.putExtra("task_id", task.getId());
        intent.putExtra("task_name", task.getName());
        intent.putExtra("task_description", task.getDescription());
        startActivity(intent);
    }

    private void onDeleteTask(Task task) {
        new AlertDialog.Builder(this)
            .setTitle("Delete Task")
            .setMessage("Are you sure you want to delete " + task.getName() + "?")
            .setPositiveButton("Delete", (dialog, which) -> {
                taskService.deleteTask(task.getId()).enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(TaskListActivity.this, "Deleted successfully", Toast.LENGTH_SHORT).show();
                            loadTasks();
                        } else {
                            Toast.makeText(TaskListActivity.this, "Failed to delete", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        Toast.makeText(TaskListActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void onPageChange(int page) {
        currentPage = page;
        loadTasks();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}

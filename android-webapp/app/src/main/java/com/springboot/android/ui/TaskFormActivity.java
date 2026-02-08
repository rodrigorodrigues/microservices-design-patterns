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
import com.springboot.android.api.TaskService;
import com.springboot.android.model.Task;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TaskFormActivity extends AppCompatActivity {
    private TextInputEditText etName, etDescription;
    private MaterialButton btnSave;
    private ProgressBar progressBar;
    private TaskService taskService;
    private String taskId;
    private boolean isEditMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_form);

        taskService = ApiClient.getClient().create(TaskService.class);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        etName = findViewById(R.id.etName);
        etDescription = findViewById(R.id.etDescription);
        btnSave = findViewById(R.id.btnSave);
        progressBar = findViewById(R.id.progressBar);

        // Check if editing existing task
        taskId = getIntent().getStringExtra("task_id");
        isEditMode = taskId != null && !taskId.isEmpty();

        if (isEditMode) {
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Edit Task");
            }
            loadTaskData();
        } else {
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Add Task");
            }
        }

        btnSave.setOnClickListener(v -> saveTask());
    }

    private void loadTaskData() {
        if (etName != null) etName.setText(getIntent().getStringExtra("task_name"));
        if (etDescription != null) etDescription.setText(getIntent().getStringExtra("task_description"));
    }

    private void saveTask() {
        String name = etName.getText() != null ? etName.getText().toString().trim() : "";
        String description = etDescription.getText() != null ? etDescription.getText().toString().trim() : "";

        if (name.isEmpty()) {
            Toast.makeText(this, "Name is required", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSave.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);

        Task task = new Task();
        task.setName(name);
        task.setDescription(description);

        Call<Task> call = isEditMode ?
            taskService.updateTask(taskId, task) :
            taskService.createTask(task);

        call.enqueue(new Callback<Task>() {
            @Override
            public void onResponse(Call<Task> call, Response<Task> response) {
                btnSave.setEnabled(true);
                progressBar.setVisibility(View.GONE);

                if (response.isSuccessful()) {
                    Toast.makeText(TaskFormActivity.this,
                        isEditMode ? "Updated successfully" : "Created successfully",
                        Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(TaskFormActivity.this,
                        "Failed: " + response.message(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Task> call, Throwable t) {
                btnSave.setEnabled(true);
                progressBar.setVisibility(View.GONE);
                Toast.makeText(TaskFormActivity.this,
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

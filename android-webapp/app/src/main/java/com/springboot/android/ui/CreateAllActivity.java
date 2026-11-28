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
import com.springboot.android.api.PersonService;
import com.springboot.android.api.TaskService;
import com.springboot.android.model.Person;
import com.springboot.android.model.Task;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CreateAllActivity extends AppCompatActivity {
    private TextInputEditText etFullName, etDateOfBirth, etAddress, etTaskName;
    private MaterialButton btnCreate;
    private ProgressBar progressBar;
    private PersonService personService;
    private TaskService taskService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_all);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Add Person + Task");
        }

        personService = ApiClient.getClient().create(PersonService.class);
        taskService = ApiClient.getClient().create(TaskService.class);

        etFullName = findViewById(R.id.etFullName);
        etDateOfBirth = findViewById(R.id.etDateOfBirth);
        etAddress = findViewById(R.id.etAddress);
        etTaskName = findViewById(R.id.etTaskName);
        btnCreate = findViewById(R.id.btnCreate);
        progressBar = findViewById(R.id.progressBar);

        btnCreate.setOnClickListener(v -> createPersonThenTask());
    }

    private void createPersonThenTask() {
        String fullName = etFullName.getText() != null ? etFullName.getText().toString().trim() : "";
        String dateOfBirth = etDateOfBirth.getText() != null ? etDateOfBirth.getText().toString().trim() : "";
        String address = etAddress.getText() != null ? etAddress.getText().toString().trim() : "";
        String taskName = etTaskName.getText() != null ? etTaskName.getText().toString().trim() : "";

        if (fullName.isEmpty() || dateOfBirth.isEmpty()) {
            Toast.makeText(this, "Full name and date of birth are required", Toast.LENGTH_SHORT).show();
            return;
        }
        if (taskName.isEmpty()) {
            Toast.makeText(this, "Task name is required", Toast.LENGTH_SHORT).show();
            return;
        }

        btnCreate.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);

        Person person = new Person();
        person.setFullName(fullName);
        person.setDateOfBirth(dateOfBirth);
        if (!address.isEmpty()) {
            Person.Address personAddress = new Person.Address();
            personAddress.setAddress(address);
            person.setAddress(personAddress);
        }

        personService.createPerson(person).enqueue(new Callback<Person>() {
            @Override
            public void onResponse(Call<Person> call, Response<Person> response) {
                if (response.isSuccessful() && response.body() != null) {
                    createTask(response.body().getId(), taskName);
                } else {
                    btnCreate.setEnabled(true);
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(CreateAllActivity.this, "Failed to create person: " + response.message(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Person> call, Throwable t) {
                btnCreate.setEnabled(true);
                progressBar.setVisibility(View.GONE);
                Toast.makeText(CreateAllActivity.this, "Error creating person: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void createTask(String personId, String taskName) {
        Task task = new Task();
        task.setName(taskName);
        task.setPersonId(personId);

        taskService.createTask(task).enqueue(new Callback<Task>() {
            @Override
            public void onResponse(Call<Task> call, Response<Task> response) {
                btnCreate.setEnabled(true);
                progressBar.setVisibility(View.GONE);

                if (response.isSuccessful()) {
                    Toast.makeText(CreateAllActivity.this, "Person and Task created successfully", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(CreateAllActivity.this, "Person created, but task failed: " + response.message(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Task> call, Throwable t) {
                btnCreate.setEnabled(true);
                progressBar.setVisibility(View.GONE);
                Toast.makeText(CreateAllActivity.this, "Person created, but task failed: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}

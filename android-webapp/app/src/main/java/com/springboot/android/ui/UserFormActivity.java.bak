package com.springboot.android.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.springboot.android.R;
import com.springboot.android.api.ApiClient;
import com.springboot.android.api.UserService;
import com.springboot.android.model.User;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UserFormActivity extends AppCompatActivity {
    private TextInputEditText etUsername, etEmail, etFullName;
    private CheckBox cbActivated;
    private MaterialButton btnSave;
    private ProgressBar progressBar;
    private UserService userService;
    private String userId;
    private boolean isEditMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_form);

        userService = ApiClient.getClient().create(UserService.class);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        etUsername = findViewById(R.id.etUsername);
        etEmail = findViewById(R.id.etEmail);
        etFullName = findViewById(R.id.etFullName);
        cbActivated = findViewById(R.id.cbActivated);
        btnSave = findViewById(R.id.btnSave);
        progressBar = findViewById(R.id.progressBar);

        // Check if editing existing user
        userId = getIntent().getStringExtra("user_id");
        isEditMode = userId != null;

        if (isEditMode) {
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Edit User");
            }
            loadUserData();
        } else {
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Add User");
            }
        }

        btnSave.setOnClickListener(v -> saveUser());
    }

    private void loadUserData() {
        if (etUsername != null) etUsername.setText(getIntent().getStringExtra("user_username"));
        if (etEmail != null) etEmail.setText(getIntent().getStringExtra("user_email"));
        if (etFullName != null) etFullName.setText(getIntent().getStringExtra("user_full_name"));
        if (cbActivated != null) cbActivated.setChecked(getIntent().getBooleanExtra("user_activated", false));
    }

    private void saveUser() {
        String username = etUsername.getText() != null ? etUsername.getText().toString().trim() : "";
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String fullName = etFullName.getText() != null ? etFullName.getText().toString().trim() : "";
        boolean activated = cbActivated.isChecked();

        if (username.isEmpty()) {
            Toast.makeText(this, "Username is required", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSave.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setFullName(fullName);
        user.setActivated(activated);

        Call<User> call = isEditMode ?
            userService.updateUser(userId, user) :
            userService.createUser(user);

        call.enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                btnSave.setEnabled(true);
                progressBar.setVisibility(View.GONE);

                if (response.isSuccessful()) {
                    Toast.makeText(UserFormActivity.this,
                        isEditMode ? "Updated successfully" : "Created successfully",
                        Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(UserFormActivity.this,
                        "Failed: " + response.message(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                btnSave.setEnabled(true);
                progressBar.setVisibility(View.GONE);
                Toast.makeText(UserFormActivity.this,
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

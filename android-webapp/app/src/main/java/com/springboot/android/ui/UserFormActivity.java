package com.springboot.android.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.springboot.android.R;
import com.springboot.android.api.ApiClient;
import com.springboot.android.api.UserService;
import com.springboot.android.model.Permission;
import com.springboot.android.model.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UserFormActivity extends AppCompatActivity {
    private TextInputEditText etFullName, etEmail;
    private TextInputEditText etCurrentPassword, etPassword, etConfirmPassword;
    private TextInputLayout tilCurrentPassword;
    private TextView tvCurrentPasswordLabel;
    private LinearLayout permissionsContainer;
    private MaterialButton btnSave;
    private ProgressBar progressBar;
    private UserService userService;
    private String userId;
    private boolean isEditMode;
    private List<Permission> allPermissions = new ArrayList<>();
    private Map<String, CheckBox> permissionCheckBoxes = new HashMap<>();

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

        etFullName = findViewById(R.id.etFullName);
        etEmail = findViewById(R.id.etEmail);
        etCurrentPassword = findViewById(R.id.etCurrentPassword);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        tilCurrentPassword = findViewById(R.id.tilCurrentPassword);
        tvCurrentPasswordLabel = findViewById(R.id.tvCurrentPasswordLabel);
        permissionsContainer = findViewById(R.id.permissionsContainer);
        btnSave = findViewById(R.id.btnSave);
        progressBar = findViewById(R.id.progressBar);

        // Check if editing existing user
        userId = getIntent().getStringExtra("user_id");
        isEditMode = userId != null;

        if (isEditMode) {
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Edit User");
            }
            // Show current password field for edit mode
            tilCurrentPassword.setVisibility(View.VISIBLE);
            tvCurrentPasswordLabel.setVisibility(View.VISIBLE);
        } else {
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Add User");
            }
        }

        btnSave.setOnClickListener(v -> saveUser());

        // Load permissions and user data
        loadPermissions();
    }

    private void loadPermissions() {
        userService.getPermissions().enqueue(new Callback<List<Permission>>() {
            @Override
            public void onResponse(Call<List<Permission>> call, Response<List<Permission>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    allPermissions = response.body();
                    createPermissionCheckboxes();

                    // Load user data after permissions are loaded
                    if (isEditMode) {
                        loadUserData();
                    }
                } else {
                    Toast.makeText(UserFormActivity.this,
                            "Failed to load permissions", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Permission>> call, Throwable t) {
                Toast.makeText(UserFormActivity.this,
                        "Error loading permissions: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void createPermissionCheckboxes() {
        permissionsContainer.removeAllViews();
        permissionCheckBoxes.clear();

        for (Permission permissionGroup : allPermissions) {
            // Add section header for each permission type
            TextView header = new TextView(this);
            header.setText(permissionGroup.getType());
            header.setTextSize(16);
            header.setTextColor(getResources().getColor(android.R.color.black, null));
            header.setPadding(0, 16, 0, 8);
            LinearLayout.LayoutParams headerParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            header.setLayoutParams(headerParams);
            permissionsContainer.addView(header);

            // Add checkboxes for each permission in this group
            for (String permission : permissionGroup.getPermissions()) {
                CheckBox checkBox = new CheckBox(this);
                checkBox.setText(permission);
                checkBox.setPadding(32, 8, 0, 8);
                LinearLayout.LayoutParams checkBoxParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                checkBox.setLayoutParams(checkBoxParams);
                permissionsContainer.addView(checkBox);

                // Store reference to checkbox
                permissionCheckBoxes.put(permission, checkBox);
            }
        }
    }

    private void loadUserData() {
        progressBar.setVisibility(View.VISIBLE);

        userService.getUser(userId).enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                progressBar.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null) {
                    User user = response.body();

                    etFullName.setText(user.getFullName());
                    etEmail.setText(user.getEmail());

                    // Check the appropriate permission checkboxes
                    if (user.getAuthorities() != null) {
                        for (User.Authority authority : user.getAuthorities()) {
                            String role = authority.getRole();
                            CheckBox checkBox = permissionCheckBoxes.get(role);
                            if (checkBox != null) {
                                checkBox.setChecked(true);
                            }
                        }
                    }
                } else {
                    Toast.makeText(UserFormActivity.this,
                            "Failed to load user", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(UserFormActivity.this,
                        "Error loading user: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveUser() {
        String fullName = etFullName.getText() != null ? etFullName.getText().toString().trim() : "";
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String currentPassword = etCurrentPassword.getText() != null ? etCurrentPassword.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";
        String confirmPassword = etConfirmPassword.getText() != null ? etConfirmPassword.getText().toString().trim() : "";

        // Validation
        if (fullName.isEmpty()) {
            Toast.makeText(this, "Full name is required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (email.isEmpty()) {
            Toast.makeText(this, "Email is required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isEditMode && password.isEmpty()) {
            Toast.makeText(this, "Password is required for new users", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.isEmpty() && !password.equals(confirmPassword)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSave.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);

        User user = new User();
        user.setFullName(fullName);
        user.setEmail(email);

        if (isEditMode && !currentPassword.isEmpty()) {
            user.setCurrentPassword(currentPassword);
        }

        if (!password.isEmpty()) {
            user.setPassword(password);
            user.setConfirmPassword(confirmPassword);
        }

        // Collect selected permissions
        List<User.Authority> authorities = new ArrayList<>();
        for (Map.Entry<String, CheckBox> entry : permissionCheckBoxes.entrySet()) {
            if (entry.getValue().isChecked()) {
                User.Authority authority = new User.Authority();
                authority.setRole(entry.getKey());
                authorities.add(authority);
            }
        }
        user.setAuthorities(authorities);

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

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
import com.springboot.android.api.PostService;
import com.springboot.android.model.Post;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PostFormActivity extends AppCompatActivity {
    private TextInputEditText etName;
    private MaterialButton btnSave;
    private ProgressBar progressBar;
    private PostService postService;
    private Long postId;
    private boolean isEditMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_form);

        postService = ApiClient.getClient().create(PostService.class);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        etName = findViewById(R.id.etName);
        btnSave = findViewById(R.id.btnSave);
        progressBar = findViewById(R.id.progressBar);

        // Check if editing existing post
        postId = getIntent().getLongExtra("post_id", -1);
        isEditMode = postId != -1;

        if (isEditMode) {
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Edit Post");
            }
            loadPostData();
        } else {
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Add Post");
            }
        }

        btnSave.setOnClickListener(v -> savePost());
    }

    private void loadPostData() {
        if (etName != null) etName.setText(getIntent().getStringExtra("post_name"));
    }

    private void savePost() {
        String name = etName.getText() != null ? etName.getText().toString().trim() : "";

        if (name.isEmpty()) {
            Toast.makeText(this, "Name is required", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSave.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);

        Post post = new Post();
        post.setName(name);

        Call<Post> call = isEditMode ?
            postService.updatePost(postId, post) :
            postService.createPost(post);

        call.enqueue(new Callback<Post>() {
            @Override
            public void onResponse(Call<Post> call, Response<Post> response) {
                btnSave.setEnabled(true);
                progressBar.setVisibility(View.GONE);

                if (response.isSuccessful()) {
                    Toast.makeText(PostFormActivity.this,
                        isEditMode ? "Updated successfully" : "Created successfully",
                        Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(PostFormActivity.this,
                        "Failed: " + response.message(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Post> call, Throwable t) {
                btnSave.setEnabled(true);
                progressBar.setVisibility(View.GONE);
                Toast.makeText(PostFormActivity.this,
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

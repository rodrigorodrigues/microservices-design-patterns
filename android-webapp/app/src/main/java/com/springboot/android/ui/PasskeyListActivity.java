package com.springboot.android.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.springboot.android.R;
import com.springboot.android.api.ApiClient;
import com.springboot.android.api.PasskeyService;
import com.springboot.android.model.Passkey;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PasskeyListActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private PasskeyAdapter adapter;
    private PasskeyService passkeyService;
    private final List<Passkey> passkeys = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_passkey_list);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        passkeyService = ApiClient.getClient().create(PasskeyService.class);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PasskeyAdapter(passkeys, this::deletePasskey);
        recyclerView.setAdapter(adapter);

        FloatingActionButton fabAdd = findViewById(R.id.fabAdd);
        fabAdd.setOnClickListener(v -> startActivity(new Intent(this, PasskeyFormActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPasskeys();
    }

    private void loadPasskeys() {
        passkeyService.getPasskeys().enqueue(new Callback<List<Passkey>>() {
            @Override
            public void onResponse(Call<List<Passkey>> call, Response<List<Passkey>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    passkeys.clear();
                    passkeys.addAll(response.body());
                    adapter.updateData(passkeys);
                } else {
                    Toast.makeText(PasskeyListActivity.this, "Failed to load passkeys", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Passkey>> call, Throwable t) {
                // The backend returns 200 with a genuinely empty (0-byte) body when the
                // user has no passkeys yet, instead of "[]" - Gson can't parse that as a
                // List, so Retrofit surfaces it here rather than onResponse. Treat it as
                // an empty list rather than an error.
                if (t instanceof java.io.EOFException) {
                    passkeys.clear();
                    adapter.updateData(passkeys);
                    return;
                }
                Toast.makeText(PasskeyListActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deletePasskey(Passkey passkey) {
        passkeyService.deletePasskey(passkey.getId()).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    loadPasskeys();
                } else {
                    Toast.makeText(PasskeyListActivity.this, "Failed to delete passkey", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(PasskeyListActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}

package com.springboot.android.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.credentials.CreateCredentialResponse;
import androidx.credentials.CreatePublicKeyCredentialRequest;
import androidx.credentials.CreatePublicKeyCredentialResponse;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.exceptions.CreateCredentialException;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.springboot.android.R;
import com.springboot.android.api.ApiClient;
import com.springboot.android.api.PasskeyService;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PasskeyFormActivity extends AppCompatActivity {
    private TextInputEditText etLabel;
    private MaterialButton btnRegister;
    private ProgressBar progressBar;
    private PasskeyService passkeyService;
    private CredentialManager credentialManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_passkey_form);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        passkeyService = ApiClient.getClient().create(PasskeyService.class);
        credentialManager = CredentialManager.create(this);

        etLabel = findViewById(R.id.etLabel);
        btnRegister = findViewById(R.id.btnRegister);
        progressBar = findViewById(R.id.progressBar);

        btnRegister.setOnClickListener(v -> registerPasskey());
    }

    private void registerPasskey() {
        String label = etLabel.getText() != null ? etLabel.getText().toString().trim() : "";
        if (label.isEmpty()) {
            Toast.makeText(this, "Label is required", Toast.LENGTH_SHORT).show();
            return;
        }

        btnRegister.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);

        passkeyService.getRegisterOptions().enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    createCredential(response.body(), label);
                } else {
                    finishWithError("Failed to get registration challenge");
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                finishWithError("Error: " + t.getMessage());
            }
        });
    }

    private void createCredential(JsonObject challengeOptions, String label) {
        // challengeOptions matches the server's PublicKeyCredentialCreationOptions JSON
        // (same shape react-webapp decodes in PasskeyEdit.js).
        CreatePublicKeyCredentialRequest request =
                new CreatePublicKeyCredentialRequest(challengeOptions.toString());

        credentialManager.createCredentialAsync(
                this,
                request,
                null,
                getMainExecutor(),
                new CredentialManagerCallback<CreateCredentialResponse, CreateCredentialException>() {
                    @Override
                    public void onResult(CreateCredentialResponse result) {
                        CreatePublicKeyCredentialResponse response = (CreatePublicKeyCredentialResponse) result;
                        JsonObject registrationPayload = JsonParser.parseString(response.getRegistrationResponseJson()).getAsJsonObject();
                        registrationPayload.addProperty("label", label);
                        sendRegistration(registrationPayload);
                    }

                    @Override
                    public void onError(CreateCredentialException e) {
                        finishWithError("Registration failed: " + e.getMessage());
                    }
                });
    }

    private void sendRegistration(JsonObject registrationPayload) {
        passkeyService.register(registrationPayload).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                btnRegister.setEnabled(true);
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful()) {
                    Toast.makeText(PasskeyFormActivity.this, "Passkey registered", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(PasskeyFormActivity.this, "Failed to save passkey", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                finishWithError("Error: " + t.getMessage());
            }
        });
    }

    private void finishWithError(String message) {
        btnRegister.setEnabled(true);
        progressBar.setVisibility(View.GONE);
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}

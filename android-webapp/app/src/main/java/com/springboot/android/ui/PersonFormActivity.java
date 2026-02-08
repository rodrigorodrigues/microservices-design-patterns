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
import com.springboot.android.model.Person;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PersonFormActivity extends AppCompatActivity {
    private TextInputEditText etFullName, etDateOfBirth;
    private TextInputEditText etAddress, etCity, etStateOrProvince, etCountry, etPostalCode;
    private MaterialButton btnSave;
    private ProgressBar progressBar;
    private PersonService personService;
    private String personId;
    private boolean isEditMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_person_form);

        personService = ApiClient.getClient().create(PersonService.class);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        etFullName = findViewById(R.id.etFullName);
        etDateOfBirth = findViewById(R.id.etDateOfBirth);
        etAddress = findViewById(R.id.etAddress);
        etCity = findViewById(R.id.etCity);
        etStateOrProvince = findViewById(R.id.etStateOrProvince);
        etCountry = findViewById(R.id.etCountry);
        etPostalCode = findViewById(R.id.etPostalCode);
        btnSave = findViewById(R.id.btnSave);
        progressBar = findViewById(R.id.progressBar);

        // Check if editing existing person
        personId = getIntent().getStringExtra("person_id");
        isEditMode = personId != null;

        if (isEditMode) {
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Edit Person");
            }
            loadPersonData();
        } else {
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Add Person");
            }
        }

        btnSave.setOnClickListener(v -> savePerson());
    }

    private void loadPersonData() {
        if (etFullName != null) etFullName.setText(getIntent().getStringExtra("person_full_name"));
        if (etDateOfBirth != null) etDateOfBirth.setText(getIntent().getStringExtra("person_date_of_birth"));

        // Load address data
        if (etAddress != null) etAddress.setText(getIntent().getStringExtra("person_address"));
        if (etCity != null) etCity.setText(getIntent().getStringExtra("person_city"));
        if (etStateOrProvince != null) etStateOrProvince.setText(getIntent().getStringExtra("person_state_or_province"));
        if (etCountry != null) etCountry.setText(getIntent().getStringExtra("person_country"));
        if (etPostalCode != null) etPostalCode.setText(getIntent().getStringExtra("person_postal_code"));
    }

    private void savePerson() {
        String fullName = etFullName.getText() != null ? etFullName.getText().toString().trim() : "";
        String dateOfBirth = etDateOfBirth.getText() != null ? etDateOfBirth.getText().toString().trim() : "";

        String address = etAddress.getText() != null ? etAddress.getText().toString().trim() : "";
        String city = etCity.getText() != null ? etCity.getText().toString().trim() : "";
        String stateOrProvince = etStateOrProvince.getText() != null ? etStateOrProvince.getText().toString().trim() : "";
        String country = etCountry.getText() != null ? etCountry.getText().toString().trim() : "";
        String postalCode = etPostalCode.getText() != null ? etPostalCode.getText().toString().trim() : "";

        if (fullName.isEmpty()) {
            Toast.makeText(this, "Full name is required", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSave.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);

        Person person = new Person();
        person.setFullName(fullName);
        person.setDateOfBirth(dateOfBirth);

        // Create and set address if any address field is filled
        if (!address.isEmpty() || !city.isEmpty() || !stateOrProvince.isEmpty() || !country.isEmpty() || !postalCode.isEmpty()) {
            Person.Address personAddress = new Person.Address();
            personAddress.setAddress(address);
            personAddress.setCity(city);
            personAddress.setStateOrProvince(stateOrProvince);
            personAddress.setCountry(country);
            personAddress.setPostalCode(postalCode);
            person.setAddress(personAddress);
        }

        Call<Person> call = isEditMode ?
            personService.updatePerson(personId, person) :
            personService.createPerson(person);

        call.enqueue(new Callback<Person>() {
            @Override
            public void onResponse(Call<Person> call, Response<Person> response) {
                btnSave.setEnabled(true);
                progressBar.setVisibility(View.GONE);

                if (response.isSuccessful()) {
                    Toast.makeText(PersonFormActivity.this,
                        isEditMode ? "Updated successfully" : "Created successfully",
                        Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(PersonFormActivity.this,
                        "Failed: " + response.message(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Person> call, Throwable t) {
                btnSave.setEnabled(true);
                progressBar.setVisibility(View.GONE);
                Toast.makeText(PersonFormActivity.this,
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

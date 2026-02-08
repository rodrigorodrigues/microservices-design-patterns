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
import com.springboot.android.api.PersonService;
import com.springboot.android.model.PageResponse;
import com.springboot.android.model.Person;
import com.springboot.android.util.PaginationHelper;

import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PersonListActivity extends AppCompatActivity {
    private static final String TAG = "PersonListActivity";
    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefresh;
    private TextInputEditText etSearch;
    private MaterialButton btnSearch;
    private PersonService personService;
    private PersonAdapter adapter;
    private PaginationHelper paginationHelper;
    private int currentPage = 0;
    private String searchQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_person_list);

        personService = ApiClient.getClient().create(PersonService.class);

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

        adapter = new PersonAdapter(new ArrayList<>(), this::onEditPerson, this::onDeletePerson);
        recyclerView.setAdapter(adapter);

        swipeRefresh = findViewById(R.id.swipeRefresh);
        swipeRefresh.setOnRefreshListener(this::loadPersons);

        FloatingActionButton fabAdd = findViewById(R.id.fabAdd);
        fabAdd.setOnClickListener(v -> {
            Intent intent = new Intent(this, PersonFormActivity.class);
            startActivity(intent);
        });

        View paginationView = findViewById(R.id.pagination);
        paginationHelper = new PaginationHelper(paginationView, this::onPageChange);
    }

    private void performSearch() {
        searchQuery = etSearch.getText() != null ? etSearch.getText().toString().trim() : "";
        currentPage = 0; // Reset to first page when searching
        loadPersons();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPersons();
    }

    private void loadPersons() {
        swipeRefresh.setRefreshing(true);
        personService.getPersons(currentPage, 10, searchQuery).enqueue(new Callback<PageResponse<Person>>() {
            @Override
            public void onResponse(Call<PageResponse<Person>> call, Response<PageResponse<Person>> response) {
                swipeRefresh.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null) {
                    PageResponse<Person> pageResponse = response.body();
                    adapter.updateData(pageResponse.getContent());
                    paginationHelper.updatePagination(
                        pageResponse.getNumber(),
                        pageResponse.getTotalPages(),
                        pageResponse.getTotalElements()
                    );
                    Log.d(TAG, "Loaded " + pageResponse.getContent().size() + " persons");
                } else {
                    Toast.makeText(PersonListActivity.this, "Failed to load persons", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<PageResponse<Person>> call, Throwable t) {
                swipeRefresh.setRefreshing(false);
                Toast.makeText(PersonListActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void onEditPerson(Person person) {
        Intent intent = new Intent(this, PersonFormActivity.class);
        intent.putExtra("person_id", person.getId());
        intent.putExtra("person_full_name", person.getFullName());
        intent.putExtra("person_date_of_birth", person.getDateOfBirth());

        // Pass address data if available
        if (person.getAddress() != null) {
            Person.Address address = person.getAddress();
            intent.putExtra("person_address", address.getAddress());
            intent.putExtra("person_city", address.getCity());
            intent.putExtra("person_state_or_province", address.getStateOrProvince());
            intent.putExtra("person_country", address.getCountry());
            intent.putExtra("person_postal_code", address.getPostalCode());
        }
        startActivity(intent);
    }

    private void onDeletePerson(Person person) {
        new AlertDialog.Builder(this)
            .setTitle("Delete Person")
            .setMessage("Are you sure you want to delete " + person.getFullName() + "?")
            .setPositiveButton("Delete", (dialog, which) -> {
                personService.deletePerson(person.getId()).enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(PersonListActivity.this, "Deleted successfully", Toast.LENGTH_SHORT).show();
                            loadPersons();
                        } else {
                            Toast.makeText(PersonListActivity.this, "Failed to delete", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        Toast.makeText(PersonListActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void onPageChange(int page) {
        currentPage = page;
        loadPersons();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}

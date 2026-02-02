package com.springboot.android.ui;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.springboot.android.R;
import com.springboot.android.api.ApiClient;
import com.springboot.android.api.PersonService;
import com.springboot.android.model.PageResponse;
import com.springboot.android.model.Person;

import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PersonListActivity extends AppCompatActivity {
    private static final String TAG = "PersonListActivity";
    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefresh;
    private PersonService personService;
    private PersonAdapter adapter;

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

        loadPersons();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPersons();
    }

    private void loadPersons() {
        swipeRefresh.setRefreshing(true);
        personService.getPersons(0, 100).enqueue(new Callback<PageResponse<Person>>() {
            @Override
            public void onResponse(Call<PageResponse<Person>> call, Response<PageResponse<Person>> response) {
                swipeRefresh.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null) {
                    adapter.updateData(response.body().getContent());
                    Log.d(TAG, "Loaded " + response.body().getContent().size() + " persons");
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
        intent.putExtra("person_first_name", person.getFirstName());
        intent.putExtra("person_last_name", person.getLastName());
        intent.putExtra("person_email", person.getEmail());
        intent.putExtra("person_phone", person.getPhone());
        startActivity(intent);
    }

    private void onDeletePerson(Person person) {
        new AlertDialog.Builder(this)
            .setTitle("Delete Person")
            .setMessage("Are you sure you want to delete " + person.getFirstName() + " " + person.getLastName() + "?")
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

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}

# Android WebApp Feature Parity Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Bring `android-webapp` to feature parity with `react-webapp`: read-only Person children view, WeekMenu Category product assignment, an admin-only "Create All" (Person+Task) wizard, and full WebAuthn/Passkey support (list/register/delete + passkey login). Also delete a stale leftover file.

**Architecture:**
- Reuse existing retrofit/model/adapter conventions (`ApiClient`, `PersonService`, `ProductService`, `TaskService`, `PermissionHelper`) rather than introducing new patterns.
- Product multi-select in the Category form reuses the dynamic-checkbox pattern already used for permissions in `UserFormActivity`.
- WebAuthn uses `androidx.credentials` (Credential Manager), the current Android API for passkeys — no such dependency exists in the project yet.
- Create All chains two existing REST calls (`PersonService.createPerson` → `TaskService.createTask`) rather than one combined call, matching the real `Task.personId` contract.

**Tech Stack:** Java, Android SDK 35 (minSdk 24), Retrofit 2.11, Gson, Material Components, `androidx.credentials` (new).

---

### Task 1: ~~Delete stale UserFormActivity.java.bak~~ — SKIPPED

`UserFormActivity.java.bak` is an intentional backup, not dead weight. Do not delete it. Proceed to Task 2.

### Task 2: Read-only Person children view

**Files:**
- Modify: `android-webapp/app/src/main/res/layout/item_person.xml`
- Modify: `android-webapp/app/src/main/java/com/springboot/android/ui/PersonAdapter.java`

- [x] **Step 1: Add a "Show Children" button to item_person.xml**

```xml
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="8dp"
        android:orientation="horizontal">

        <com.google.android.material.button.MaterialButton
            android:id="@+id/btnChildren"
            style="@style/Widget.MaterialComponents.Button.TextButton"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="Show Children"
            android:visibility="gone" />

        <com.google.android.material.button.MaterialButton
            android:id="@+id/btnEdit"
            style="@style/Widget.MaterialComponents.Button.TextButton"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="Edit" />

        <com.google.android.material.button.MaterialButton
            android:id="@+id/btnDelete"
            style="@style/Widget.MaterialComponents.Button.TextButton"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="Delete"
            android:textColor="@android:color/holo_red_dark" />
    </LinearLayout>
```

(Replace the existing button `LinearLayout` in `item_person.xml` with the block above — same two buttons, plus `btnChildren` prepended.)

- [x] **Step 2: Show the children dialog from PersonAdapter**

```java
package com.springboot.android.ui;

import android.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.springboot.android.R;
import com.springboot.android.model.Person;

import java.util.List;

public class PersonAdapter extends RecyclerView.Adapter<PersonAdapter.ViewHolder> {
    private List<Person> persons;
    private final OnItemClickListener<Person> editListener;
    private final OnItemClickListener<Person> deleteListener;
    private boolean hasSaveAccess = true;
    private boolean hasDeleteAccess = true;

    public interface OnItemClickListener<T> {
        void onClick(T item);
    }

    public PersonAdapter(List<Person> persons, OnItemClickListener<Person> editListener, OnItemClickListener<Person> deleteListener) {
        this.persons = persons;
        this.editListener = editListener;
        this.deleteListener = deleteListener;
    }

    public void setPermissions(boolean hasSaveAccess, boolean hasDeleteAccess) {
        this.hasSaveAccess = hasSaveAccess;
        this.hasDeleteAccess = hasDeleteAccess;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_person, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Person person = persons.get(position);
        holder.tvName.setText(person.getFullName() != null ? person.getFullName() : "");
        holder.tvEmail.setText(person.getDateOfBirth() != null ? person.getDateOfBirth() : "");

        String addressText = "";
        if (person.getAddress() != null) {
            Person.Address address = person.getAddress();
            if (address.getCity() != null || address.getCountry() != null) {
                addressText = (address.getCity() != null ? address.getCity() : "") +
                             (address.getCountry() != null ? ", " + address.getCountry() : "");
            }
        }
        holder.tvPhone.setText(addressText);

        if (person.getChildren() != null && !person.getChildren().isEmpty()) {
            holder.btnChildren.setVisibility(View.VISIBLE);
            holder.btnChildren.setOnClickListener(v -> showChildrenDialog(holder.itemView.getContext(), person));
        } else {
            holder.btnChildren.setVisibility(View.GONE);
        }

        if (hasSaveAccess) {
            holder.btnEdit.setVisibility(View.VISIBLE);
            holder.btnEdit.setOnClickListener(v -> editListener.onClick(person));
        } else {
            holder.btnEdit.setVisibility(View.GONE);
        }

        if (hasDeleteAccess) {
            holder.btnDelete.setVisibility(View.VISIBLE);
            holder.btnDelete.setOnClickListener(v -> deleteListener.onClick(person));
        } else {
            holder.btnDelete.setVisibility(View.GONE);
        }
    }

    private void showChildrenDialog(android.content.Context context, Person person) {
        StringBuilder message = new StringBuilder();
        for (Person.Children child : person.getChildren()) {
            message.append(child.getName() != null ? child.getName() : "")
                   .append(" — ")
                   .append(child.getDateOfBirth() != null ? child.getDateOfBirth() : "")
                   .append("\n");
        }
        new AlertDialog.Builder(context)
            .setTitle("Children")
            .setMessage(message.toString().trim())
            .setPositiveButton("Close", (dialog, which) -> dialog.dismiss())
            .show();
    }

    @Override
    public int getItemCount() {
        return persons.size();
    }

    public void updateData(List<Person> newData) {
        this.persons = newData;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvEmail, tvPhone;
        MaterialButton btnChildren, btnEdit, btnDelete;

        ViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvEmail = itemView.findViewById(R.id.tvEmail);
            tvPhone = itemView.findViewById(R.id.tvPhone);
            btnChildren = itemView.findViewById(R.id.btnChildren);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
```

- [x] **Step 3: Build to verify**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL

- [x] **Step 4: Commit changes**

```bash
git add android-webapp/app/src/main/res/layout/item_person.xml android-webapp/app/src/main/java/com/springboot/android/ui/PersonAdapter.java
git commit -m "feat: add read-only children view to person list" --trailer "Co-authored-by: Junie <junie@jetbrains.com>"
```

### Task 3: WeekMenu Category product assignment

**Files:**
- Modify: `android-webapp/app/src/main/res/layout/activity_category_form.xml`
- Modify: `android-webapp/app/src/main/java/com/springboot/android/ui/CategoryFormActivity.java`

- [x] **Step 1: Add a products checkbox container to activity_category_form.xml**

```xml
            <com.google.android.material.textfield.TextInputLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginBottom="16dp">

                <com.google.android.material.textfield.TextInputEditText
                    android:id="@+id/etName"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:hint="Name" />
            </com.google.android.material.textfield.TextInputLayout>

            <TextView
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:text="Products"
                android:textSize="16sp"
                android:layout_marginBottom="8dp" />

            <LinearLayout
                android:id="@+id/productsContainer"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="vertical" />

            <com.google.android.material.button.MaterialButton
                android:id="@+id/btnSave"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:text="Save"
                android:layout_marginTop="16dp" />
```

(Replace the `TextInputLayout`-through-`btnSave` block in the existing file with the above — same `etName` field, plus a new `productsContainer` before `btnSave`.)

- [x] **Step 2: Load products and pre-check existing category products in CategoryFormActivity**

```java
package com.springboot.android.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.springboot.android.R;
import com.springboot.android.api.ApiClient;
import com.springboot.android.api.ProductService;
import com.springboot.android.api.WeekMenuCategoryService;
import com.springboot.android.model.Category;
import com.springboot.android.model.CategoryProduct;
import com.springboot.android.model.PageResponse;
import com.springboot.android.model.Product;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CategoryFormActivity extends AppCompatActivity {
    private TextInputEditText etName;
    private LinearLayout productsContainer;
    private MaterialButton btnSave;
    private ProgressBar progressBar;
    private WeekMenuCategoryService categoryService;
    private ProductService productService;
    private String categoryId;
    private boolean isEditMode;
    private final Map<String, CheckBox> productCheckBoxes = new HashMap<>();
    private List<String> existingProductNames = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_form);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        categoryService = ApiClient.getClient().create(WeekMenuCategoryService.class);
        productService = ApiClient.getClient().create(ProductService.class);

        etName = findViewById(R.id.etName);
        productsContainer = findViewById(R.id.productsContainer);
        btnSave = findViewById(R.id.btnSave);
        progressBar = findViewById(R.id.progressBar);

        categoryId = getIntent().getStringExtra("category_id");
        isEditMode = categoryId != null;

        if (isEditMode) {
            getSupportActionBar().setTitle("Edit Category");
            loadCategoryData();
        } else {
            getSupportActionBar().setTitle("Add Category");
        }

        btnSave.setOnClickListener(v -> saveCategory());
        loadProducts();
    }

    private void loadCategoryData() {
        String name = getIntent().getStringExtra("category_name");
        if (name != null) {
            etName.setText(name);
        }

        existingProductNames = getIntent().getStringArrayListExtra("category_product_names");
        if (existingProductNames == null) {
            existingProductNames = new ArrayList<>();
        }
    }

    private void loadProducts() {
        productService.getProducts(0, 100, null).enqueue(new Callback<PageResponse<Product>>() {
            @Override
            public void onResponse(Call<PageResponse<Product>> call, Response<PageResponse<Product>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    createProductCheckboxes(response.body().getContent());
                } else {
                    Toast.makeText(CategoryFormActivity.this, "Failed to load products", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<PageResponse<Product>> call, Throwable t) {
                Toast.makeText(CategoryFormActivity.this, "Error loading products: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void createProductCheckboxes(List<Product> products) {
        productsContainer.removeAllViews();
        productCheckBoxes.clear();

        for (Product product : products) {
            CheckBox checkBox = new CheckBox(this);
            checkBox.setText(product.getName());
            checkBox.setChecked(existingProductNames.contains(product.getName()));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            checkBox.setLayoutParams(params);
            productsContainer.addView(checkBox);
            productCheckBoxes.put(product.getName(), checkBox);
        }
    }

    private void saveCategory() {
        String name = etName.getText().toString().trim();

        if (name.isEmpty()) {
            Toast.makeText(this, "Name is required", Toast.LENGTH_SHORT).show();
            return;
        }

        Category category = new Category();
        category.setName(name);

        List<CategoryProduct> selectedProducts = new ArrayList<>();
        for (Map.Entry<String, CheckBox> entry : productCheckBoxes.entrySet()) {
            if (entry.getValue().isChecked()) {
                CategoryProduct categoryProduct = new CategoryProduct();
                categoryProduct.setName(entry.getKey());
                categoryProduct.setQuantity(1);
                categoryProduct.setCompleted(false);
                selectedProducts.add(categoryProduct);
            }
        }
        category.setProducts(selectedProducts);

        progressBar.setVisibility(ProgressBar.VISIBLE);
        btnSave.setEnabled(false);

        Call<Category> call;
        if (isEditMode) {
            category.setId(categoryId);
            call = categoryService.updateCategory(categoryId, category);
        } else {
            call = categoryService.createCategory(category);
        }

        call.enqueue(new Callback<Category>() {
            @Override
            public void onResponse(Call<Category> call, Response<Category> response) {
                progressBar.setVisibility(ProgressBar.GONE);
                btnSave.setEnabled(true);

                if (response.isSuccessful()) {
                    Toast.makeText(CategoryFormActivity.this,
                        isEditMode ? "Category updated successfully" : "Category created successfully",
                        Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(CategoryFormActivity.this, "Failed to save category", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Category> call, Throwable t) {
                progressBar.setVisibility(ProgressBar.GONE);
                btnSave.setEnabled(true);
                Toast.makeText(CategoryFormActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
```

- [x] **Step 3: Pass existing product names when launching edit mode**

Correction: the edit `Intent` is built in `WeekMenuCategoryListActivity.onEditCategory()`, not `WeekMenuCategoryAdapter.java` (the adapter only invokes a callback). Added there:

```java
ArrayList<String> productNames = new ArrayList<>();
if (category.getProducts() != null) {
    for (CategoryProduct product : category.getProducts()) {
        productNames.add(product.getName());
    }
}
intent.putStringArrayListExtra("category_product_names", productNames);
```

- [x] **Step 4: Build to verify**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL

- [x] **Step 5: Commit changes**

```bash
git add android-webapp/app/src/main/res/layout/activity_category_form.xml android-webapp/app/src/main/java/com/springboot/android/ui/CategoryFormActivity.java android-webapp/app/src/main/java/com/springboot/android/ui/WeekMenuCategoryAdapter.java
git commit -m "feat: assign products to weekmenu category from android app" --trailer "Co-authored-by: Junie <junie@jetbrains.com>"
```

### Task 4: Admin "Create All" wizard

**Files:**
- Modify: `android-webapp/app/src/main/java/com/springboot/android/util/PermissionHelper.java`
- Create: `android-webapp/app/src/main/res/layout/activity_create_all.xml`
- Create: `android-webapp/app/src/main/java/com/springboot/android/ui/CreateAllActivity.java`
- Modify: `android-webapp/app/src/main/res/menu/drawer_menu.xml`
- Modify: `android-webapp/app/src/main/java/com/springboot/android/ui/DashboardActivity.java`
- Modify: `android-webapp/app/src/main/AndroidManifest.xml`

- [ ] **Step 1: Add a strict admin-only check to PermissionHelper**

```java
    // Admin-only check (unlike hasAnyPermission, does NOT pass for SCOPE_openid alone)
    public static boolean isAdmin(List<String> authorities) {
        return authorities != null && authorities.contains("ROLE_ADMIN");
    }
```

(Add this method to `PermissionHelper.java`, near `hasAnyPermission`.)

- [ ] **Step 2: Create activity_create_all.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.coordinatorlayout.widget.CoordinatorLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <com.google.android.material.appbar.AppBarLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content">

        <androidx.appcompat.widget.Toolbar
            android:id="@+id/toolbar"
            android:layout_width="match_parent"
            android:layout_height="?attr/actionBarSize"
            android:background="?attr/colorPrimary"
            app:title="Add Person + Task" />
    </com.google.android.material.appbar.AppBarLayout>

    <androidx.core.widget.NestedScrollView
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        app:layout_behavior="@string/appbar_scrolling_view_behavior">

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical"
            android:padding="16dp">

            <com.google.android.material.card.MaterialCardView
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginBottom="16dp"
                app:cardCornerRadius="8dp"
                app:cardElevation="2dp">

                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:orientation="vertical"
                    android:padding="16dp">

                    <TextView
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:text="Add Person"
                        android:textSize="18sp"
                        android:textStyle="bold"
                        android:layout_marginBottom="8dp" />

                    <com.google.android.material.textfield.TextInputLayout
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:layout_marginBottom="8dp">
                        <com.google.android.material.textfield.TextInputEditText
                            android:id="@+id/etFullName"
                            android:layout_width="match_parent"
                            android:layout_height="wrap_content"
                            android:hint="Full Name" />
                    </com.google.android.material.textfield.TextInputLayout>

                    <com.google.android.material.textfield.TextInputLayout
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:layout_marginBottom="8dp">
                        <com.google.android.material.textfield.TextInputEditText
                            android:id="@+id/etDateOfBirth"
                            android:layout_width="match_parent"
                            android:layout_height="wrap_content"
                            android:hint="Date of Birth (YYYY-MM-DD)" />
                    </com.google.android.material.textfield.TextInputLayout>

                    <com.google.android.material.textfield.TextInputLayout
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content">
                        <com.google.android.material.textfield.TextInputEditText
                            android:id="@+id/etAddress"
                            android:layout_width="match_parent"
                            android:layout_height="wrap_content"
                            android:hint="Address" />
                    </com.google.android.material.textfield.TextInputLayout>
                </LinearLayout>
            </com.google.android.material.card.MaterialCardView>

            <com.google.android.material.card.MaterialCardView
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginBottom="16dp"
                app:cardCornerRadius="8dp"
                app:cardElevation="2dp">

                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:orientation="vertical"
                    android:padding="16dp">

                    <TextView
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:text="Add Task"
                        android:textSize="18sp"
                        android:textStyle="bold"
                        android:layout_marginBottom="8dp" />

                    <com.google.android.material.textfield.TextInputLayout
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content">
                        <com.google.android.material.textfield.TextInputEditText
                            android:id="@+id/etTaskName"
                            android:layout_width="match_parent"
                            android:layout_height="wrap_content"
                            android:hint="Task Name" />
                    </com.google.android.material.textfield.TextInputLayout>
                </LinearLayout>
            </com.google.android.material.card.MaterialCardView>

            <com.google.android.material.button.MaterialButton
                android:id="@+id/btnCreate"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:text="Create" />

            <ProgressBar
                android:id="@+id/progressBar"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_gravity="center"
                android:layout_marginTop="16dp"
                android:visibility="gone" />
        </LinearLayout>
    </androidx.core.widget.NestedScrollView>
</androidx.coordinatorlayout.widget.CoordinatorLayout>
```

- [ ] **Step 3: Create CreateAllActivity.java**

```java
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
```

- [ ] **Step 4: Register the activity in AndroidManifest.xml**

```xml
        <activity
            android:name=".ui.CreateAllActivity"
            android:exported="false"
            android:label="Add Person + Task"
            android:parentActivityName=".ui.DashboardActivity" />
```

(Add alongside the other `.ui.*FormActivity` entries.)

- [ ] **Step 5: Add nav_create_all to drawer_menu.xml**

```xml
    <item android:title="Admin">
        <menu>
            <item
                android:id="@+id/nav_create_all"
                android:icon="@android:drawable/ic_menu_add"
                android:title="Add Person + Task" />
        </menu>
    </item>
```

(Insert this group before the existing `<item android:title="Account">` group.)

- [ ] **Step 6: Wire up navigation + ROLE_ADMIN gating in DashboardActivity**

In `configureMenuItemsVisibility(List<String> authorities)`:

```java
        MenuItem navCreateAll = navigationView.getMenu().findItem(R.id.nav_create_all);
        if (navCreateAll != null) {
            navCreateAll.setEnabled(PermissionHelper.isAdmin(authorities));
        }
```

In `onNavigationItemSelected(MenuItem item)`:

```java
        } else if (id == R.id.nav_create_all) {
            startActivity(new Intent(this, CreateAllActivity.class));
```

- [ ] **Step 7: Build to verify**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 8: Commit changes**

```bash
git add android-webapp/app/src/main/java/com/springboot/android/util/PermissionHelper.java android-webapp/app/src/main/res/layout/activity_create_all.xml android-webapp/app/src/main/java/com/springboot/android/ui/CreateAllActivity.java android-webapp/app/src/main/res/menu/drawer_menu.xml android-webapp/app/src/main/java/com/springboot/android/ui/DashboardActivity.java android-webapp/app/src/main/AndroidManifest.xml
git commit -m "feat: add admin Create All (person + task) wizard" --trailer "Co-authored-by: Junie <junie@jetbrains.com>"
```

### Task 5: WebAuthn / Passkeys

**Files:**
- Modify: `android-webapp/app/build.gradle`
- Create: `android-webapp/app/src/main/java/com/springboot/android/model/Passkey.java`
- Create: `android-webapp/app/src/main/java/com/springboot/android/api/PasskeyService.java`
- Create: `android-webapp/app/src/main/res/layout/activity_passkey_list.xml`
- Create: `android-webapp/app/src/main/res/layout/item_passkey.xml`
- Create: `android-webapp/app/src/main/java/com/springboot/android/ui/PasskeyAdapter.java`
- Create: `android-webapp/app/src/main/java/com/springboot/android/ui/PasskeyListActivity.java`
- Create: `android-webapp/app/src/main/res/layout/activity_passkey_form.xml`
- Create: `android-webapp/app/src/main/java/com/springboot/android/ui/PasskeyFormActivity.java`
- Modify: `android-webapp/app/src/main/res/layout/activity_login.xml`
- Modify: `android-webapp/app/src/main/java/com/springboot/android/ui/LoginActivity.java`
- Modify: `android-webapp/app/src/main/res/menu/drawer_menu.xml`
- Modify: `android-webapp/app/src/main/java/com/springboot/android/ui/DashboardActivity.java`
- Modify: `android-webapp/app/src/main/AndroidManifest.xml`

- [ ] **Step 1: Add Credential Manager dependencies**

```groovy
    // Passkeys / WebAuthn
    implementation 'androidx.credentials:credentials:1.3.0'
    implementation 'androidx.credentials:credentials-play-services-auth:1.3.0'
```

(Add to the `dependencies` block in `android-webapp/app/build.gradle`, near the other `androidx.*` entries.)

- [ ] **Step 2: Add the Passkey model**

```java
package com.springboot.android.model;

import com.google.gson.annotations.SerializedName;

public class Passkey {
    private String id;
    private String label;
    private String created;

    @SerializedName("lastUsed")
    private String lastUsed;

    @SerializedName("signatureCount")
    private long signatureCount;

    @SerializedName("lastModifiedByUser")
    private String lastModifiedByUser;

    @SerializedName("lastModifiedDate")
    private String lastModifiedDate;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public String getCreated() { return created; }
    public void setCreated(String created) { this.created = created; }
    public String getLastUsed() { return lastUsed; }
    public void setLastUsed(String lastUsed) { this.lastUsed = lastUsed; }
    public long getSignatureCount() { return signatureCount; }
    public void setSignatureCount(long signatureCount) { this.signatureCount = signatureCount; }
    public String getLastModifiedByUser() { return lastModifiedByUser; }
    public void setLastModifiedByUser(String lastModifiedByUser) { this.lastModifiedByUser = lastModifiedByUser; }
    public String getLastModifiedDate() { return lastModifiedDate; }
    public void setLastModifiedDate(String lastModifiedDate) { this.lastModifiedDate = lastModifiedDate; }
}
```

- [ ] **Step 3: Add PasskeyService**

```java
package com.springboot.android.api;

import com.google.gson.JsonObject;
import com.springboot.android.model.Passkey;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface PasskeyService {

    @GET("api/webauthns")
    Call<List<Passkey>> getPasskeys();

    @DELETE("api/webauthns/{id}")
    Call<Void> deletePasskey(@Path("id") String id);

    @POST("webauthn/register/options")
    Call<JsonObject> getRegisterOptions();

    @POST("webauthn/register")
    Call<Void> register(@Body JsonObject registrationPayload);

    @POST("webauthn/authenticate/options")
    Call<JsonObject> getAuthenticateOptions();

    @POST("login/webauthn")
    Call<Void> loginWithPasskey(@Body JsonObject assertionPayload);
}
```

- [ ] **Step 4: List + delete screen (activity_passkey_list.xml, item_passkey.xml, PasskeyAdapter.java, PasskeyListActivity.java)**

`activity_passkey_list.xml` (same structure as `activity_person_list.xml`, minus search):

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.coordinatorlayout.widget.CoordinatorLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <com.google.android.material.appbar.AppBarLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content">

        <androidx.appcompat.widget.Toolbar
            android:id="@+id/toolbar"
            android:layout_width="match_parent"
            android:layout_height="?attr/actionBarSize"
            android:background="?attr/colorPrimary"
            app:title="Passkeys" />
    </com.google.android.material.appbar.AppBarLayout>

    <androidx.swiperefreshlayout.widget.SwipeRefreshLayout
        android:id="@+id/swipeRefresh"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        app:layout_behavior="@string/appbar_scrolling_view_behavior">

        <androidx.recyclerview.widget.RecyclerView
            android:id="@+id/recyclerView"
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            android:padding="8dp" />
    </androidx.swiperefreshlayout.widget.SwipeRefreshLayout>

    <com.google.android.material.floatingactionbutton.FloatingActionButton
        android:id="@+id/fabAdd"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_gravity="bottom|end"
        android:layout_margin="16dp"
        app:srcCompat="@android:drawable/ic_input_add" />

</androidx.coordinatorlayout.widget.CoordinatorLayout>
```

`item_passkey.xml` (same card style as `item_person.xml`, one Delete button):

```xml
<?xml version="1.0" encoding="utf-8"?>
<com.google.android.material.card.MaterialCardView xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_margin="8dp"
    app:cardCornerRadius="8dp"
    app:cardElevation="4dp">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:padding="16dp">

        <TextView
            android:id="@+id/tvLabel"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:textSize="18sp"
            android:textStyle="bold" />

        <TextView
            android:id="@+id/tvCreated"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="4dp"
            android:textSize="14sp" />

        <com.google.android.material.button.MaterialButton
            android:id="@+id/btnDelete"
            style="@style/Widget.MaterialComponents.Button.TextButton"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginTop="8dp"
            android:text="Delete"
            android:textColor="@android:color/holo_red_dark" />
    </LinearLayout>
</com.google.android.material.card.MaterialCardView>
```

`PasskeyAdapter.java`:

```java
package com.springboot.android.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.springboot.android.R;
import com.springboot.android.model.Passkey;

import java.util.List;

public class PasskeyAdapter extends RecyclerView.Adapter<PasskeyAdapter.ViewHolder> {
    private List<Passkey> passkeys;
    private final OnItemClickListener<Passkey> deleteListener;

    public interface OnItemClickListener<T> {
        void onClick(T item);
    }

    public PasskeyAdapter(List<Passkey> passkeys, OnItemClickListener<Passkey> deleteListener) {
        this.passkeys = passkeys;
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_passkey, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Passkey passkey = passkeys.get(position);
        holder.tvLabel.setText(passkey.getLabel() != null ? passkey.getLabel() : "");
        holder.tvCreated.setText(passkey.getCreated() != null ? "Created: " + passkey.getCreated() : "");
        holder.btnDelete.setOnClickListener(v -> deleteListener.onClick(passkey));
    }

    @Override
    public int getItemCount() {
        return passkeys.size();
    }

    public void updateData(List<Passkey> newData) {
        this.passkeys = newData;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvLabel, tvCreated;
        MaterialButton btnDelete;

        ViewHolder(View itemView) {
            super(itemView);
            tvLabel = itemView.findViewById(R.id.tvLabel);
            tvCreated = itemView.findViewById(R.id.tvCreated);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
```

`PasskeyListActivity.java`:

```java
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
```

- [ ] **Step 5: Registration screen (activity_passkey_form.xml, PasskeyFormActivity.java)**

`activity_passkey_form.xml` (same shape as `activity_category_form.xml`, one label field):

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.coordinatorlayout.widget.CoordinatorLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <com.google.android.material.appbar.AppBarLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content">

        <androidx.appcompat.widget.Toolbar
            android:id="@+id/toolbar"
            android:layout_width="match_parent"
            android:layout_height="?attr/actionBarSize"
            android:background="?attr/colorPrimary"
            app:title="Add Passkey" />
    </com.google.android.material.appbar.AppBarLayout>

    <androidx.core.widget.NestedScrollView
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        app:layout_behavior="@string/appbar_scrolling_view_behavior">

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical"
            android:padding="16dp">

            <com.google.android.material.textfield.TextInputLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginBottom="16dp">

                <com.google.android.material.textfield.TextInputEditText
                    android:id="@+id/etLabel"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:hint="Label" />
            </com.google.android.material.textfield.TextInputLayout>

            <com.google.android.material.button.MaterialButton
                android:id="@+id/btnRegister"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:text="Register" />

            <ProgressBar
                android:id="@+id/progressBar"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_gravity="center"
                android:layout_marginTop="16dp"
                android:visibility="gone" />
        </LinearLayout>
    </androidx.core.widget.NestedScrollView>
</androidx.coordinatorlayout.widget.CoordinatorLayout>
```

`PasskeyFormActivity.java` — fetches the server challenge, then calls `CredentialManager.createCredential` with a `CreatePublicKeyCredentialRequest`, mirroring `PasskeyEdit.js`'s `handleSubmit` (challenge → browser WebAuthn call → send registration back to server):

```java
package com.springboot.android.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.credentials.CreatePublicKeyCredentialRequest;
import androidx.credentials.CreatePublicKeyCredentialResponse;
import androidx.credentials.CredentialManager;
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
                new androidx.credentials.CredentialManagerCallback<androidx.credentials.CreateCredentialResponse, CreateCredentialException>() {
                    @Override
                    public void onResult(androidx.credentials.CreateCredentialResponse result) {
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
```

- [ ] **Step 6: Passkey sign-in on the login screen**

Add to `activity_login.xml` (near the existing `btnGoogleLogin`):

```xml
        <com.google.android.material.button.MaterialButton
            android:id="@+id/btnPasskeyLogin"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="8dp"
            android:text="Sign in with a passkey" />
```

Add to `LoginActivity.java` — a `CredentialManager.getCredentialAsync` call using `GetPublicKeyCredentialOption`, mirroring `Login.js`'s `handlePasskey()` (get challenge → browser WebAuthn assert → `login/webauthn` → re-check `authenticatedUser`):

```java
    private CredentialManager credentialManager;
    private PasskeyService passkeyService;
```

```java
        credentialManager = CredentialManager.create(this);
        passkeyService = ApiClient.getClient().create(PasskeyService.class);

        MaterialButton btnPasskeyLogin = findViewById(R.id.btnPasskeyLogin);
        btnPasskeyLogin.setOnClickListener(v -> performPasskeyLogin());
```

```java
    private void performPasskeyLogin() {
        progressBar.setVisibility(View.VISIBLE);
        btnLogin.setEnabled(false);

        passkeyService.getAuthenticateOptions().enqueue(new Callback<com.google.gson.JsonObject>() {
            @Override
            public void onResponse(Call<com.google.gson.JsonObject> call, Response<com.google.gson.JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    getPasskeyAssertion(response.body());
                } else {
                    finishPasskeyLoginWithError("Failed to get passkey challenge");
                }
            }

            @Override
            public void onFailure(Call<com.google.gson.JsonObject> call, Throwable t) {
                finishPasskeyLoginWithError("Error: " + t.getMessage());
            }
        });
    }

    private void getPasskeyAssertion(com.google.gson.JsonObject challengeOptions) {
        androidx.credentials.GetPublicKeyCredentialOption option =
                new androidx.credentials.GetPublicKeyCredentialOption(challengeOptions.toString());
        androidx.credentials.GetCredentialRequest request =
                new androidx.credentials.GetCredentialRequest.Builder()
                        .addCredentialOption(option)
                        .build();

        credentialManager.getCredentialAsync(
                this,
                request,
                null,
                getMainExecutor(),
                new androidx.credentials.CredentialManagerCallback<androidx.credentials.GetCredentialResponse, androidx.credentials.exceptions.GetCredentialException>() {
                    @Override
                    public void onResult(androidx.credentials.GetCredentialResponse result) {
                        androidx.credentials.PublicKeyCredential credential =
                                (androidx.credentials.PublicKeyCredential) result.getCredential();
                        com.google.gson.JsonObject assertion = com.google.gson.JsonParser
                                .parseString(credential.getAuthenticationResponseJson()).getAsJsonObject();
                        sendPasskeyAssertion(assertion);
                    }

                    @Override
                    public void onError(androidx.credentials.exceptions.GetCredentialException e) {
                        finishPasskeyLoginWithError("Passkey sign-in failed: " + e.getMessage());
                    }
                });
    }

    private void sendPasskeyAssertion(com.google.gson.JsonObject assertion) {
        passkeyService.loginWithPasskey(assertion).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                progressBar.setVisibility(View.GONE);
                btnLogin.setEnabled(true);
                if (response.isSuccessful()) {
                    checkIfAlreadyAuthenticated();
                } else {
                    Toast.makeText(LoginActivity.this, "Passkey sign-in failed", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                finishPasskeyLoginWithError("Error: " + t.getMessage());
            }
        });
    }

    private void finishPasskeyLoginWithError(String message) {
        progressBar.setVisibility(View.GONE);
        btnLogin.setEnabled(true);
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
```

- [ ] **Step 7: Add nav_passkeys to drawer_menu.xml and DashboardActivity**

In `drawer_menu.xml`, add under the existing "Account" group (self-service, no role gate — same as react-webapp):

```xml
            <item
                android:id="@+id/nav_passkeys"
                android:icon="@android:drawable/ic_lock_lock"
                android:title="Passkeys" />
```

In `DashboardActivity.onNavigationItemSelected`:

```java
        } else if (id == R.id.nav_passkeys) {
            startActivity(new Intent(this, PasskeyListActivity.class));
```

- [ ] **Step 8: Register new activities in AndroidManifest.xml**

```xml
        <activity
            android:name=".ui.PasskeyListActivity"
            android:exported="false"
            android:label="Passkeys"
            android:parentActivityName=".ui.DashboardActivity" />
        <activity
            android:name=".ui.PasskeyFormActivity"
            android:exported="false"
            android:label="Add Passkey"
            android:parentActivityName=".ui.PasskeyListActivity" />
```

- [ ] **Step 9: Build to verify**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 10: Commit changes**

```bash
git add android-webapp/app/build.gradle android-webapp/app/src/main/java/com/springboot/android/model/Passkey.java android-webapp/app/src/main/java/com/springboot/android/api/PasskeyService.java android-webapp/app/src/main/res/layout/activity_passkey_list.xml android-webapp/app/src/main/res/layout/item_passkey.xml android-webapp/app/src/main/java/com/springboot/android/ui/PasskeyAdapter.java android-webapp/app/src/main/java/com/springboot/android/ui/PasskeyListActivity.java android-webapp/app/src/main/res/layout/activity_passkey_form.xml android-webapp/app/src/main/java/com/springboot/android/ui/PasskeyFormActivity.java android-webapp/app/src/main/res/layout/activity_login.xml android-webapp/app/src/main/java/com/springboot/android/ui/LoginActivity.java android-webapp/app/src/main/res/menu/drawer_menu.xml android-webapp/app/src/main/java/com/springboot/android/ui/DashboardActivity.java android-webapp/app/src/main/AndroidManifest.xml
git commit -m "feat: add passkey (WebAuthn) registration, list, delete, and login" --trailer "Co-authored-by: Junie <junie@jetbrains.com>"
```

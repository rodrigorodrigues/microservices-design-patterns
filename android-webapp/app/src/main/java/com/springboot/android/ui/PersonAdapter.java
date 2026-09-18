package com.springboot.android.ui;

import android.app.AlertDialog;
import android.content.Context;
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

        // Display address if available
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

        // Hide/show buttons based on permissions
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

    private void showChildrenDialog(Context context, Person person) {
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

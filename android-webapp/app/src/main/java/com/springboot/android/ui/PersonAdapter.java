package com.springboot.android.ui;

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

    public interface OnItemClickListener<T> {
        void onClick(T item);
    }

    public PersonAdapter(List<Person> persons, OnItemClickListener<Person> editListener, OnItemClickListener<Person> deleteListener) {
        this.persons = persons;
        this.editListener = editListener;
        this.deleteListener = deleteListener;
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

        holder.btnEdit.setOnClickListener(v -> editListener.onClick(person));
        holder.btnDelete.setOnClickListener(v -> deleteListener.onClick(person));
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
        MaterialButton btnEdit, btnDelete;

        ViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvEmail = itemView.findViewById(R.id.tvEmail);
            tvPhone = itemView.findViewById(R.id.tvPhone);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}

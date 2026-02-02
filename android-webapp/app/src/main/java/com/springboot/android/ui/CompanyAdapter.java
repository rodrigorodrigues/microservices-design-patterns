package com.springboot.android.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.springboot.android.R;
import com.springboot.android.model.Company;

import java.util.List;

public class CompanyAdapter extends RecyclerView.Adapter<CompanyAdapter.ViewHolder> {
    private List<Company> companies;
    private final OnItemClickListener<Company> editListener;
    private final OnItemClickListener<Company> deleteListener;

    public interface OnItemClickListener<T> {
        void onClick(T item);
    }

    public CompanyAdapter(List<Company> companies, OnItemClickListener<Company> editListener, OnItemClickListener<Company> deleteListener) {
        this.companies = companies;
        this.editListener = editListener;
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_company, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Company company = companies.get(position);
        holder.tvName.setText(company.getName());
        holder.tvEmail.setText(company.getEmail() != null ? company.getEmail() : "");
        holder.tvPhone.setText(company.getPhone() != null ? company.getPhone() : "");

        holder.btnEdit.setOnClickListener(v -> editListener.onClick(company));
        holder.btnDelete.setOnClickListener(v -> deleteListener.onClick(company));
    }

    @Override
    public int getItemCount() {
        return companies.size();
    }

    public void updateData(List<Company> newData) {
        this.companies = newData;
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

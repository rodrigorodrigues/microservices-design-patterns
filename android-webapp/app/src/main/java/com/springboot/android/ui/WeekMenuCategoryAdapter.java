package com.springboot.android.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.springboot.android.R;
import com.springboot.android.model.Category;

import java.util.List;

public class WeekMenuCategoryAdapter extends RecyclerView.Adapter<WeekMenuCategoryAdapter.ViewHolder> {
    private List<Category> categories;
    private final OnItemClickListener<Category> editListener;
    private final OnItemClickListener<Category> deleteListener;
    private boolean hasSaveAccess = true;
    private boolean hasDeleteAccess = true;

    public interface OnItemClickListener<T> {
        void onClick(T item);
    }

    public WeekMenuCategoryAdapter(List<Category> categories, OnItemClickListener<Category> editListener, OnItemClickListener<Category> deleteListener) {
        this.categories = categories;
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
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_week_menu_category, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Category category = categories.get(position);
        holder.tvName.setText(category.getName());

        // Display products if available
        if (category.getProducts() != null && !category.getProducts().isEmpty()) {
            StringBuilder productsText = new StringBuilder("Products: ");
            for (int i = 0; i < category.getProducts().size(); i++) {
                if (i > 0) productsText.append(", ");
                productsText.append(category.getProducts().get(i).getName());
            }
            holder.tvProducts.setText(productsText.toString());
            holder.tvProducts.setVisibility(View.VISIBLE);
        } else {
            holder.tvProducts.setVisibility(View.GONE);
        }

        // Hide/show buttons based on permissions
        if (hasSaveAccess) {
            holder.btnEdit.setVisibility(View.VISIBLE);
            holder.btnEdit.setOnClickListener(v -> editListener.onClick(category));
        } else {
            holder.btnEdit.setVisibility(View.GONE);
        }

        if (hasDeleteAccess) {
            holder.btnDelete.setVisibility(View.VISIBLE);
            holder.btnDelete.setOnClickListener(v -> deleteListener.onClick(category));
        } else {
            holder.btnDelete.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    public void updateData(List<Category> newData) {
        this.categories = newData;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvProducts;
        MaterialButton btnEdit, btnDelete;

        ViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvProducts = itemView.findViewById(R.id.tvProducts);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}

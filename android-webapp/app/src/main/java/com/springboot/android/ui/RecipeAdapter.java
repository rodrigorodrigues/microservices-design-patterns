package com.springboot.android.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.springboot.android.R;
import com.springboot.android.model.Recipe;

import java.util.List;

public class RecipeAdapter extends RecyclerView.Adapter<RecipeAdapter.ViewHolder> {
    private List<Recipe> recipes;
    private final OnItemClickListener<Recipe> editListener;
    private final OnItemClickListener<Recipe> deleteListener;
    private boolean hasSaveAccess = true;
    private boolean hasDeleteAccess = true;

    public interface OnItemClickListener<T> {
        void onClick(T item);
    }

    public RecipeAdapter(List<Recipe> recipes, OnItemClickListener<Recipe> editListener, OnItemClickListener<Recipe> deleteListener) {
        this.recipes = recipes;
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
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_recipe, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Recipe recipe = recipes.get(position);
        holder.tvName.setText(recipe.getName());

        // Display categories if available
        if (recipe.getCategories() != null && !recipe.getCategories().isEmpty()) {
            StringBuilder categoriesText = new StringBuilder("Categories: ");
            for (int i = 0; i < recipe.getCategories().size(); i++) {
                if (i > 0) categoriesText.append(", ");
                categoriesText.append(recipe.getCategories().get(i).getName());
            }
            holder.tvCategories.setText(categoriesText.toString());
            holder.tvCategories.setVisibility(View.VISIBLE);
        } else {
            holder.tvCategories.setVisibility(View.GONE);
        }

        // Hide/show Edit button based on permissions
        if (hasSaveAccess) {
            holder.btnEdit.setVisibility(View.VISIBLE);
            holder.btnEdit.setOnClickListener(v -> editListener.onClick(recipe));
        } else {
            holder.btnEdit.setVisibility(View.GONE);
        }

        // Hide/show Delete button based on permissions
        if (hasDeleteAccess) {
            holder.btnDelete.setVisibility(View.VISIBLE);
            holder.btnDelete.setOnClickListener(v -> deleteListener.onClick(recipe));
        } else {
            holder.btnDelete.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return recipes.size();
    }

    public void updateData(List<Recipe> newData) {
        this.recipes = newData;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvCategories;
        MaterialButton btnEdit, btnDelete;

        ViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvCategories = itemView.findViewById(R.id.tvCategories);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}

package com.springboot.android.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.springboot.android.R;
import com.springboot.android.model.Product;

import java.util.List;
import java.util.Locale;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ViewHolder> {
    private List<Product> products;
    private final OnItemClickListener<Product> editListener;
    private final OnItemClickListener<Product> deleteListener;

    public interface OnItemClickListener<T> {
        void onClick(T item);
    }

    public ProductAdapter(List<Product> products, OnItemClickListener<Product> editListener, OnItemClickListener<Product> deleteListener) {
        this.products = products;
        this.editListener = editListener;
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_product, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Product product = products.get(position);
        holder.tvName.setText(product.getName());

        String priceText = String.format(Locale.getDefault(), "$%.2f", product.getPrice());
        holder.tvPrice.setText(priceText);

        String quantityText = "Qty: " + product.getQuantity();
        holder.tvQuantity.setText(quantityText);

        holder.btnEdit.setOnClickListener(v -> editListener.onClick(product));
        holder.btnDelete.setOnClickListener(v -> deleteListener.onClick(product));
    }

    @Override
    public int getItemCount() {
        return products.size();
    }

    public void updateData(List<Product> newData) {
        this.products = newData;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvPrice, tvQuantity;
        MaterialButton btnEdit, btnDelete;

        ViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvQuantity = itemView.findViewById(R.id.tvQuantity);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}

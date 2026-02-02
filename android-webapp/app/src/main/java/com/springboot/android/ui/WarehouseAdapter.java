package com.springboot.android.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.springboot.android.R;
import com.springboot.android.model.Warehouse;

import java.util.List;
import java.util.Locale;

public class WarehouseAdapter extends RecyclerView.Adapter<WarehouseAdapter.ViewHolder> {
    private List<Warehouse> warehouses;
    private final OnItemClickListener<Warehouse> editListener;
    private final OnItemClickListener<Warehouse> deleteListener;

    public interface OnItemClickListener<T> {
        void onClick(T item);
    }

    public WarehouseAdapter(List<Warehouse> warehouses, OnItemClickListener<Warehouse> editListener, OnItemClickListener<Warehouse> deleteListener) {
        this.warehouses = warehouses;
        this.editListener = editListener;
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_warehouse, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Warehouse warehouse = warehouses.get(position);
        holder.tvName.setText(warehouse.getName() != null ? warehouse.getName() : "");

        String quantityText = "Qty: " + warehouse.getQuantity();
        holder.tvQuantity.setText(quantityText);

        String currency = warehouse.getCurrency() != null ? warehouse.getCurrency() : "USD";
        String priceText = String.format(Locale.getDefault(), "%.2f %s", warehouse.getPrice(), currency);
        holder.tvPrice.setText(priceText);

        holder.btnEdit.setOnClickListener(v -> editListener.onClick(warehouse));
        holder.btnDelete.setOnClickListener(v -> deleteListener.onClick(warehouse));
    }

    @Override
    public int getItemCount() {
        return warehouses.size();
    }

    public void updateData(List<Warehouse> newData) {
        this.warehouses = newData;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvQuantity, tvPrice;
        MaterialButton btnEdit, btnDelete;

        ViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvQuantity = itemView.findViewById(R.id.tvQuantity);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}

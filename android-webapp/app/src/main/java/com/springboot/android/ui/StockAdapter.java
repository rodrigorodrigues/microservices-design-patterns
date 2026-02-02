package com.springboot.android.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.springboot.android.R;
import com.springboot.android.model.Stock;

import java.util.List;
import java.util.Locale;

public class StockAdapter extends RecyclerView.Adapter<StockAdapter.ViewHolder> {
    private List<Stock> stocks;
    private final OnItemClickListener<Stock> editListener;
    private final OnItemClickListener<Stock> deleteListener;

    public interface OnItemClickListener<T> {
        void onClick(T item);
    }

    public StockAdapter(List<Stock> stocks, OnItemClickListener<Stock> editListener, OnItemClickListener<Stock> deleteListener) {
        this.stocks = stocks;
        this.editListener = editListener;
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_stock, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Stock stock = stocks.get(position);
        holder.tvName.setText(stock.getName() != null ? stock.getName() : "");

        String quantityText = "Qty: " + stock.getQuantity();
        holder.tvQuantity.setText(quantityText);

        String currency = stock.getCurrency() != null ? stock.getCurrency() : "USD";
        String priceText = String.format(Locale.getDefault(), "%.2f %s", stock.getPrice(), currency);
        holder.tvPrice.setText(priceText);

        holder.btnEdit.setOnClickListener(v -> editListener.onClick(stock));
        holder.btnDelete.setOnClickListener(v -> deleteListener.onClick(stock));
    }

    @Override
    public int getItemCount() {
        return stocks.size();
    }

    public void updateData(List<Stock> newData) {
        this.stocks = newData;
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

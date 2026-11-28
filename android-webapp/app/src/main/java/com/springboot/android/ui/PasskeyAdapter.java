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

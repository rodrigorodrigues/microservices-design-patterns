package com.springboot.android.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.springboot.android.R;
import com.springboot.android.model.User;

import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.ViewHolder> {
    private List<User> users;
    private final OnItemClickListener<User> editListener;
    private final OnItemClickListener<User> deleteListener;
    private boolean hasSaveAccess = true;
    private boolean hasDeleteAccess = true;

    public interface OnItemClickListener<T> {
        void onClick(T item);
    }

    public UserAdapter(List<User> users, OnItemClickListener<User> editListener, OnItemClickListener<User> deleteListener) {
        this.users = users;
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
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User user = users.get(position);
        holder.tvUsername.setText(user.getUsername() != null ? user.getUsername() : "");
        holder.tvEmail.setText(user.getEmail() != null ? user.getEmail() : "");
        holder.tvActivated.setText(user.isActivated() ? "Active" : "Inactive");

        // Hide/show buttons based on permissions
        if (hasSaveAccess) {
            holder.btnEdit.setVisibility(View.VISIBLE);
            holder.btnEdit.setOnClickListener(v -> editListener.onClick(user));
        } else {
            holder.btnEdit.setVisibility(View.GONE);
        }

        if (hasDeleteAccess) {
            holder.btnDelete.setVisibility(View.VISIBLE);
            holder.btnDelete.setOnClickListener(v -> deleteListener.onClick(user));
        } else {
            holder.btnDelete.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    public void updateData(List<User> newData) {
        this.users = newData;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvUsername, tvEmail, tvActivated;
        MaterialButton btnEdit, btnDelete;

        ViewHolder(View itemView) {
            super(itemView);
            tvUsername = itemView.findViewById(R.id.tvUsername);
            tvEmail = itemView.findViewById(R.id.tvEmail);
            tvActivated = itemView.findViewById(R.id.tvActivated);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}

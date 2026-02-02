package com.springboot.android.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.springboot.android.R;
import com.springboot.android.model.Task;

import java.util.List;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.ViewHolder> {
    private List<Task> tasks;
    private final OnItemClickListener<Task> editListener;
    private final OnItemClickListener<Task> deleteListener;

    public interface OnItemClickListener<T> {
        void onClick(T item);
    }

    public TaskAdapter(List<Task> tasks, OnItemClickListener<Task> editListener, OnItemClickListener<Task> deleteListener) {
        this.tasks = tasks;
        this.editListener = editListener;
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_task, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Task task = tasks.get(position);
        holder.tvName.setText(task.getName());
        holder.tvDescription.setText(task.getDescription() != null ? task.getDescription() : "");

        holder.btnEdit.setOnClickListener(v -> editListener.onClick(task));
        holder.btnDelete.setOnClickListener(v -> deleteListener.onClick(task));
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }

    public void updateData(List<Task> newData) {
        this.tasks = newData;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvDescription;
        MaterialButton btnEdit, btnDelete;

        ViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}

package com.springboot.android.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.springboot.android.R;
import com.springboot.android.model.Post;

import java.util.List;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.ViewHolder> {
    private List<Post> posts;
    private final OnItemClickListener<Post> editListener;
    private final OnItemClickListener<Post> deleteListener;

    public interface OnItemClickListener<T> {
        void onClick(T item);
    }

    public PostAdapter(List<Post> posts, OnItemClickListener<Post> editListener, OnItemClickListener<Post> deleteListener) {
        this.posts = posts;
        this.editListener = editListener;
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_post, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Post post = posts.get(position);
        holder.tvName.setText(post.getName());

        // Display tasks if available
        if (post.getTasks() != null && !post.getTasks().isEmpty()) {
            StringBuilder tasksText = new StringBuilder("Tasks: ");
            for (int i = 0; i < post.getTasks().size(); i++) {
                if (i > 0) tasksText.append(", ");
                tasksText.append(post.getTasks().get(i).getName());
            }
            holder.tvTasks.setText(tasksText.toString());
            holder.tvTasks.setVisibility(View.VISIBLE);
        } else {
            holder.tvTasks.setVisibility(View.GONE);
        }

        holder.btnEdit.setOnClickListener(v -> editListener.onClick(post));
        holder.btnDelete.setOnClickListener(v -> deleteListener.onClick(post));
    }

    @Override
    public int getItemCount() {
        return posts.size();
    }

    public void updateData(List<Post> newData) {
        this.posts = newData;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvTasks;
        MaterialButton btnEdit, btnDelete;

        ViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvTasks = itemView.findViewById(R.id.tvTasks);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}

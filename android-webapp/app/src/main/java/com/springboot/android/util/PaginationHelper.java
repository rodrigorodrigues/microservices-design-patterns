package com.springboot.android.util;

import android.view.View;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;
import com.springboot.android.R;

public class PaginationHelper {
    private final View paginationView;
    private final MaterialButton btnFirst;
    private final MaterialButton btnPrevious;
    private final MaterialButton btnNext;
    private final MaterialButton btnLast;
    private final TextView tvPageInfo;
    private final TextView tvTotal;

    private int currentPage = 0;
    private int totalPages = 0;
    private long totalElements = 0;
    private PaginationListener listener;

    public interface PaginationListener {
        void onPageChange(int page);
    }

    public PaginationHelper(View paginationView, PaginationListener listener) {
        this.paginationView = paginationView;
        this.listener = listener;

        btnFirst = paginationView.findViewById(R.id.btnFirst);
        btnPrevious = paginationView.findViewById(R.id.btnPrevious);
        btnNext = paginationView.findViewById(R.id.btnNext);
        btnLast = paginationView.findViewById(R.id.btnLast);
        tvPageInfo = paginationView.findViewById(R.id.tvPageInfo);
        tvTotal = paginationView.findViewById(R.id.tvTotal);

        setupListeners();
    }

    private void setupListeners() {
        btnFirst.setOnClickListener(v -> {
            if (currentPage > 0) {
                listener.onPageChange(0);
            }
        });

        btnPrevious.setOnClickListener(v -> {
            if (currentPage > 0) {
                listener.onPageChange(currentPage - 1);
            }
        });

        btnNext.setOnClickListener(v -> {
            if (currentPage < totalPages - 1) {
                listener.onPageChange(currentPage + 1);
            }
        });

        btnLast.setOnClickListener(v -> {
            if (currentPage < totalPages - 1) {
                listener.onPageChange(totalPages - 1);
            }
        });
    }

    public void updatePagination(int currentPage, int totalPages, long totalElements) {
        this.currentPage = currentPage;
        this.totalPages = totalPages;
        this.totalElements = totalElements;

        tvPageInfo.setText(String.format("Page %d of %d", currentPage + 1, totalPages));
        tvTotal.setText(String.format("Total: %d", totalElements));

        btnFirst.setEnabled(currentPage > 0);
        btnPrevious.setEnabled(currentPage > 0);
        btnNext.setEnabled(currentPage < totalPages - 1);
        btnLast.setEnabled(currentPage < totalPages - 1);

        paginationView.setVisibility(totalPages > 1 ? View.VISIBLE : View.GONE);
    }

    public void hide() {
        paginationView.setVisibility(View.GONE);
    }
}

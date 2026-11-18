package com.example.kalkulator;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * RecyclerView adapter displaying calculation history items.
 */
public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    public interface OnHistoryActionListener {
        void onItemClick(HistoryItem item);
        void onUseClick(HistoryItem item);
    }

    private final List<HistoryItem> items = new ArrayList<>();
    private final OnHistoryActionListener listener;
    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault());

    public HistoryAdapter(OnHistoryActionListener listener) {
        this.listener = listener;
    }

    public void submitList(List<HistoryItem> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HistoryItem item = items.get(position);
        holder.tvCategory.setText(item.getCategory());
        holder.tvTimestamp.setText(dateFormat.format(new Date(item.getTimestamp())));
        holder.tvExpression.setText(item.getExpression());
        holder.tvResult.setText("= " + item.getResult());

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(item);
            }
        });
        holder.btnUseValue.setOnClickListener(v -> {
            if (listener != null) {
                listener.onUseClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvCategory;
        final TextView tvTimestamp;
        final TextView tvExpression;
        final TextView tvResult;
        final MaterialButton btnUseValue;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
            tvExpression = itemView.findViewById(R.id.tvExpression);
            tvResult = itemView.findViewById(R.id.tvResult);
            btnUseValue = itemView.findViewById(R.id.btnUseValue);
        }
    }
}

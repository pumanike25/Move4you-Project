package com.example.maps4u;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder> {

    private List<RouteHistory> historyList;

    public HistoryAdapter(List<RouteHistory> historyList) {
        this.historyList = historyList;
    }

    public void updateData(List<RouteHistory> newHistory) {
        this.historyList = newHistory;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_history, parent, false);
        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        if (historyList != null && position < historyList.size()) {
            holder.bindData(historyList.get(position));
        }
    }

    @Override
    public int getItemCount() {
        return historyList != null ? historyList.size() : 0;
    }

    public static class HistoryViewHolder extends RecyclerView.ViewHolder {
        private final TextView originText;
        private final TextView destinationText;
        private final TextView timestampText;
        private final TextView modeText;

        public HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            originText = itemView.findViewById(R.id.history_origin);
            destinationText = itemView.findViewById(R.id.history_destination);
            timestampText = itemView.findViewById(R.id.history_timestamp);
            modeText = itemView.findViewById(R.id.history_mode);
        }

        public void bindData(RouteHistory route) {
            originText.setText(route.getOrigin());
            destinationText.setText(route.getDestination());

            String mode = route.getTransportMode() != null ? route.getTransportMode() : "car";
            switch (mode) {
                case "car": modeText.setText("🚗 By car"); break;
                case "walking": modeText.setText("🚶‍♂️ Walking"); break;
                case "bicycle": modeText.setText("🚴‍♂️ By bicycle"); break;
                default: modeText.setText("Transport: " + mode);
            }

            String dateStr = "N/A";
            if (route.getTimestamp() != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault());
                dateStr = sdf.format(route.getTimestamp());
            }

            timestampText.setText(dateStr);
        }
    }
}
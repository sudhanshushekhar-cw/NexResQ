package com.example.nexresq;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.libraries.places.api.model.AutocompletePrediction;

import java.util.List;

public class SuggestionAdapter extends RecyclerView.Adapter<SuggestionAdapter.SuggestionViewHolder> {

    private List<AutocompletePrediction> predictions;
    private final OnSuggestionClickListener listener;

    public interface OnSuggestionClickListener {
        void onSuggestionClick(AutocompletePrediction prediction);
    }

    public SuggestionAdapter(List<AutocompletePrediction> predictions, OnSuggestionClickListener listener) {
        this.predictions = predictions;
        this.listener = listener;
    }

    public void updateData(List<AutocompletePrediction> newPredictions) {
        this.predictions = newPredictions;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SuggestionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_1, parent, false);
        return new SuggestionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SuggestionViewHolder holder, int position) {
        AutocompletePrediction prediction = predictions.get(position);
        holder.textView.setText(prediction.getPrimaryText(null));
        holder.itemView.setOnClickListener(v -> listener.onSuggestionClick(prediction));
    }

    @Override
    public int getItemCount() {
        return predictions != null ? predictions.size() : 0;
    }

    static class SuggestionViewHolder extends RecyclerView.ViewHolder {
        TextView textView;
        public SuggestionViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(android.R.id.text1);
        }
    }
}

package com.example.smokedetection;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class AlertAdapter extends RecyclerView.Adapter<AlertVH> {
    private final List<SmokeEvent> items;

    public AlertAdapter(List<SmokeEvent> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public AlertVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        var v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_alert, parent, false);
        return new AlertVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull AlertVH holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }
}

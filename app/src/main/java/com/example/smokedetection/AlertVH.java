package com.example.smokedetection;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class AlertVH extends RecyclerView.ViewHolder {
    private final TextView tvMain;
    private final TextView tvSub;

    public AlertVH(@NonNull View itemView) {
        super(itemView);
        tvMain = itemView.findViewById(R.id.tvMain);
        tvSub = itemView.findViewById(R.id.tvSub);
    }

    public void bind(SmokeEvent e) {
        tvMain.setText(e.title);
        tvSub.setText(e.subtitle);
    }
}

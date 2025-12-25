package com.example.smokedetection;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.ArrayList;

import com.example.smokedetection.databinding.ActivityDetectorBinding;

public class DetectorActivity extends AppCompatActivity {

    private ActivityDetectorBinding b;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        b = ActivityDetectorBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        b.btnBack.setOnClickListener(v -> finish());

        // Fake log
        var log = new ArrayList<SmokeEvent>();
        log.add(new SmokeEvent("Monitoring", "Now • Confidence 0.18"));
        log.add(new SmokeEvent("No smoke detected", "10s ago • Confidence 0.11"));
        log.add(new SmokeEvent("No smoke detected", "25s ago • Confidence 0.09"));

        b.rvLog.setLayoutManager(new LinearLayoutManager(this));
        b.rvLog.setAdapter(new AlertAdapter(log));

        b.progressConfidence.setProgressCompat(18, true);
        b.tvConfidence.setText("Smoke confidence: 0.18");
    }
}

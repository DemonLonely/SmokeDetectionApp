package com.example.smokedetection;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.smokedetection.databinding.ActivityDashboardBinding;

import java.util.ArrayList;

public class DashboardActivity extends AppCompatActivity {

    private ActivityDashboardBinding b;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        b = ActivityDashboardBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        // Fake data for UI preview
        ArrayList<SmokeEvent> data = new ArrayList<>();
        data.add(new SmokeEvent("No smoke detected", "Just now • Confidence 0.18"));
        data.add(new SmokeEvent("Monitoring stable", "2 min ago • Confidence 0.12"));
        data.add(new SmokeEvent("Camera connected", "5 min ago • Ready"));

        b.rvAlerts.setLayoutManager(new LinearLayoutManager(this));
        b.rvAlerts.setAdapter(new AlertAdapter(data));

        // ✅ Open correct activities (MATCH your AndroidManifest.xml)
        b.btnImage.setOnClickListener(v -> open(ImageDetectActivity.class));
        b.btnVideo.setOnClickListener(v -> open(VideoDetectActivity.class));
        b.btnLive.setOnClickListener(v -> open(LiveCamActivity.class));
    }

    private void open(Class<?> cls) {
        try {
            startActivity(new Intent(DashboardActivity.this, cls));
        } catch (Exception e) {
            Toast.makeText(this, "Cannot open screen: " + cls.getSimpleName(), Toast.LENGTH_SHORT).show();
        }
    }
}

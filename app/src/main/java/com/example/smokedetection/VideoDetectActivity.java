package com.example.smokedetection;

import android.net.Uri;
import android.os.Bundle;
import android.widget.MediaController;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.smokedetection.databinding.ActivityDetectVideoBinding;

public class VideoDetectActivity extends AppCompatActivity {

    private ActivityDetectVideoBinding b;
    private Uri selectedVideoUri;

    private ActivityResultLauncher<String> pickVideoLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityDetectVideoBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        pickVideoLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        selectedVideoUri = uri;
                        b.tvStatus.setText("Selected: " + uri);

                        MediaController mediaController = new MediaController(this);
                        mediaController.setAnchorView(b.videoPreview);
                        b.videoPreview.setMediaController(mediaController);
                        b.videoPreview.setVideoURI(uri);
                        b.videoPreview.start();
                    }
                }
        );

        b.btnPickVideo.setOnClickListener(v -> pickVideoLauncher.launch("video/*"));

        b.btnDetectVideo.setOnClickListener(v -> {
            if (selectedVideoUri == null) {
                Toast.makeText(this, "Pick a video first!", Toast.LENGTH_SHORT).show();
                return;
            }

            b.tvStatus.setText("Running detection...");

            // TODO: GẮN DETECT VIDEO THẬT Ở ĐÂY
            // startActivity(new Intent(this, ResultActivity.class)
            //         .putExtra("type", "video")
            //         .putExtra("uri", selectedVideoUri.toString()));

            b.tvStatus.setText("Done (UI flow OK). Plug your video detection here.");
        });
    }
}

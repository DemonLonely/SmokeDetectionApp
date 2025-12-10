package com.example.smokedetection;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private static final String SUPABASE_URL = "https://gklovokybxmonmnuoflk.supabase.co";
    private static final String SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImdrbG92b2t5Ynhtb25tbnVvZmxrIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjUzNTY0NTcsImV4cCI6MjA4MDkzMjQ1N30.rgEFaf0M0WBKNbaintilKHILM3HS4Dnpb40wSbj_hZA";

    private SupabaseClient supabase;
    private boolean isGuest = true;
    private String currentUserId = null; // Store UUID if log in

    private Button btnUploadImage, btnUploadVideo, btnLogout;

    // Gallery Launcher
    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri selectedUri = result.getData().getData();
                    if (selectedUri != null) {
                        // Check if it is video or image based on the request
                        String type = getContentResolver().getType(selectedUri);
                        boolean isVideo = type != null && type.startsWith("video");
                        processFile(selectedUri, isVideo);
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Init Supabase
        supabase = new SupabaseClient(SUPABASE_URL, SUPABASE_KEY);
        checkLoginStatus();

        btnUploadImage = findViewById(R.id.btnUploadImage);
        btnUploadVideo = findViewById(R.id.btnUploadVideo);
        btnLogout = findViewById(R.id.btnLogout);

        btnUploadImage.setOnClickListener(v -> openGallery("image/*"));
        btnUploadVideo.setOnClickListener(v -> openGallery("video/*"));

        btnLogout.setOnClickListener(v -> logout());
    }

    private void checkLoginStatus() {
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        String token = prefs.getString("auth_token", null);

        if (token != null) {
            isGuest = false;
            supabase.setAuthToken(token); // Guest mode -> User mode
        } else {
            isGuest = true;
        }
    }

    private void openGallery(String type) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType(type);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        galleryLauncher.launch(Intent.createChooser(intent, "Select File"));
    }

    // Upload to database
    private void processFile(Uri uri, boolean isVideo) {
        Toast.makeText(this, "Starting Upload...", Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            try {
                // Convert URI to bytes
                byte[] fileBytes = getBytesFromUri(uri);
                if (fileBytes == null) return;

                // Determine bucket's name
                String bucket;
                if (isGuest) {
                    bucket = isVideo ? "guest-videos" : "guest-images";
                } else {
                    bucket = isVideo ? "user-videos" : "user-images";
                }

                // Create unique filename
                String extension = isVideo ? ".mp4" : ".jpg";
                String fileName = System.currentTimeMillis() + extension;

                // Upload file to storage
                supabase.uploadFile(bucket, fileName, fileBytes, new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        runOnUiThread(() -> Toast.makeText(MainActivity.this, "Upload Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    }

                    @Override
                    public void onResponse(Call call, Response response) {
                        if (response.isSuccessful()) {
                            // Upload success -> database
                            saveMetadataToDatabase(bucket, fileName);
                        } else {
                            runOnUiThread(() -> Toast.makeText(MainActivity.this, "Storage Error: " + response.code(), Toast.LENGTH_SHORT).show());
                        }
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void saveMetadataToDatabase(String bucket, String filePath) {
        // Create JSON for database
        String json = "{"
                + "\"bucket_name\": \"" + bucket + "\","
                + "\"file_path\": \"" + filePath + "\""
                + "}";

        supabase.insert("media_uploads", json, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "DB Error", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) {
                runOnUiThread(() -> {
                    if (response.isSuccessful()) {
                        Toast.makeText(MainActivity.this, "Upload Complete!", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(MainActivity.this, "DB Save Failed", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void logout() {
        getSharedPreferences("AppPrefs", MODE_PRIVATE).edit().clear().apply();
        startActivity(new Intent(MainActivity.this, LoginActivity.class));
        finish();
    }

    // Help read file data
    private byte[] getBytesFromUri(Uri uri) throws IOException {
        InputStream iStream = getContentResolver().openInputStream(uri);
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];
        int len;
        while ((len = iStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }
        return byteBuffer.toByteArray();
    }
}
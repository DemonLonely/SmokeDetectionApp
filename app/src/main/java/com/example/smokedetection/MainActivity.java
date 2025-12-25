package com.example.smokedetection;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    private static final String SUPABASE_URL = "https://gklovokybxmonmnuoflk.supabase.co";
    private static final String SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImdrbG92b2t5Ynhtb25tbnVvZmxrIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjUzNTY0NTcsImV4cCI6MjA4MDkzMjQ1N30.rgEFaf0M0WBKNbaintilKHILM3HS4Dnpb40wSbj_hZA";

    private static final String SERVER_URL = "http://10.0.2.2:8000";

    private SupabaseClient supabase;
    private boolean isGuest = true;

    private Button btnUploadImage, btnUploadVideo, btnLogout;

    private View loadingOverlay;

    private long currentProcessID = 0;

    private Call currentCall; // To control the network request

    // Gallery Launcher
    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri selectedUri = result.getData().getData();
                    if (selectedUri != null) {
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

        supabase = new SupabaseClient(SUPABASE_URL, SUPABASE_KEY);
        checkLoginStatus();

        btnUploadImage = findViewById(R.id.btnUploadImage);
        btnUploadVideo = findViewById(R.id.btnUploadVideo);
        btnLogout = findViewById(R.id.btnLogout);
        loadingOverlay = findViewById(R.id.loadingOverlay);
        Button btnStopProcessing = findViewById(R.id.btnStopProcessing);

        btnUploadImage.setOnClickListener(v -> openGallery("image/*"));
        btnUploadVideo.setOnClickListener(v -> openGallery("video/*"));

        btnLogout.setOnClickListener(v -> logout());

        btnStopProcessing.setOnClickListener(v -> {
            currentProcessID = 0;

            if(currentCall != null){
                currentCall.cancel();
            }

            loadingOverlay.setVisibility(View.GONE); // This reveals the Main UI again
            Toast.makeText(MainActivity.this, "Stop Processing", Toast.LENGTH_SHORT).show();
        });
    }

    private void checkLoginStatus() {
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        String token = prefs.getString("auth_token", null);

        if (token != null) {
            isGuest = false;
            supabase.setAuthToken(token);
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

    // Send to server
    private void processFile(Uri uri, boolean isVideo) {
        Toast.makeText(this, "Uploading to Detection Server...", Toast.LENGTH_SHORT).show();
        loadingOverlay.setVisibility(View.VISIBLE);

        // Generate unique ProcessID
        long myProcessID = System.currentTimeMillis();
        currentProcessID = myProcessID;

        // Determine Bucket
        String bucketName;
        if (isGuest) {
            bucketName = isVideo ? "guest-videos" : "guest-images";
        } else {
            bucketName = isVideo ? "user-videos" : "user-images";
        }

        // Read File Bytes
        byte[] fileBytes = getBytesFromUri(uri);
        if (fileBytes == null) {
            Toast.makeText(this, "Error reading file", Toast.LENGTH_SHORT).show();
            loadingOverlay.setVisibility(View.GONE);
            return;
        }

        // Prepare Metadata
        String mediaType = isVideo ? "video" : "image";
        String filename = "upload_" + System.currentTimeMillis() + (isVideo ? ".mp4" : ".jpg");

        // Send to Python Server (via SupabaseClient)
        currentCall = supabase.sendToDetectionServer(SERVER_URL, mediaType, fileBytes, filename, bucketName, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    // Click stop -> ignore error
                    if (currentProcessID != myProcessID) return;

                    loadingOverlay.setVisibility(View.GONE);
                    Toast.makeText(MainActivity.this, "Server Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String responseBody = response.body().string();
                if (response.isSuccessful()) {
                    try {
                        // Python Server returns: { "status": "success", "processed_url": "..." }
                        JSONObject json = new JSONObject(responseBody);
                        String processedUrl = json.getString("processed_url");

                        runOnUiThread(() -> {
                            if(currentProcessID != myProcessID) return;
                            handleUploadSuccess(processedUrl, bucketName);
                        });

                    } catch (Exception e) {
                        runOnUiThread(() -> {
                            if (currentProcessID != myProcessID) return;
                            loadingOverlay.setVisibility(View.GONE);
                            Toast.makeText(MainActivity.this, "JSON Error", Toast.LENGTH_SHORT).show();
                        });
                    }
                } else {
                    runOnUiThread(() -> {
                        if (currentProcessID != myProcessID) return;
                        loadingOverlay.setVisibility(View.GONE);
                        Toast.makeText(MainActivity.this, "Server Failed: " + response.code(), Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    private void handleUploadSuccess(String url, String bucket) {
        Log.d("UPLOAD", "Processed URL: " + url);

        // Save to Database
        String fileName = url.substring(url.lastIndexOf("/") + 1);
        saveMetadataToDatabase(bucket, fileName);

        runOnUiThread(() -> {
            // Hide the Loading Screen
            loadingOverlay.setVisibility(View.GONE);

            // Open the Result Screen
            Intent intent = new Intent(MainActivity.this, ResultActivity.class);
            intent.putExtra("url", url);
            intent.putExtra("is_video", bucket.contains("video"));
            startActivity(intent);
        });
    }

    private void saveMetadataToDatabase(String bucket, String filePath) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("bucket_name", bucket);
            jsonObject.put("file_path", filePath);
        } catch (Exception e) {
            e.printStackTrace();
        }

        supabase.insert("media_uploads", jsonObject.toString(), new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // Log error but doesn't annoy user if image logic works
                Log.e("DB", "Database error: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) {
                if (response.isSuccessful()) {
                    Log.d("DB", "Metadata saved");
                }
            }
        });
    }

    private void logout() {
        getSharedPreferences("AppPrefs", MODE_PRIVATE).edit().clear().apply();
        startActivity(new Intent(MainActivity.this, LoginActivity.class));
        finish();
    }

    // Helper: Read Bytes from URI
    private byte[] getBytesFromUri(Uri uri) {
        try (InputStream inputStream = getContentResolver().openInputStream(uri);
             ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream()) {

            if (inputStream == null) return null;

            int bufferSize = 1024;
            byte[] buffer = new byte[bufferSize];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                byteBuffer.write(buffer, 0, len);
            }
            return byteBuffer.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
package com.example.smokedetection;

import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.smokedetection.databinding.ActivityDetectImageBinding;

public class ImageDetectActivity extends AppCompatActivity {

    private ActivityDetectImageBinding b;
    private Uri selectedImageUri;

    private ActivityResultLauncher<String> pickImageLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityDetectImageBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        selectedImageUri = uri;
                        b.imgPreview.setImageURI(uri);
                        b.tvStatus.setText("Selected: " + uri);
                    }
                }
        );

        b.btnPickImage.setOnClickListener(v -> pickImageLauncher.launch("image/*"));

        b.btnDetectImage.setOnClickListener(v -> {
            if (selectedImageUri == null) {
                Toast.makeText(this, "Pick an image first!", Toast.LENGTH_SHORT).show();
                return;
            }

            b.tvStatus.setText("Running detection...");

            // TODO: GẮN DETECT THẬT Ở ĐÂY
            // Ví dụ:
            // - Gọi hàm detect cũ của bạn
            // - Upload lên server / chạy model local
            // - Rồi chuyển ResultActivity
            //
            // startActivity(new Intent(this, ResultActivity.class)
            //         .putExtra("type", "image")
            //         .putExtra("uri", selectedImageUri.toString()));

            b.tvStatus.setText("Done (UI flow OK). Now plug your YOLO detection here.");
        });
    }
}

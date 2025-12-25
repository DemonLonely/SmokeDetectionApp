package com.example.smokedetection;

import android.os.Handler;
import android.os.Looper;
import org.json.JSONObject;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ImageUploader {
    private static final String SERVER_URL = "http://192.168.1.6:8000/process_image";

    private final OkHttpClient client;

    public interface UploadCallback {
        void onSuccess(String imageUrl, String bucketName);
        void onError(String error);
    }

    public ImageUploader(){
        client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS) // Images are faster than video
                .build();
    }

    public void uploadImage(File imageFile, String bucketName, UploadCallback callback) {
        // Request body for image
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", imageFile.getName(),
                        RequestBody.create(imageFile, MediaType.parse("image/jpeg")))
                .addFormDataPart("bucket_name", bucketName)
                .build();

        Request request = new Request.Builder()
                .url(SERVER_URL)
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                new Handler(Looper.getMainLooper()).post(() ->
                        callback.onError("Server Connection Failed: " + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String responseData = response.body().string();
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (response.isSuccessful()) {
                        try {
                            JSONObject json = new JSONObject(responseData);
                            if (json.has("processed_url")) {
                                String url = json.getString("processed_url");
                                String usedBucket = json.optString("bucket", bucketName);
                                callback.onSuccess(url, usedBucket);
                            } else {
                                callback.onError("Server Error: " + responseData);
                            }
                        } catch (Exception e) {
                            callback.onError("JSON Error: " + e.getMessage());
                        }
                    } else {
                        callback.onError("Server Error Code: " + response.code());
                    }
                });
            }
        });
    }
}
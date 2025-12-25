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

public class VideoUploader {
    private static final String SERVER_URL = "http://10.0.2.2:8000/process_video";

    private final OkHttpClient client;

    public interface UploadCallback {
        // Pass back the bucket name -> MainActivity knows where it goes
        void onSuccess(String videoURL, String bucketName);
        void onError(String error);
    }

    public VideoUploader(){
        client = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();
    }

    public void uploadVideo(File videoFile, String bucketName, UploadCallback callback) {
        // Request body for video
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", videoFile.getName(),
                        RequestBody.create(videoFile, MediaType.parse("video/mp4")))
                // Tell the server which bucket to use (User vs Guest)
                .addFormDataPart("bucket_name", bucketName)
                .build();

        // HTTP Request
        Request request = new Request.Builder()
                .url(SERVER_URL)
                .post(requestBody)
                .build();

        // Send to server
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
                                // Extract URL and the Bucket actually used
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
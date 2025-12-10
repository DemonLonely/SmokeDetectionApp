package com.example.smokedetection;

import okhttp3.*;
import org.json.JSONObject;
import java.io.IOException;

public class SupabaseClient {
    private final String supabaseUrl;
    private final String supabaseKey;
    private final OkHttpClient client;
    private String userToken; // Stores the logged-in user's token

    public SupabaseClient(String url, String key) {
        this.supabaseUrl = url;
        this.supabaseKey = key;
        this.client = new OkHttpClient();
        this.userToken = key; // Default to guest mode
    }

    // Call this after successful login
    public void setAuthToken(String token) {
        this.userToken = token;
    }

    // Register
    public void signUp(String email, String password, Callback callback) {
        String fullUrl = supabaseUrl + "/auth/v1/signup";
        String json = "{\"email\": \"" + email + "\", \"password\": \"" + password + "\"}";
        postRequest(fullUrl, json, callback);
    }

    // Login
    public void signIn(String email, String password, Callback callback) {
        String fullUrl = supabaseUrl + "/auth/v1/token?grant_type=password";
        String json = "{\"email\": \"" + email + "\", \"password\": \"" + password + "\"}";
        postRequest(fullUrl, json, callback);
    }

    // Insert data into database
    public void insert(String tableName, String jsonBody, Callback callback) {
        String fullUrl = supabaseUrl + "/rest/v1/" + tableName;
        // Use userToken
        Request request = new Request.Builder()
                .url(fullUrl)
                .addHeader("apikey", supabaseKey)
                .addHeader("Authorization", "Bearer " + userToken)
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=minimal")
                .post(RequestBody.create(jsonBody, MediaType.get("application/json")))
                .build();
        client.newCall(request).enqueue(callback);
    }

    // Send requests
    private void postRequest(String url, String json, Callback callback) {
        RequestBody body = RequestBody.create(json, MediaType.get("application/json"));
        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", supabaseKey)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();
        client.newCall(request).enqueue(callback);
    }

    // Upload file
    public void uploadFile(String bucket, String fileName, byte[] fileBytes, Callback callback) {
        // storage/v1/object/bucket/filename
        String fullUrl = supabaseUrl + "/storage/v1/object/" + bucket + "/" + fileName;

        // Send the raw file bytes
        RequestBody body = RequestBody.create(fileBytes, MediaType.parse("application/octet-stream"));

        Request request = new Request.Builder()
                .url(fullUrl)
                .addHeader("apikey", supabaseKey)
                .addHeader("Authorization", "Bearer " + userToken) // Uses Guest Key OR User Token
                .addHeader("Content-Type", "application/octet-stream")
                .post(body)
                .build();

        client.newCall(request).enqueue(callback);
    }
}
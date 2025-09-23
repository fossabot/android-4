package uk.trigpointing.android.api;

import android.util.Log;
import com.google.gson.Gson;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * API client for handling authentication with the new TrigpointingUK API
 */
public class AuthApiClient {
    private static final String TAG = "AuthApiClient";
    private static final String API_BASE_URL = "https://api.trigpointing.uk/v1";
    private static final String LOGIN_ENDPOINT = "/legacy/login";

    private final OkHttpClient httpClient;
    private final Gson gson;

    public AuthApiClient() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .build();
        this.gson = new Gson();
    }

    /**
     * Interface for authentication callbacks
     */
    public interface AuthCallback {
        void onSuccess(AuthResponse authResponse);
        void onError(String errorMessage);
    }

    /**
     * Authenticate user with username and password
     * @param username The username or email
     * @param password The user's password
     * @param callback Callback to handle success or error
     */
    public void authenticate(String username, String password, AuthCallback callback) {
        Log.i(TAG, "authenticate: Starting authentication for user: " + username);
        
        CompletableFuture.supplyAsync(() -> {
            try {
                // Create form data for the POST request
                FormBody formBody = new FormBody.Builder()
                        .add("username", username)
                        .add("password", password)
                        .build();

                // Build the request
                Request request = new Request.Builder()
                        .url(API_BASE_URL + LOGIN_ENDPOINT)
                        .post(formBody)
                        .addHeader("Content-Type", "application/x-www-form-urlencoded")
                        .build();

                Log.i(TAG, "authenticate: Sending POST request to " + API_BASE_URL + LOGIN_ENDPOINT);

                // Execute the request with timeout handling
                try (Response response = httpClient.newCall(request).execute()) {
                    String responseBody = response.body() != null ? response.body().string() : "";
                    Log.i(TAG, "authenticate: Received response with code: " + response.code());
                    
                    if (response.isSuccessful()) {
                        // Parse successful authentication response
                        Log.i(TAG, "authenticate: Authentication successful, parsing response");
                        try {
                            AuthResponse authResponse = gson.fromJson(responseBody, AuthResponse.class);
                            if (authResponse == null || authResponse.getUser() == null) {
                                Log.e(TAG, "authenticate: Invalid response structure");
                                return new AuthResult(false, null, "Invalid response from server");
                            }
                            return new AuthResult(true, authResponse, null);
                        } catch (Exception e) {
                            Log.e(TAG, "authenticate: Failed to parse successful response", e);
                            return new AuthResult(false, null, "Failed to parse server response");
                        }
                    } else {
                        // Parse error response
                        Log.w(TAG, "authenticate: Authentication failed with code: " + response.code());
                        try {
                            if (!responseBody.isEmpty()) {
                                ErrorResponse errorResponse = gson.fromJson(responseBody, ErrorResponse.class);
                                return new AuthResult(false, null, errorResponse.getDetail());
                            } else {
                                return new AuthResult(false, null, "Authentication failed: HTTP " + response.code());
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "authenticate: Failed to parse error response", e);
                            return new AuthResult(false, null, "Authentication failed: HTTP " + response.code());
                        }
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "authenticate: Network error during authentication", e);
                return new AuthResult(false, null, "Network error: " + e.getMessage());
            } catch (Exception e) {
                Log.e(TAG, "authenticate: Unexpected error during authentication", e);
                return new AuthResult(false, null, "Unexpected error: " + e.getMessage());
            }
        }).thenAccept(result -> {
            // Call the callback on the result
            if (result.isSuccess()) {
                Log.i(TAG, "authenticate: Calling success callback");
                callback.onSuccess(result.getAuthResponse());
            } else {
                Log.i(TAG, "authenticate: Calling error callback with message: " + result.getErrorMessage());
                callback.onError(result.getErrorMessage());
            }
        }).exceptionally(throwable -> {
            Log.e(TAG, "authenticate: Exception in async operation", throwable);
            callback.onError("Authentication failed: " + throwable.getMessage());
            return null;
        });
    }

    /**
     * Refresh authentication token using stored credentials
     * @param username The username or email
     * @param password The user's password
     * @param callback Callback to handle success or error
     */
    public void refreshToken(String username, String password, AuthCallback callback) {
        Log.i(TAG, "refreshToken: Refreshing token for user: " + username);
        
        // Use the same authentication method as initial login
        authenticate(username, password, callback);
    }

    /**
     * Helper class to hold authentication results
     */
    private static class AuthResult {
        private final boolean success;
        private final AuthResponse authResponse;
        private final String errorMessage;

        public AuthResult(boolean success, AuthResponse authResponse, String errorMessage) {
            this.success = success;
            this.authResponse = authResponse;
            this.errorMessage = errorMessage;
        }

        public boolean isSuccess() {
            return success;
        }

        public AuthResponse getAuthResponse() {
            return authResponse;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }
}

package uk.trigpointing.android.api;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;
import com.google.gson.Gson;

/**
 * Helper class to manage storage of authentication data from the new API
 */
public class AuthPreferences {
    private static final String PREF_ACCESS_TOKEN = "api_access_token";
    private static final String PREF_TOKEN_TYPE = "api_token_type";
    private static final String PREF_USER_DATA = "api_user_data";
    private static final String PREF_USER_ID = "api_user_id";
    private static final String PREF_EXPIRES_IN = "api_expires_in";
    private static final String PREF_LOGIN_TIMESTAMP = "api_login_timestamp";

    private final SharedPreferences preferences;
    private final Gson gson;

    public AuthPreferences(Context context) {
        this.preferences = PreferenceManager.getDefaultSharedPreferences(context);
        this.gson = new Gson();
    }

    /**
     * Store authentication data from successful login
     */
    public void storeAuthData(AuthResponse authResponse) {
        SharedPreferences.Editor editor = preferences.edit();
        
        editor.putString(PREF_ACCESS_TOKEN, authResponse.getAccessToken());
        editor.putString(PREF_TOKEN_TYPE, authResponse.getTokenType());
        editor.putInt(PREF_EXPIRES_IN, authResponse.getExpiresIn());
        editor.putLong(PREF_LOGIN_TIMESTAMP, System.currentTimeMillis());
        
        // Store user data as JSON
        String userJson = gson.toJson(authResponse.getUser());
        editor.putString(PREF_USER_DATA, userJson);
        // Store user id explicitly for durability and easy access
        if (authResponse.getUser() != null) {
            editor.putInt(PREF_USER_ID, authResponse.getUser().getId());
        }
        
        editor.apply();
    }

    /**
     * Clear all stored authentication data
     */
    public void clearAuthData() {
        SharedPreferences.Editor editor = preferences.edit();
        
        editor.remove(PREF_ACCESS_TOKEN);
        editor.remove(PREF_TOKEN_TYPE);
        editor.remove(PREF_USER_DATA);
        editor.remove(PREF_USER_ID);
        editor.remove(PREF_EXPIRES_IN);
        editor.remove(PREF_LOGIN_TIMESTAMP);
        
        editor.apply();
    }

    /**
     * Get stored access token
     */
    public String getAccessToken() {
        return preferences.getString(PREF_ACCESS_TOKEN, null);
    }

    /**
     * Get stored token type
     */
    public String getTokenType() {
        return preferences.getString(PREF_TOKEN_TYPE, null);
    }

    /**
     * Get stored user data
     */
    public User getUser() {
        String userJson = preferences.getString(PREF_USER_DATA, null);
        if (userJson != null) {
            try {
                return gson.fromJson(userJson, User.class);
            } catch (Exception e) {
                // If parsing fails, return null
                return null;
            }
        }
        return null;
    }

    /**
     * Get stored user id, even if token has expired.
     */
    public int getUserId() {
        // Prefer id from parsed user JSON
        User user = getUser();
        if (user != null && user.getId() > 0) {
            return user.getId();
        }
        // Fallback to explicitly stored id
        return preferences.getInt(PREF_USER_ID, 0);
    }

    /**
     * Get expires in value (seconds)
     */
    public int getExpiresIn() {
        return preferences.getInt(PREF_EXPIRES_IN, 0);
    }

    /**
     * Get the timestamp when the user logged in
     */
    public long getLoginTimestamp() {
        return preferences.getLong(PREF_LOGIN_TIMESTAMP, 0);
    }

    /**
     * Check if user is currently logged in with valid token
     */
    public boolean isLoggedIn() {
        String token = getAccessToken();
        if (token == null || token.isEmpty()) {
            return false;
        }

        // Check if token has expired
        long loginTime = getLoginTimestamp();
        int expiresIn = getExpiresIn();
        
        if (loginTime > 0 && expiresIn > 0) {
            long currentTime = System.currentTimeMillis();
            long expirationTime = loginTime + (expiresIn * 1000L); // Convert seconds to milliseconds
            
            return currentTime < expirationTime;
        }

        // If we don't have expiration info, assume logged in if we have a token
        return true;
    }

    /**
     * Get display name for the user (user.name from API, or username if not available)
     */
    public String getDisplayName() {
        User user = getUser();
        if (user != null && user.getName() != null && !user.getName().isEmpty()) {
            return user.getName();
        }

        // Fallback to legacy username if API user data is not available
        return preferences.getString("username", "");
    }

    /**
     * Get display name with user ID for developer mode
     */
    public String getDisplayNameWithId() {
        User user = getUser();
        if (user != null && user.getName() != null && !user.getName().isEmpty()) {
            return user.getName() + " (" + user.getId() + ")";
        }

        // Fallback to legacy username if API user data is not available
        return preferences.getString("username", "");
    }

    /**
     * Check if the token should be refreshed (expires within 5 minutes)
     * In developer mode, always returns true to allow testing of token refresh
     */
    public boolean shouldRefreshToken() {
        String token = getAccessToken();
        if (token == null || token.isEmpty()) {
            return false;
        }

        // Check if developer mode is enabled
        boolean devMode = preferences.getBoolean("dev_mode", false);
        if (devMode) {
            return true; // Always refresh in developer mode for testing
        }

        long loginTime = getLoginTimestamp();
        int expiresIn = getExpiresIn();
        
        if (loginTime > 0 && expiresIn > 0) {
            long currentTime = System.currentTimeMillis();
            long expirationTime = loginTime + (expiresIn * 1000L); // Convert seconds to milliseconds
            long fiveMinutesFromNow = currentTime + (5 * 60 * 1000L); // 5 minutes in milliseconds
            
            // Refresh if token expires within 5 minutes
            return fiveMinutesFromNow >= expirationTime;
        }

        // If we don't have expiration info, don't refresh
        return false;
    }
}

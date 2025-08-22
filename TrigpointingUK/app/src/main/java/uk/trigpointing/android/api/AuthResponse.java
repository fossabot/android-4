package uk.trigpointing.android.api;

/**
 * Authentication response model for the new API
 */
public class AuthResponse {
    private String access_token;
    private String token_type;
    private User user;
    private int expires_in;

    // Default constructor required for deserialization
    public AuthResponse() {}

    // Getters and setters
    public String getAccessToken() {
        return access_token;
    }

    public void setAccessToken(String access_token) {
        this.access_token = access_token;
    }

    public String getTokenType() {
        return token_type;
    }

    public void setTokenType(String token_type) {
        this.token_type = token_type;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public int getExpiresIn() {
        return expires_in;
    }

    public void setExpiresIn(int expires_in) {
        this.expires_in = expires_in;
    }

    @Override
    public String toString() {
        return "AuthResponse{" +
                "access_token='" + access_token + '\'' +
                ", token_type='" + token_type + '\'' +
                ", user=" + user +
                ", expires_in=" + expires_in +
                '}';
    }
}

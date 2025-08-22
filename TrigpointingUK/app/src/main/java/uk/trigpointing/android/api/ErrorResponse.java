package uk.trigpointing.android.api;

/**
 * Error response model for API authentication errors
 */
public class ErrorResponse {
    private String detail;

    // Default constructor required for deserialization
    public ErrorResponse() {}

    public ErrorResponse(String detail) {
        this.detail = detail;
    }

    // Getters and setters
    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    @Override
    public String toString() {
        return "ErrorResponse{" +
                "detail='" + detail + '\'' +
                '}';
    }
}

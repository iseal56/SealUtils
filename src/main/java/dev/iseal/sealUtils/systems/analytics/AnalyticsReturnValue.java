package dev.iseal.sealUtils.systems.analytics;

public enum AnalyticsReturnValue {

    NOT_ENABLED("Analytics is not enabled", -1),
    EVENT_SENT("Analytics event sent successfully", 200),
    RATE_LIMIT_EXCEEDED("Rate limit exceeded, event not sent", 429),
    NOT_FOUND("Analytics provider not found", 404),
    MALFORMED_DATA("Data is malformed", 400),
    SERVER_ERROR("An error occurred on the server while processing the analytics event", 500),
    UNKNOWN_ERROR("An unknown error occurred while sending analytics event", 520),
    UNAUTHENTICATED("User is not authenticated, cannot send analytics event", 401), // probably will never be used, but just in case
    UNAUTHORIZED("User is not authorized to send this analytics event", 403), // probably will never be used, but just in case
    CUSTOM_MESSAGE("Custom message", 1000); // Custom message for specific use cases

    private String message;
    private int statusCode;

    AnalyticsReturnValue(String message, int statusCode) {
        this.message = message;
        this.statusCode = statusCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        if (this != CUSTOM_MESSAGE) {
            throw new UnsupportedOperationException("Cannot change message for predefined AnalyticsReturnValue");
        }
        this.message = message;
    }

    public void setStatusCode(int statusCode) {
        if (this != CUSTOM_MESSAGE) {
            throw new UnsupportedOperationException("Cannot change status code for predefined AnalyticsReturnValue");
        }
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public static AnalyticsReturnValue fromStatusCode(int statusCode) {
        for (AnalyticsReturnValue value : values()) {
            if (value.getStatusCode() == statusCode) {
                return value;
            }
        }
        return CUSTOM_MESSAGE; // Default to CUSTOM_MESSAGE if no match found
    }

}

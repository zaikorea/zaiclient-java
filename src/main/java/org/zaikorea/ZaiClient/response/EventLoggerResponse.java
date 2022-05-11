package org.zaikorea.ZaiClient.response;

import com.google.gson.annotations.SerializedName;

public class EventLoggerResponse {

    @SerializedName("message")
    private String message;

    @SerializedName("failed_count")
    private int failureCount;

    @SerializedName("timestamp")
    private double timestamp;

    public String getMessage() {
        return this.message;
    }

    public Integer getFailureCount() { return this.failureCount; }

    public double getTimestamp() { return this.timestamp; }

    @Override
    public String toString() {
        return "EventLoggerResponse{" +
                "message='" + message + "', " +
                "failure count=" + failureCount + ", " +
                "timestamp=" + timestamp +
                "}";
    }
}

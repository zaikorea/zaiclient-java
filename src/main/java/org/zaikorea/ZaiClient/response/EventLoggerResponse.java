package org.zaikorea.ZaiClient.response;

import com.google.gson.annotations.SerializedName;

public class EventLoggerResponse {

    @SerializedName("message")
    private String message;

    public String getMessage() {
        return this.message;
    }

    @Override
    public String toString() {
        return "EventLoggerResponse{" +
            "message='" + message + '\'' +
            '}';
    }
}

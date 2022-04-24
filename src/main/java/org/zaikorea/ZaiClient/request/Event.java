package org.zaikorea.ZaiClient.request;

import com.google.gson.annotations.SerializedName;

public class Event {

    @SerializedName("user_id")
    protected String userId;

    @SerializedName("item_id")
    protected String itemId;

    @SerializedName("timestamp")
    protected double timestamp;

    @SerializedName("event_type")
    protected String eventType;

    @SerializedName("event_value")
    protected String eventValue;

    public String getUserId() {
        return userId;
    }

    public String getItemId() {
        return itemId;
    }

    public double getTimestamp() {
        return timestamp;
    }

    public String getEventType() {
        return eventType;
    }

    public String getEventValue() {
        return eventValue;
    }
}

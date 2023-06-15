package org.zaikorea.zaiclient.request;

import com.google.gson.annotations.SerializedName;

import java.security.InvalidParameterException;


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

    @SerializedName("time_to_live")
    protected Integer timeToLive = null;

    @SerializedName("is_zai_recommendation")
    protected Boolean isZaiRecommendation = false;

    @SerializedName("from")
    protected String from = null;

    public static double getCurrentUnixTimestamp() {
        // Have to track nanosecond because client sometimes calls api multiple times in a millisecond
        // Use nanoTime because jdk 1.8 doesn't support Instant.getNano() function.
        String time = Long.toString(System.nanoTime());
        time = time.substring(time.length()-7);

        long longTime = Long.parseLong(time);
        long currentTime = System.currentTimeMillis();

        return currentTime / 1000.d + longTime / 1e10;
    }

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

    public Integer getTimeToLive() {
        return timeToLive;
    }

    public boolean getIsZaiRecommendation() {
        return isZaiRecommendation;
    }

    public String getFrom() {
        return from;
    }

    public void setUserId(String userId) {
        if (!(userId.length() > 0 && userId.length() <= 500))
            throw new InvalidParameterException("Length of user id must be between 1 and 500.");

        this.userId = userId;
    }

    public void setItemId(String itemId) {
        if (!(itemId.length() > 0 && itemId.length() <= 500))
            throw new InvalidParameterException("Length of item id must be between 1 and 500.");

        this.itemId = itemId;
    }

    protected void setTimeStamp(double timestamp) {
        if (!(timestamp >= 1_648_871_097 || timestamp <= 2_147_483_647))
            throw new InvalidParameterException("Invalid timestamp.");

        this.timestamp = timestamp;
    }

    public void setEventType(String eventType) {
        if (!(eventType.length() > 0 && eventType.length() <= 500))
            throw new InvalidParameterException("Length of event type must be between 1 and 500.");

        this.eventType = eventType;
    }

    public void setEventValue(String eventValue) {
        if (eventValue.length() == 0)
            throw new InvalidParameterException("Length of event value must be at least 1.");

        if (eventValue.length() > 500)
            this.eventValue = eventValue.substring(0, 500);
        else
            this.eventValue = eventValue;
    }

    public void setTimeToLive(int timeToLive) {
        if (timeToLive < 0)
            throw new InvalidParameterException("Time value can not be negative.");
        
        this.timeToLive = timeToLive;
    }
}

package org.zaikorea.zaiclient.request.events;

import org.zaikorea.zaiclient.utils.Validator;

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

    @SerializedName("time_to_live")
    protected Integer timeToLive = null;

    @SerializedName("is_zai_recommendation")
    protected Boolean isZaiRecommendation = false;

    @SerializedName("from")
    protected String from = null;

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

    public Event setUserId(String userId) {
        this.userId = Validator.validateString(userId, 1, 500, false, "userId");

        return this;
    }

    public Event setItemId(String itemId) {
        this.itemId = Validator.validateString(itemId, 1, 500, false, "itemId");

        return this;
    }

    public Event setTimestamp(int timestamp) {
        this.timestamp = Validator.validateNumber(timestamp, 1_648_871_097, 2_147_483_647, false, "timestamp");

        return this;
    }

    public Event setTimestamp(long timestamp) {
        this.timestamp = Validator.validateNumber(timestamp, (long) 1_648_871_097, (long) 2_147_483_647, false, "timestamp");

        return this;
    }

    public Event setTimestamp(double timestamp) {
        this.timestamp = Validator.validateNumber(timestamp, 1_648_871_097., 2_147_483_647., false, "timestamp");

        return this;
    }

    public Event setEventType(String eventType) {
        this.eventType = Validator.validateString(eventType, 1, 500, false, "eventType");

        return this;
    }

    public Event setEventValue(String eventValue) {
        this.eventValue = Validator.validateString(eventValue, 0, 500, true, "eventValue");

        return this;
    }

    public Event setIsZaiRecommendation(boolean isZaiRecommendation) {
        this.isZaiRecommendation = isZaiRecommendation;

        return this;
    }

    public Event setFrom(String from) {
        this.from = Validator.validateString(from, 0, 500, true, "from");

        return this;
    }

    public Event setTimeToLive(Integer timeToLive) {
        this.timeToLive = Validator.validateNumber(timeToLive, 0, 2_147_483_647, true, "timeToLive");

        return this;
    }
}

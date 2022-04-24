package org.zaikorea.ZaiClient.request;

public class RateEvent extends Event{

    public RateEvent(String userId, String itemId, float rating) {
        this.userId = userId;
        this.itemId = itemId;
        this.timestamp = System.currentTimeMillis() / 1000.d;
        this.eventValue = Float.toString(rating);
        this.setEventType();
    }

    public RateEvent(String userId, String itemId, float rating, int timestamp) {
        this.userId = userId;
        this.itemId = itemId;
        this.timestamp = timestamp;
        this.eventValue = Float.toString(rating);
        this.setEventType();
    }

    public RateEvent(String userId, String itemId, float rating, long timestamp) {
        this.userId = userId;
        this.itemId = itemId;
        this.timestamp = timestamp;
        this.eventValue = Float.toString(rating);
        this.setEventType();
    }

    public RateEvent(String userId, String itemId, float rating, float timestamp) {
        this.userId = userId;
        this.itemId = itemId;
        this.timestamp = timestamp;
        this.eventValue = Float.toString(rating);
        this.setEventType();
    }

    public RateEvent(String userId, String itemId, float rating, double timestamp) {
        this.userId = userId;
        this.itemId = itemId;
        this.timestamp = timestamp;
        this.eventValue = Float.toString(rating);
        this.setEventType();
    }

    private void setEventType() {
        this.eventType = "rate";
    }
}

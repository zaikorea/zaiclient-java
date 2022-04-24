package org.zaikorea.ZaiClient.request;

public class LikeEvent extends Event {

    public LikeEvent(String userId, String itemId) {
        this.userId = userId;
        this.itemId = itemId;
        this.timestamp = System.currentTimeMillis() / 1000.d;
        this.setEventTypeAndValue();
    }

    public LikeEvent(String userId, String itemId, int timestamp) {
        this.userId = userId;
        this.itemId = itemId;
        this.timestamp = timestamp;
        this.setEventTypeAndValue();
    }

    public LikeEvent(String userId, String itemId, long timestamp) {
        this.userId = userId;
        this.itemId = itemId;
        this.timestamp = timestamp;
        this.setEventTypeAndValue();
    }

    public LikeEvent(String userId, String itemId, float timestamp) {
        this.userId = userId;
        this.itemId = itemId;
        this.timestamp = timestamp;
        this.setEventTypeAndValue();
    }

    public LikeEvent(String userId, String itemId, double timestamp) {
        this.userId = userId;
        this.itemId = itemId;
        this.timestamp = timestamp;
        this.setEventTypeAndValue();
    }

    private void setEventTypeAndValue() {
        this.eventType = "like";
        this.eventValue = "1";
    }
}

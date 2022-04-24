package org.zaikorea.ZaiClient.request;

public class RateEvent extends Event{

    private static final String defaultEventType = "rate";

    public RateEvent(String userId, String itemId, double rating) {
        this(userId, itemId, rating, Event.getCurrentUnixTimestamp());
    }

    public RateEvent(String userId, String itemId, double rating, double timestamp) {
        this.userId = userId;
        this.itemId = itemId;
        this.timestamp = (float)timestamp;
        this.eventType = defaultEventType;
        this.eventValue = Double.toString(rating);
    }
}

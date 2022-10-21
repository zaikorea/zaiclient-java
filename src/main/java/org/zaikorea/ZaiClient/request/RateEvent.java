package org.zaikorea.ZaiClient.request;

public class RateEvent extends Event{

    private static final String defaultEventType = "rate";

    public RateEvent(String userId, String itemId, double rating) {
        this(userId, itemId, rating, Event.getCurrentUnixTimestamp());
    }

    public RateEvent(String userId, String itemId, double rating, double timestamp) {
        this.setUserId(userId);
        this.setItemId(itemId);
        this.setTimestamp(timestamp);
        this.setEventType(defaultEventType);
        this.setEventValue(Double.toString(rating));
    }
}

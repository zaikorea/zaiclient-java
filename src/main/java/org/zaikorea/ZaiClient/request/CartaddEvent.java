package org.zaikorea.ZaiClient.request;

public class CartaddEvent extends Event {

    private static final String defaultEventType = "cartadd";
    private static final String defaultEventValue = "1";

    public CartaddEvent(String userId, String itemId) {
        this(userId, itemId, Event.getCurrentUnixTimestamp());
    }

    public CartaddEvent(String userId, String itemId, double timestamp) {
        this.userId = userId;
        this.itemId = itemId;
        this.timestamp = timestamp;
        this.eventType = defaultEventType;
        this.eventValue = defaultEventValue;
    }

}

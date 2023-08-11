package org.zaikorea.zaiclient.request;

public class CartaddEvent extends Event {

    private static final String defaultEventType = "cartadd";
    private static final String defaultEventValue = "null";

    public CartaddEvent(String userId, String itemId) {
        this(userId, itemId, Event.getCurrentUnixTimestamp());
    }

    public CartaddEvent(String userId, String itemId, double timestamp) {
        this.setUserId(userId);
        this.setItemId(itemId);
        this.setTimestamp(timestamp);
        this.setEventType(defaultEventType);
        this.setEventValue(defaultEventValue);
    }
}

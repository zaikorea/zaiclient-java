package org.zaikorea.ZaiClient.request;

public class PurchaseEvent extends Event {

    private static final String defaultEventType = "purchase";

    public PurchaseEvent(String userId, String itemId, int price) {
        this(userId, itemId, price, Event.getCurrentUnixTimestamp());
    }

    public PurchaseEvent(String userId, String itemId, int price, double timestamp) {
        this.userId = userId;
        this.itemId = itemId;
        this.timestamp = timestamp;
        this.eventType = defaultEventType;
        this.eventValue = Integer.toString(price);
    }
}

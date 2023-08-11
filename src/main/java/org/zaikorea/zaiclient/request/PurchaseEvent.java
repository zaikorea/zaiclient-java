package org.zaikorea.zaiclient.request;

public class PurchaseEvent extends Event {

    private static final String defaultEventType = "purchase";

    public PurchaseEvent(String userId, String itemId, int price) {
        this(userId, itemId, price, Event.getCurrentUnixTimestamp());
    }

    public PurchaseEvent(String userId, String itemId, int price, double timestamp) {
        this.setUserId(userId);
        this.setItemId(itemId);
        this.setTimestamp(timestamp);
        this.setEventType(defaultEventType);
        this.setEventValue(Integer.toString(price));
    }
}

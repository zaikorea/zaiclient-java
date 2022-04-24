package org.zaikorea.ZaiClient.request;

public class PurchaseEvent extends Event {

    public PurchaseEvent(String userId, String itemId, int price) {
        this.userId = userId;
        this.itemId = itemId;
        this.timestamp = System.currentTimeMillis() / 1000.d;
        this.eventValue = Integer.toString(price);
        this.setEventType();
    }

    public PurchaseEvent(String userId, String itemId, int price, int timestamp) {
        this.userId = userId;
        this.itemId = itemId;
        this.timestamp = timestamp;
        this.eventValue = Integer.toString(price);
        this.setEventType();
    }

    public PurchaseEvent(String userId, String itemId, int price, long timestamp) {
        this.userId = userId;
        this.itemId = itemId;
        this.timestamp = timestamp;
        this.eventValue = Integer.toString(price);
        this.setEventType();
    }

    public PurchaseEvent(String userId, String itemId, int price, float timestamp) {
        this.userId = userId;
        this.itemId = itemId;
        this.timestamp = timestamp;
        this.eventValue = Integer.toString(price);
        this.setEventType();
    }

    public PurchaseEvent(String userId, String itemId, int price, double timestamp) {
        this.userId = userId;
        this.itemId = itemId;
        this.timestamp = timestamp;
        this.eventValue = Integer.toString(price);
        this.setEventType();
    }

    public PurchaseEvent(String userId, String itemId) {
        this.userId = userId;
        this.itemId = itemId;
        this.timestamp = System.currentTimeMillis() / 1000.d;
        this.eventValue = "null";
        this.setEventType();
    }

    public PurchaseEvent(String userId, String itemId, long timestamp) {
        this.userId = userId;
        this.itemId = itemId;
        this.timestamp = timestamp;
        this.eventValue = "null";
        this.setEventType();
    }

    public PurchaseEvent(String userId, String itemId, float timestamp) {
        this.userId = userId;
        this.itemId = itemId;
        this.timestamp = timestamp;
        this.eventValue = "null";
        this.setEventType();
    }

    public PurchaseEvent(String userId, String itemId, double timestamp) {
        this.userId = userId;
        this.itemId = itemId;
        this.timestamp = timestamp;
        this.eventValue = "null";
        this.setEventType();
    }

    private void setEventType() {
        this.eventType = "purchase";
    }
}

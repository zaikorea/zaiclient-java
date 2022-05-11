package org.zaikorea.ZaiClient.request;

public class CustomEvent extends Event {

    public CustomEvent(String userId, String itemId, String eventType, String eventValue) {
        this(userId, itemId, eventType, eventValue, Event.getCurrentUnixTimestamp());
    }

    public CustomEvent(String userId, String itemId, String eventType, String eventValue, double timestamp) {
        this.userId = userId;
        this.itemId = itemId;
        this.timestamp = timestamp;
        this.eventType = eventType;
        this.eventValue = eventValue;
    }
}

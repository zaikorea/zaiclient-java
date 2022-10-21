package org.zaikorea.ZaiClient.request;

public class CustomEvent extends Event {

    public CustomEvent(String userId, String itemId, String eventType, String eventValue) {
        this(userId, itemId, eventType, eventValue, Event.getCurrentUnixTimestamp());
    }

    public CustomEvent(String userId, String itemId, String eventType, String eventValue, double timestamp) {
        this.setUserId(userId);
        this.setItemId(itemId);
        this.setTimestamp(timestamp);
        this.setEventType(eventType);
        this.setEventValue(eventValue);
    }
}

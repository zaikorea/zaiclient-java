package org.zaikorea.ZaiClient.request;

public class ViewEvent extends Event {

    private static final String defaultEventType = "view";
    private static final String defaultEventValue = "null";

    public ViewEvent(String userId, String itemId) {
        this(userId, itemId, Event.getCurrentUnixTimestamp());
    }

    public ViewEvent(String userId, String itemId, double timestamp) {
        this.userId = userId;
        this.itemId = itemId;
        this.timestamp = timestamp;
        this.eventType = defaultEventType;
        this.eventValue = defaultEventValue;
    }
}

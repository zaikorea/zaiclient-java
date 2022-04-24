package org.zaikorea.ZaiClient.request;

public class LikeEvent extends Event {

    private static final String defaultEventType = "like";
    private static final String defaultEventValue = "1";

    public LikeEvent(String userId, String itemId) {
        this(userId, itemId, Event.getCurrentUnixTimestamp());
    }

    public LikeEvent(String userId, String itemId, double timestamp) {
        this.userId = userId;
        this.itemId = itemId;
        this.timestamp = timestamp;
        this.eventType = defaultEventType;
        this.eventValue = defaultEventValue;
    }
}

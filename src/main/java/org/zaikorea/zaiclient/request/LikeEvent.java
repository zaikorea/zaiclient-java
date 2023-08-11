package org.zaikorea.zaiclient.request;

public class LikeEvent extends Event {

    private static final String defaultEventType = "like";
    private static final String defaultEventValue = "null";

    public LikeEvent(String userId, String itemId) {
        this(userId, itemId, Event.getCurrentUnixTimestamp());
    }

    public LikeEvent(String userId, String itemId, double timestamp) {
        this.setUserId(userId);
        this.setItemId(itemId);
        this.setTimestamp(timestamp);
        this.setEventType(defaultEventType);
        this.setEventValue(defaultEventValue);
    }
}

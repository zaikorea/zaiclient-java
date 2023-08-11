package org.zaikorea.zaiclient.request;

public class ViewEvent extends Event {

    private static final String defaultEventType = "view";
    private static final String defaultEventValue = "null";

    public ViewEvent(String userId, String itemId) {
        this(userId, itemId, Event.getCurrentUnixTimestamp());
    }

    public ViewEvent(String userId, String itemId, double timestamp) {
        this.setUserId(userId);
        this.setItemId(itemId);
        this.setTimestamp(timestamp);
        this.setEventType(defaultEventType);
        this.setEventValue(defaultEventValue);
    }
}

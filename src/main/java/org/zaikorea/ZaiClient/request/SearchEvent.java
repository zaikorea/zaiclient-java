package org.zaikorea.ZaiClient.request;

public class SearchEvent extends Event {

    private static final String defaultEventType = "search";
    private static final String defaultItemId = "null";

    public SearchEvent(String userId, String searchQuery) {
        this(userId, searchQuery, Event.getCurrentUnixTimestamp());
    }

    public SearchEvent(String userId, String searchQuery, double timestamp) {
        this.userId = userId;
        this.itemId = defaultItemId;
        this.timestamp = timestamp;
        this.eventType = defaultEventType;
        this.eventValue = searchQuery;
    }
}

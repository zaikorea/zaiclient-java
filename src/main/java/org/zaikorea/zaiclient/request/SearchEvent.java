package org.zaikorea.zaiclient.request;

public class SearchEvent extends Event {

    private static final String defaultEventType = "search";
    private static final String defaultItemId = "null";

    public SearchEvent(String userId, String searchQuery) {
        this(userId, searchQuery, Event.getCurrentUnixTimestamp());
    }

    public SearchEvent(String userId, String searchQuery, double timestamp) {
        this.setUserId(userId);
        this.setItemId(defaultItemId);
        this.setTimestamp(timestamp);
        this.setEventType(defaultEventType);
        this.setEventValue(searchQuery);
    }
}

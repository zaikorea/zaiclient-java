package org.zaikorea.ZaiClient.request;

public class PageViewEvent extends Event {

    private static final String defaultEventType = "page_view";
    private static final String defaultItemId = "1";

    public PageViewEvent(String userId, String pageType) {
        this(userId, pageType, Event.getCurrentUnixTimestamp());
    }

    public PageViewEvent(String userId, String pageType, double timestamp) {
        this.userId = userId;
        this.itemId = defaultItemId;
        this.timestamp = timestamp;
        this.eventType = defaultEventType;
        this.eventValue = pageType;
    }

}

package org.zaikorea.ZaiClient.request;

public class PageViewEvent extends Event {

    private static final String defaultEventType = "page_view";
    private static final String defaultItemId = "null";

    public PageViewEvent(String userId, String pageType) {
        this(userId, pageType, Event.getCurrentUnixTimestamp());
    }

    public PageViewEvent(String userId, String pageType, double timestamp) {
        this.setUserId(userId);
        this.setItemId(defaultItemId);
        this.setTimestamp(timestamp);
        this.setEventType(defaultEventType);
        this.setEventValue(pageType);
    }

}

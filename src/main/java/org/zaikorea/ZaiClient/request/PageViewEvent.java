package org.zaikorea.ZaiClient.request;

public class PageViewEvent extends Event {

    private static final String defaultEventType = "page_view";
    private static final String defaultItemId = "null";

    public PageViewEvent(String userId, String pageType) {
        this.setUserId(userId);
        this.setItemId(defaultItemId);
        this.setTimestamp(getCurrentUnixTimestamp());
        this.setEventType(defaultEventType);
        this.setEventValue(pageType);
    }

    public PageViewEvent setContainsZaiRec(boolean containsZaiRec) {
        this.isZaiRecommendation = containsZaiRec;
        return this;
    }

    public PageViewEvent setTimestamp(double timestamp) {
        super.setTimeStamp(timestamp);
        return this;
    }
}

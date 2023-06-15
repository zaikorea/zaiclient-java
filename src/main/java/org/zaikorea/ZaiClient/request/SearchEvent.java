package org.zaikorea.ZaiClient.request;

public class SearchEvent extends Event {

    private static final String defaultEventType = "search";
    private static final String defaultItemId = "null";

    public SearchEvent(String userId, String searchQuery) {
        this.setUserId(userId);
        this.setItemId(defaultItemId);
        this.setTimestamp(getCurrentUnixTimestamp());
        this.setEventType(defaultEventType);
        this.setEventValue(searchQuery);
    }

    public SearchEvent setIsZaiRec(boolean isZaiRec) {
        this.isZaiRecommendation = isZaiRec;
        return this;
    }

    public SearchEvent setTimestamp(double timestamp) {
        super.setTimeStamp(timestamp);
        return this;
    }
}

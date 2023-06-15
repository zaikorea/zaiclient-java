package org.zaikorea.zaiclient.request;

public class RateEvent extends Event{

    private static final String defaultEventType = "rate";

    public RateEvent(String userId, String itemId, double rating) {
        this.setUserId(userId);
        this.setItemId(itemId);
        this.setTimestamp(getCurrentUnixTimestamp());
        this.setEventType(defaultEventType);
        this.setEventValue(Double.toString(rating));
    }

    public RateEvent setIsZaiRec(boolean isZaiRec) {
        this.isZaiRecommendation = isZaiRec;
        return this;
    }

    public RateEvent setTimestamp(double timestamp) {
        super.setTimeStamp(timestamp);
        return this;
    }
}

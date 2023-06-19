package org.zaikorea.ZaiClient.request;

public class CartaddEvent extends Event {

    private static final String defaultEventType = "cartadd";
    private static final String defaultEventValue = "null";

    public CartaddEvent(String userId, String itemId) {
        this.setUserId(userId);
        this.setItemId(itemId);
        this.setTimestamp(getCurrentUnixTimestamp());
        this.setEventType(defaultEventType);
        this.setEventValue(defaultEventValue);
    }

    public CartaddEvent setIsZaiRec(boolean isZaiRec) {
        this.isZaiRecommendation = isZaiRec;
        return this;
    }

    public CartaddEvent setFrom(String from) {
        if (from.length() == 0)
            this.from = null;
        else if (from.length() > 500)
            this.from = from.substring(0, 500);
        else
            this.from = from;
        
        return this;
    }

    public CartaddEvent setTimestamp(double timestamp) {
        super.setTimeStamp(timestamp);
        return this;
    }
}

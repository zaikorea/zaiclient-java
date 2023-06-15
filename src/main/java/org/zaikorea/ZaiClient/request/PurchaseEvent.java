package org.zaikorea.ZaiClient.request;

public class PurchaseEvent extends Event {

    private static final String defaultEventType = "purchase";

    public PurchaseEvent(String userId, String itemId, int price) {
        this.setUserId(userId);
        this.setItemId(itemId);
        this.setTimestamp(getCurrentUnixTimestamp());
        this.setEventType(defaultEventType);
        this.setEventValue(Integer.toString(price));
    }

    public PurchaseEvent setIsZaiRec(boolean isZaiRec) {
        this.isZaiRecommendation = isZaiRec;
        return this;
    }

    public PurchaseEvent setTimestamp(double timestamp) {
        super.setTimeStamp(timestamp);
        return this;
    }
}

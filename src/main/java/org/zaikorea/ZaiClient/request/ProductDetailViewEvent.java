package org.zaikorea.ZaiClient.request;

public class ProductDetailViewEvent extends Event {

    private static final String defaultEventType = "product_detail_view";
    private static final String defaultEventValue = "null";

    public ProductDetailViewEvent(String userId, String itemId) {
        this.setUserId(userId);
        this.setItemId(itemId);
        this.setTimestamp(getCurrentUnixTimestamp());
        this.setEventType(defaultEventType);
        this.setEventValue(defaultEventValue);
    }

    public ProductDetailViewEvent setIsZaiRec(boolean isZaiRec) {
        this.isZaiRecommendation = isZaiRec;
        return this;
    }

    public ProductDetailViewEvent setFrom(String from) {
        if (from.length() == 0)
            this.from = null;
        else if (from.length() > 500)
            this.from = from.substring(0, 500);
        else
            this.from = from;

        return this;
    }

    public ProductDetailViewEvent setTimestamp(double timestamp) {
        super.setTimeStamp(timestamp);
        return this;
    }
}

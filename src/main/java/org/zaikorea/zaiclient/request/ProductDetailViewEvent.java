package org.zaikorea.ZaiClient.request;

public class ProductDetailViewEvent extends Event {

    private static final String defaultEventType = "product_detail_view";
    private static final String defaultEventValue = "null";

    public ProductDetailViewEvent(String userId, String itemId) {
        this(userId, itemId, Event.getCurrentUnixTimestamp());
    }

    public ProductDetailViewEvent(String userId, String itemId, double timestamp) {
        this.setUserId(userId);
        this.setItemId(itemId);
        this.setTimestamp(timestamp);
        this.setEventType(defaultEventType);
        this.setEventValue(defaultEventValue);
    }

}

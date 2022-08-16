package org.zaikorea.ZaiClient.request;

public class ProductDetailViewEvent extends Event {

    private static final String defaultEventType = "product_detail_view";
    private static final String defaultEventValue = "1";

    public ProductDetailViewEvent(String userId, String itemId) {
        this(userId, itemId, Event.getCurrentUnixTimestamp());
    }

    public ProductDetailViewEvent(String userId, String itemId, double timestamp) {
        this.userId = userId;
        this.itemId = itemId;
        this.timestamp = timestamp;
        this.eventType = defaultEventType;
        this.eventValue = defaultEventValue;
    }

}

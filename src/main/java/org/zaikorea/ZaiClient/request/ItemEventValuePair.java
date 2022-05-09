package org.zaikorea.ZaiClient.request;

public class ItemEventValuePair {

    private String itemId;
    private String EventValue;

    public ItemEventValuePair(String itemId, Integer EventValue) {
        this.itemId = itemId;
        this.EventValue = Integer.toString(EventValue);
    }

    public ItemEventValuePair(String itemId, Double EventValue) {
        this.itemId = itemId;
        this.EventValue = Double.toString(EventValue);
    }

    public String getItemId() { return this.itemId; }
    public String getEventValue() { return this.EventValue; }
}

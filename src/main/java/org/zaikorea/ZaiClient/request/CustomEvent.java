package org.zaikorea.ZaiClient.request;

public class CustomEvent extends Event {

    public CustomEvent(String userId, String itemId, String eventType, String eventValue) {
        this.setUserId(userId);
        this.setItemId(itemId);
        this.setTimestamp(getCurrentUnixTimestamp());
        this.setEventType(eventType);
        this.setEventValue(eventValue);
    }

    public CustomEvent setIsZaiRec(boolean isZaiRec) {
        this.isZaiRecommendation = isZaiRec;
        return this;
    }

    public CustomEvent setFrom(String from) {
        if (from == null) {
            this.from = from;
            return this;
        }

        if (from.length() == 0)
            this.from = null;
        else if (from.length() > 500)
            this.from = from.substring(0, 500);
        else
            this.from = from;
        
        return this;
    }

    public CustomEvent setTimestamp(double timestamp) {
        super.setTimeStamp(timestamp);
        return this;
    }
}

package org.zaikorea.ZaiClient.request;

import java.security.InvalidParameterException;

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
            return this;
        }

        if (from.length() > 500)
            throw new InvalidParameterException("Length of from value must be between 1 and 500.");

        this.from = from;
        return this;
    }

    public CustomEvent setTimestamp(double timestamp) {
        super.setTimeStamp(timestamp);
        return this;
    }
}

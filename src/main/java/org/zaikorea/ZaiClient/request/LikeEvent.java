package org.zaikorea.ZaiClient.request;

import java.security.InvalidParameterException;

public class LikeEvent extends Event {

    private static final String defaultEventType = "like";
    private static final String defaultEventValue = "null";

    public LikeEvent(String userId, String itemId) {
        this.setUserId(userId);
        this.setItemId(itemId);
        this.setTimestamp(getCurrentUnixTimestamp());
        this.setEventType(defaultEventType);
        this.setEventValue(defaultEventValue);
    }

    public LikeEvent setIsZaiRec(boolean isZaiRec) {
        this.isZaiRecommendation = isZaiRec;
        return this;
    }

    public LikeEvent setFrom(String from) {
        if (from.length() > 500)
            throw new InvalidParameterException("Length of from value must be between 1 and 500.");

        this.from = from;
        return this;
    }

    public LikeEvent setTimestamp(double timestamp) {
        super.setTimeStamp(timestamp);
        return this;
    }
}

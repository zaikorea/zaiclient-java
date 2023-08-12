package org.zaikorea.zaiclient.request.events;

import org.zaikorea.zaiclient.utils.Utils;

public class AddRateEvent extends EventRequest {
    private static final String DEFAULT_EVENT_TYPE = "rate";

    public AddRateEvent(Builder builder) {
        this.events.add(new Event()
            .setUserId(builder.userId)
            .setItemId(builder.itemId)
            .setTimestamp(Utils.getCurrentUnixTimestamp())
            .setEventType(DEFAULT_EVENT_TYPE)
            .setEventValue(builder.eventValue)
            .setIsZaiRecommendation(builder.isZaiRecommendation)
            .setFrom(builder.from)
        );
    }

    public static class Builder {
        private final String userId;
        private final String itemId;
        private final String eventValue;
        private boolean isZaiRecommendation = false;
        private String from;

        public Builder(String userId, String itemId, double rating) {
            this.userId = userId;
            this.itemId = itemId;
            this.eventValue = Double.toString(rating);
        }

        public Builder setFrom(String from) {
            this.from = from;

            return this;
        }

        public Builder setIsZaiRecommendation(boolean isZaiRec) {
            this.isZaiRecommendation = isZaiRec;

            return this;
        }

        public AddRateEvent build() {
            return new AddRateEvent(this);
        }
    }
}

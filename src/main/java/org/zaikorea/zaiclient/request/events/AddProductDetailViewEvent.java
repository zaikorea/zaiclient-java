package org.zaikorea.zaiclient.request.events;

import org.zaikorea.zaiclient.utils.Utils;

public class AddProductDetailViewEvent extends EventRequest {
    private static final String DEFAULT_EVENT_TYPE = "product_detail_view";
    private static final String DEFAULT_EVENT_VALUE = "null";

    public AddProductDetailViewEvent(Builder builder) {
        this.events.add(new Event()
            .setUserId(builder.userId)
            .setItemId(builder.itemId)
            .setTimestamp(builder.timestamp)
            .setEventType(builder.eventType)
            .setEventValue(builder.eventValue)
            .setTimeToLive(builder.timeToLive)
            .setIsZaiRecommendation(builder.isZaiRecommendation)
            .setFrom(builder.from)
        );
    }

    public static class Builder {
        private final String userId;
        private final String itemId;
        private String eventType = DEFAULT_EVENT_TYPE;
        private String eventValue = DEFAULT_EVENT_VALUE;
        private double timestamp = Utils.getCurrentUnixTimestamp();
        private String from = null;
        private Integer timeToLive = null;
        private boolean isZaiRecommendation = false;

        public Builder(String userId, String itemId) {
            this.userId = userId;
            this.itemId = itemId;
        }

        public Builder setEventType(String eventType) {
            this.eventType = eventType;
            return this;
        }

        public Builder setEventValue(String eventValue) {
            this.eventValue = eventValue;
            return this;
        }

        public Builder setTimestamp(double timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder setTimeToLive(Integer timeToLive) {
            this.timeToLive = timeToLive;
            return this;
        }

        public Builder setIsZaiRecommendation(boolean isZaiRecommendation) {
            this.isZaiRecommendation = isZaiRecommendation;
            return this;
        }

        public Builder setFrom(String from) {
            this.from = from;
            return this;
        }

        public AddProductDetailViewEvent build() {
            return new AddProductDetailViewEvent(this);
        }
    }
}

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
            .setEventType(DEFAULT_EVENT_TYPE)
            .setEventValue(DEFAULT_EVENT_VALUE)
            .setTimeToLive(builder.timeToLive)
            .setIsZaiRecommendation(builder.isZaiRecommendation)
            .setFrom(builder.from)
        );
    }

    public static class Builder {
        private final String userId;
        private final String itemId;
        private double timestamp = Utils.getCurrentUnixTimestamp();
        private String from = null;
        private Integer timeToLive = null;
        private boolean isZaiRecommendation = false;

        public Builder(String userId, String itemId) {
            this.userId = userId;
            this.itemId = itemId;
        }

        public Builder timestamp(double timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder timeToLive(Integer timeToLive) {
            this.timeToLive = timeToLive;
            return this;
        }

        public Builder isZaiRecommendation(boolean isZaiRecommendation) {
            this.isZaiRecommendation = isZaiRecommendation;
            return this;
        }

        public Builder from(String from) {
            this.from = from;
            return this;
        }

        public AddProductDetailViewEvent build() {
            return new AddProductDetailViewEvent(this);
        }
    }
}

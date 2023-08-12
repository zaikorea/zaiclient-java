package org.zaikorea.zaiclient.request.events;

import org.zaikorea.zaiclient.utils.Utils;

public class AddLikeEvent extends EventRequest {
    private static final String DEFAULT_EVENT_TYPE = "like";
    private static final String DEFAULT_EVENT_VALUE = "null";

    public AddLikeEvent(Builder builder) {
        this.events.add(new Event()
            .setUserId(builder.userId)
            .setItemId(builder.itemId)
            .setTimestamp(Utils.getCurrentUnixTimestamp())
            .setEventType(DEFAULT_EVENT_TYPE)
            .setEventValue(DEFAULT_EVENT_VALUE)
            .setIsZaiRecommendation(builder.isZaiRecommendation)
            .setFrom(builder.from)
        );
    }

    public static class Builder {
        private final String userId;
        private final String itemId;
        private boolean isZaiRecommendation = false;
        private String from;

        public Builder(String userId, String itemId) {
            this.userId = userId;
            this.itemId = itemId;
        }

        public Builder setFrom(String from) {
            this.from = from;

            return this;
        }

        public Builder setIsZaiRecommendation(boolean isZaiRec) {
            this.isZaiRecommendation = isZaiRec;

            return this;
        }

        public AddLikeEvent build() {
            return new AddLikeEvent(this);
        }
    }
}

package org.zaikorea.zaiclient.request.events;

import org.zaikorea.zaiclient.utils.Utils;

public class AddSearchEvent extends EventRequest {
    private static final String DEFAULT_EVENT_TYPE = "search";
    private static final String DEFAULT_ITEM_ID = "null";

    public AddSearchEvent(Builder builder) {
        this.events.add(new Event()
            .setUserId(builder.userId)
            .setItemId(DEFAULT_ITEM_ID)
            .setTimestamp(Utils.getCurrentUnixTimestamp())
            .setEventType(DEFAULT_EVENT_TYPE)
            .setEventValue(builder.eventValue)
            .setIsZaiRecommendation(builder.isZaiRecommendation)
        );
    }

    public static class Builder {
        private final String userId;
        private final String eventValue;
        private boolean isZaiRecommendation = false;

        public Builder(String userId, String searchQuery) {
            this.userId = userId;
            this.eventValue = searchQuery;
        }

        public Builder setIsZaiRecommendation(boolean isZaiRec) {
            this.isZaiRecommendation = isZaiRec;

            return this;
        }

        public AddSearchEvent build() {
            return new AddSearchEvent(this);
        }
    }
}

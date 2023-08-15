package org.zaikorea.zaiclient.request.events;

import org.zaikorea.zaiclient.utils.Utils;

public class AddPageViewEvent extends EventRequest {
    private static final String DEFAULT_EVENT_TYPE = "page_view";
    private static final String DEFAULT_ITEM_ID = "null";

    public AddPageViewEvent(Builder builder) {
        this.events.add(new Event()
            .setUserId(builder.userId)
            .setItemId(DEFAULT_ITEM_ID)
            .setTimestamp(Utils.getCurrentUnixTimestamp())
            .setEventType(DEFAULT_EVENT_TYPE)
            .setEventValue(builder.pageType)
            .setIsZaiRecommendation(builder.isZaiRecommendation)
            .setFrom(builder.from)
        );
    }

    public static class Builder {
        private final String userId;
        private final String pageType;
        private boolean isZaiRecommendation = false;
        private String from = null;

        public Builder(String userId, String pageType) {
            this.userId = userId;
            this.pageType = pageType;
        }

        public Builder containsZaiRec(boolean containsZaiRec) {
            this.isZaiRecommendation = containsZaiRec;

            return this;
        }

        public Builder from(String from) {
            this.from = from;

            return this;
        }

        public AddPageViewEvent build() {
            return new AddPageViewEvent(this);
        }
    }
}

package org.zaikorea.zaiclient.request.events;

import java.util.Map;

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
                .setUrl(builder.url)
                .setRef(builder.ref)
                .setEventProperties(builder.eventProperties)
                .setUserProperties(builder.userProperties));
    }

    public static class Builder {
        private final String userId;
        private final String eventValue;
        private boolean isZaiRecommendation = false;
        private String url = null;
        private String ref = null;
        private Map<String, ?> eventProperties = null;
        private Map<String, ?> userProperties = null;

        public Builder(String userId, String searchQuery) {
            this.userId = userId;
            this.eventValue = searchQuery;
        }

        public Builder isZaiRecommendation(boolean isZaiRec) {
            this.isZaiRecommendation = isZaiRec;

            return this;
        }

        public Builder url(String url) {
            this.url = url;

            return this;
        }

        public Builder ref(String ref) {
            this.ref = ref;

            return this;
        }

        public Builder eventProperties(Map<String, ?> eventProperties) {
            this.eventProperties = eventProperties;

            return this;
        }

        public Builder userProperties(Map<String, ?> userProperties) {
            this.userProperties = userProperties;

            return this;
        }

        public AddSearchEvent build() {
            return new AddSearchEvent(this);
        }
    }
}

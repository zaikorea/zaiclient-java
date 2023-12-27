package org.zaikorea.zaiclient.request.events;

import java.util.Map;

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
                .setUrl(builder.url)
                .setRef(builder.ref)
                .setEventProperties(builder.eventProperties)
                .setUserProperties(builder.userProperties));
    }

    public static class Builder {
        private final String userId;
        private final String pageType;
        private boolean isZaiRecommendation = false;
        private String from = null;
        private String url = null;
        private String ref = null;
        private Map<String, ?> eventProperties = null;
        private Map<String, ?> userProperties = null;

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

        public AddPageViewEvent build() {
            return new AddPageViewEvent(this);
        }
    }
}

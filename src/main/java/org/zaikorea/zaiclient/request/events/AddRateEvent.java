package org.zaikorea.zaiclient.request.events;

import java.util.HashMap;
import java.util.Map;

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
                .setUrl(builder.url)
                .setRef(builder.ref)
                .setEventProperties(builder.eventProperties)
                .setUserProperties(builder.userProperties));
    }

    public static class Builder {
        private final String userId;
        private final String itemId;
        private final String eventValue;
        private boolean isZaiRecommendation = false;
        private String from = null;
        private String url = null;
        private String ref = null;
        private Map<String, ?> eventProperties = new HashMap<>();
        private Map<String, ?> userProperties = new HashMap<>();

        public Builder(String userId, String itemId, double rating) {
            this.userId = userId;
            this.itemId = itemId;
            this.eventValue = Double.toString(rating);
        }

        public Builder from(String from) {
            this.from = from;

            return this;
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

        public AddRateEvent build() {
            return new AddRateEvent(this);
        }
    }
}

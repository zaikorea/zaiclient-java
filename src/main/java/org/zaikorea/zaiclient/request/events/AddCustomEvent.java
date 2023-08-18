package org.zaikorea.zaiclient.request.events;

import java.util.ArrayList;
import java.util.List;

import org.zaikorea.zaiclient.configs.Config;
import org.zaikorea.zaiclient.exceptions.EmptyBatchException;
import org.zaikorea.zaiclient.exceptions.ItemNotFoundException;
import org.zaikorea.zaiclient.utils.Utils;

public class AddCustomEvent extends EventRequest {

    private AddCustomEvent(Builder builder) {
        double timestamp = Utils.getCurrentUnixTimestamp();

        if (builder.itemIds.isEmpty()) {
            throw new EmptyBatchException();
        }

        for (int i = 0; i < builder.itemIds.size(); i++) {
            this.events.add(new Event()
                .setUserId(builder.userId)
                .setItemId(builder.itemIds.get(i))
                .setTimestamp(timestamp + Config.epsilon * i)
                .setEventType(builder.eventType)
                .setEventValue(builder.eventValues.get(i))
                .setTimeToLive(builder.timeToLive.get(i))
                .setIsZaiRecommendation(builder.isZaiRecommendation.get(i))
                .setFrom(builder.from.get(i))
            );
        }
    }

    public static class Builder {
        private final String userId;
        private final String eventType;
        private List<String> itemIds = new ArrayList<>();
        private List<String> eventValues = new ArrayList<>();
        private List<Boolean> isZaiRecommendation = new ArrayList<>();
        private List<String> from = new ArrayList<>();
        private List<Integer> timeToLive = new ArrayList<>();

        public Builder(String userId, String eventType) {
            this.userId = userId;
            this.eventType = eventType;
        }

        public Builder addEventItem(String itemId, String eventValue) {
            this.itemIds.add(itemId);
            this.eventValues.add(eventValue);
            this.isZaiRecommendation.add(false);
            this.from.add(null);
            this.timeToLive.add(null);

            return this;
        }

        public Builder addEventItem(String itemId, String eventValue, String from) {
            this.itemIds.add(itemId);
            this.eventValues.add(eventValue);
            this.isZaiRecommendation.add(false);
            this.from.add(from);
            this.timeToLive.add(null);

            return this;
        }

        public Builder addEventItem(String itemId, String eventValue, boolean isZaiRec) {
            this.itemIds.add(itemId);
            this.eventValues.add(eventValue);
            this.isZaiRecommendation.add(isZaiRec);
            this.from.add(null);
            this.timeToLive.add(null);

            return this;
        }

        public Builder addEventItem(String itemdId, String eventValue, boolean isZaiRec, String from) {
            this.itemIds.add(itemdId);
            this.eventValues.add(eventValue);
            this.isZaiRecommendation.add(isZaiRec);
            this.from.add(from);
            this.timeToLive.add(null);

            return this;
        }

        public Builder deleteEventItem(String itemId) {
            if (!this.itemIds.contains(itemId)) {
                throw new ItemNotFoundException();
            }
            int idx = this.itemIds.indexOf(itemId);
            this.itemIds.remove(idx);
            this.eventValues.remove(idx);
            this.isZaiRecommendation.remove(idx);
            this.from.remove(idx);

            return this;
        }

        public AddCustomEvent build() {
            return new AddCustomEvent(this);
        }
    }
}

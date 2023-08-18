package org.zaikorea.zaiclient.request.events;

import java.util.ArrayList;
import java.util.List;

import org.zaikorea.zaiclient.configs.Config;
import org.zaikorea.zaiclient.exceptions.EmptyBatchException;
import org.zaikorea.zaiclient.exceptions.ItemNotFoundException;
import org.zaikorea.zaiclient.utils.Utils;

public class AddPurchaseEvent extends EventRequest {
    private static final String DEFAULT_EVENT_TYPE = "purchase";

    private AddPurchaseEvent(Builder builder) {
        double timestamp = Utils.getCurrentUnixTimestamp();

        if (builder.itemIds.isEmpty()) {
            throw new EmptyBatchException();
        }

        for (int i = 0; i < builder.itemIds.size(); i++) {
            this.events.add(new Event() // TODO: Creating new Event in a loop is not a good idea. Should be done in Builder methods.
                .setUserId(builder.userId)
                .setItemId(builder.itemIds.get(i))
                .setTimestamp(timestamp + Config.epsilon * i)
                .setEventType(DEFAULT_EVENT_TYPE)
                .setEventValue(builder.eventValues.get(i))
                .setTimeToLive(builder.timeToLive.get(i))
                .setIsZaiRecommendation(builder.isZaiRecommendation.get(i))
                .setFrom(builder.from.get(i))
            );
        }
    }

    public static class Builder {
        private final String userId;
        private List<String> itemIds = new ArrayList<>();
        private List<String> eventValues = new ArrayList<>();
        private List<Boolean> isZaiRecommendation = new ArrayList<>();
        private List<String> from = new ArrayList<>();
        private List<Integer> timeToLive = new ArrayList<>();

        public Builder(String userId) {
            this.userId = userId;
        }

        public Builder addPurchase(String itemId, int price) {
            this.itemIds.add(itemId);
            this.eventValues.add(Integer.toString(price));
            this.isZaiRecommendation.add(false);
            this.from.add(null);
            this.timeToLive.add(null);
            return this;
        }

        // BackLog: This might need an interface to receive float number pricing with currency (USD, KRW, etc.)

        public Builder addPurchase(String itemId, int price, String from) {
            this.itemIds.add(itemId);
            this.eventValues.add(Integer.toString(price));
            this.isZaiRecommendation.add(false);
            this.from.add(from);
            this.timeToLive.add(null);
            return this;
        }

        public Builder addPurchase(String itemId, int price, boolean isZaiRec) {
            this.itemIds.add(itemId);
            this.eventValues.add(Integer.toString(price));
            this.isZaiRecommendation.add(isZaiRec);
            this.from.add(null);
            this.timeToLive.add(null);

            return this;
        }

        public Builder addPurchase(String itemId, int price, boolean isZaiRec, String from) {
            this.itemIds.add(itemId);
            this.eventValues.add(Integer.toString(price));
            this.isZaiRecommendation.add(isZaiRec);
            this.from.add(from);
            this.timeToLive.add(null);

            return this;
        }

        public Builder deletePurchase(String itemId) {
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

        public AddPurchaseEvent build() {
            return new AddPurchaseEvent(this);
        }
    }
}

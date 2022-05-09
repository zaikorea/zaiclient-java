package org.zaikorea.ZaiClient.request;

import org.zaikorea.ZaiClient.configs.Config;

import java.util.ArrayList;

public class CartaddEventBatch extends EventBatch {

    public CartaddEventBatch(String userId, ArrayList<String> itemIds) {
        this(userId, itemIds, EventBatch.getCurrentUnixTimestamp());
    }

    public CartaddEventBatch(String userId, ArrayList<String> itemIds, double timestamp) {
        this.userId = userId;
        this.itemIds = itemIds;
        this.timestamp = timestamp;
    }

    @Override
    public ArrayList<Event> getEventList() {
        ArrayList<Event> events = new ArrayList<>();

        for (int i = 0; i < this.itemIds.size(); i++) {
            Event event = new CartaddEvent(this.userId, this.itemIds.get(i), this.timestamp + Config.epsilon * i);
            events.add(event);
        }
        return events;
    }
}

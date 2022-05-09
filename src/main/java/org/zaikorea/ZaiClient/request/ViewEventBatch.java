package org.zaikorea.ZaiClient.request;

import org.zaikorea.ZaiClient.configs.Config;

import java.util.ArrayList;

public class ViewEventBatch extends EventBatch {

    public ViewEventBatch(String userId, ArrayList<String> itemIds) {
        this(userId, itemIds, EventBatch.getCurrentUnixTimestamp());
    }

    public ViewEventBatch(String userId, ArrayList<String> itemIds, double timestamp) {
        this.userId = userId;
        this.itemIds = itemIds;
        this.timestamp = timestamp;
    }

    @Override
    public ArrayList<Event> getEventList() {
        ArrayList<Event> events = new ArrayList<>();

        for (int i = 0; i < this.itemIds.size(); i++) {
            Event event = new ViewEvent(this.userId, this.itemIds.get(i), this.timestamp + Config.epsilon * i);
            events.add(event);
        }
        return events;
    }

}

package org.zaikorea.ZaiClient.request;

import org.zaikorea.ZaiClient.configs.Config;

import java.util.ArrayList;
import java.util.Collections;

public class LikeEventBatch extends EventBatch {

    private static final String defaultEventType = "like";
    private static final String defaultEventValue = "1";

    public LikeEventBatch(String userId, ArrayList<String> itemIds) {
        this(userId, itemIds, EventBatch.getCurrentUnixTimestamp());
    }

    public LikeEventBatch(String userId, ArrayList<String> itemIds, double timestamp) {
        this.userId = userId;
        this.itemIds = itemIds;
        this.timestamp = timestamp;
        this.eventType = defaultEventType;
        this.eventValues = new ArrayList<>(Collections.nCopies(itemIds.size(), defaultEventValue));
    }

    @Override
    public ArrayList<Event> getEventList() {
        ArrayList<Event> events = new ArrayList<>();

        for (int i = 0; i < this.itemIds.size(); i++) {
            Event event = new LikeEvent(this.userId, this.itemIds.get(i), this.timestamp + Config.epsilon * i);
            events.add(event);
        }
        return events;
    }
}

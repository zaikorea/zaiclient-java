package org.zaikorea.ZaiClient.request;

import org.zaikorea.ZaiClient.configs.Config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class ViewEventBatch extends EventBatch {

    private static final String defaultEventType = "view";
    private static final String defaultEventValue = "1";

    public ViewEventBatch(String userId, ArrayList<String> itemIds) {
        this(userId, itemIds, EventBatch.getCurrentUnixTimestamp());
    }

    public ViewEventBatch(String userId, ArrayList<String> itemIds, double timestamp) {
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
            Event event = new ViewEvent(this.userId, this.itemIds.get(i), this.timestamp + Config.epsilon * i);
            events.add(event);
        }
        return events;
    }

}

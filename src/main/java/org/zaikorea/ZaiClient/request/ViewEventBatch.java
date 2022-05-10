package org.zaikorea.ZaiClient.request;

import org.zaikorea.ZaiClient.configs.Config;
import org.zaikorea.ZaiClient.exceptions.ItemNotFoundException;
import org.zaikorea.ZaiClient.exceptions.BatchSizeLimitExceededException;
import org.zaikorea.ZaiClient.exceptions.LoggedEventBatchException;

import java.util.ArrayList;

public class ViewEventBatch extends EventBatch {

    private static final String defaultEventType = "view";
    private static final String defaultEventValue = "1";

    public ViewEventBatch(String userId) {
        this(userId, EventBatch.getCurrentUnixTimestamp());
    }

    public ViewEventBatch(String userId, double timestamp) {
        this.userId = userId;
        this.itemIds = new ArrayList<>();
        this.timestamp = timestamp;
        this.eventType = defaultEventType;
        this.eventValues = new ArrayList<>();
    }

    @Override
    public ArrayList<Event> getEventList() {
        ArrayList<Event> events = new ArrayList<>();
        int i = 0;

        for (String itemId : itemIds) {
            Event event = new ViewEvent(this.userId, itemId, this.timestamp + Config.epsilon * i++);
            events.add(event);
        }
        return events;
    }

    public void addEventItem(String itemId) throws LoggedEventBatchException, BatchSizeLimitExceededException {
        super.addEventItem(itemId, defaultEventValue);
    }

    public void deleteEventItem(String itemId) throws LoggedEventBatchException, ItemNotFoundException {
        super.deleteEventItem(itemId, defaultEventValue);
    }

}

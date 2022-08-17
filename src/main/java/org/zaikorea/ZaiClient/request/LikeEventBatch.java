package org.zaikorea.ZaiClient.request;

import java.util.ArrayList;
import java.util.List;

import org.zaikorea.ZaiClient.configs.Config;
import org.zaikorea.ZaiClient.exceptions.BatchSizeLimitExceededException;
import org.zaikorea.ZaiClient.exceptions.ItemNotFoundException;
import org.zaikorea.ZaiClient.exceptions.LoggedEventBatchException;

public class LikeEventBatch extends EventBatch {

    private static final String defaultEventType = "like";
    private static final String defaultEventValue = "null";

    public LikeEventBatch(String userId) {
        this(userId, EventBatch.getCurrentUnixTimestamp());
    }

    public LikeEventBatch(String userId, double timestamp) {
        this.userId = userId;
        this.itemIds = new ArrayList<>();
        this.timestamp = timestamp;
        this.eventType = defaultEventType;
        this.eventValues = new ArrayList<>();
    }

    @Override
    public List<Event> getEventList() {
        List<Event> events = new ArrayList<>();
        int i = 0;

        for (String itemId : itemIds) {
            events.add(new LikeEvent(
                this.userId, 
                itemId, 
                this.timestamp + Config.epsilon * i++
            ));
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

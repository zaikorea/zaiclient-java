package org.zaikorea.ZaiClient.request;

import java.util.ArrayList;
import java.util.List;

import org.zaikorea.ZaiClient.configs.Config;
import org.zaikorea.ZaiClient.exceptions.BatchSizeLimitExceededException;
import org.zaikorea.ZaiClient.exceptions.ItemNotFoundException;
import org.zaikorea.ZaiClient.exceptions.LoggedEventBatchException;

public class CartaddEventBatch extends EventBatch {

    private static final String defaultEventType = "cartadd";
    private static final String defaultEventValue = "1";

    public CartaddEventBatch(String userId) {
        this(userId, EventBatch.getCurrentUnixTimestamp());
    }

    public CartaddEventBatch(String userId, double timestamp) {
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
            events.add(new CartaddEvent(
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

package org.zaikorea.ZaiClient.request;

import java.util.ArrayList;
import java.util.List;

import org.zaikorea.ZaiClient.configs.Config;
import org.zaikorea.ZaiClient.exceptions.BatchSizeLimitExceededException;
import org.zaikorea.ZaiClient.exceptions.ItemNotFoundException;
import org.zaikorea.ZaiClient.exceptions.LoggedEventBatchException;

public class PurchaseEventBatch extends EventBatch {

    private static final String defaultEventType = "purchase";

    public PurchaseEventBatch(String userId) {
        this(userId, EventBatch.getCurrentUnixTimestamp());
    }

    public PurchaseEventBatch(String userId, double timestamp) {
        this.userId = userId;
        this.itemIds = new ArrayList<>();
        this.timestamp = timestamp;
        this.eventType = defaultEventType;
        this.eventValues = new ArrayList<>();
    }

    @Override
    public List<Event> getEventList() {
        List<Event> events = new ArrayList<>();

        for (int i = 0; i < this.itemIds.size(); i++) {
            events.add(new PurchaseEvent(
                this.userId, 
                this.itemIds.get(i), 
                Integer.parseInt(this.eventValues.get(i)), 
                this.timestamp + Config.epsilon * i
            ));
        }

        return events;
    }

    public void addEventItem(String itemId, int price) throws LoggedEventBatchException, BatchSizeLimitExceededException {
        super.addEventItem(itemId, Integer.toString(price));
    }
    
    public void deleteEventItem(String itemId) throws LoggedEventBatchException, ItemNotFoundException {
        super.deleteEventItem(itemId);
    }

    public void deleteEventItem(String itemId, int price) throws LoggedEventBatchException, ItemNotFoundException {
        super.deleteEventItem(itemId, Integer.toString(price));
    }
}

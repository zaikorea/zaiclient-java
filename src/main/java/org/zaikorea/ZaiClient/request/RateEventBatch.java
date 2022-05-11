package org.zaikorea.ZaiClient.request;

import java.util.ArrayList;
import java.util.List;

import org.zaikorea.ZaiClient.configs.Config;
import org.zaikorea.ZaiClient.exceptions.BatchSizeLimitExceededException;
import org.zaikorea.ZaiClient.exceptions.ItemNotFoundException;
import org.zaikorea.ZaiClient.exceptions.LoggedEventBatchException;

public class RateEventBatch extends EventBatch {

    private static final String defaultEventType = "rate";

    public RateEventBatch(String userId) {
        this(userId, EventBatch.getCurrentUnixTimestamp());
    }

    public RateEventBatch(String userId, double timestamp) {
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
            events.add(new RateEvent(
                this.userId, 
                this.itemIds.get(i), 
                Double.parseDouble(this.eventValues.get(i)), 
                this.timestamp + Config.epsilon * i
            ));
        }
        
        return events;
    }

    public void deleteEventItem(String itemId) throws LoggedEventBatchException, ItemNotFoundException {
        super.deleteEventItem(itemId);
    }

    public void addEventItem(String itemId, double rate) throws LoggedEventBatchException, BatchSizeLimitExceededException {
        super.addEventItem(itemId, Double.toString(rate));
    }

    public void deleteEventItem(String itemId, double rate) throws LoggedEventBatchException, ItemNotFoundException {
        super.deleteEventItem(itemId, Double.toString(rate));
    }
}

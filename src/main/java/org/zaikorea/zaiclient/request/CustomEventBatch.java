package org.zaikorea.zaiclient.request;

import java.util.ArrayList;
import java.util.List;

import org.zaikorea.zaiclient.configs.Config;
import org.zaikorea.zaiclient.exceptions.BatchSizeLimitExceededException;
import org.zaikorea.zaiclient.exceptions.ItemNotFoundException;
import org.zaikorea.zaiclient.exceptions.LoggedEventBatchException;

public class CustomEventBatch extends EventBatch {

    public CustomEventBatch(String userId, String eventType) {
        this(userId, eventType, EventBatch.getCurrentUnixTimestamp());
    }

    public CustomEventBatch(String userId, String eventType, double timestamp) {
        this.userId = userId;
        this.itemIds = new ArrayList<>();
        this.timestamp = timestamp;
        this.eventType = eventType;
        this.eventValues = new ArrayList<>();
    }

    @Override
    public List<Event> getEventList() {
        List<Event> events = new ArrayList<>();

        for (int i = 0; i < this.itemIds.size(); i++) {
            events.add(new CustomEvent(
                this.userId, 
                this.itemIds.get(i), 
                this.eventType, 
                this.eventValues.get(i), 
                this.timestamp + Config.epsilon * i
            ));
        }
        
        return events;
    }

    public void addEventItem(String itemId, String eventValue) throws LoggedEventBatchException, BatchSizeLimitExceededException {
        super.addEventItem(itemId, eventValue);
    }
    
    public void deleteEventItem(String itemId) throws LoggedEventBatchException, ItemNotFoundException {
        super.deleteEventItem(itemId);
    }

    public void deleteEventItem(String itemId, String eventValue) throws LoggedEventBatchException, ItemNotFoundException {
        super.deleteEventItem(itemId, eventValue);
    }
}

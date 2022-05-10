package org.zaikorea.ZaiClient.request;

import org.zaikorea.ZaiClient.configs.Config;
import org.zaikorea.ZaiClient.exceptions.ItemNotFoundException;
import org.zaikorea.ZaiClient.exceptions.BatchSizeLimitExceededException;
import org.zaikorea.ZaiClient.exceptions.LoggedEventBatchException;

import java.util.ArrayList;

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
    public ArrayList<Event> getEventList() {
        ArrayList<Event> events = new ArrayList<>();

        for (int i = 0; i < this.itemIds.size(); i++) {
            Event event = new CustomEvent(this.userId, this.itemIds.get(i), this.eventType, this.eventValues.get(i), this.timestamp + Config.epsilon * i);
            events.add(event);
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

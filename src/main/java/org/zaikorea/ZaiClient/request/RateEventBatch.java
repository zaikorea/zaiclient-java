package org.zaikorea.ZaiClient.request;

import org.zaikorea.ZaiClient.configs.Config;
import org.zaikorea.ZaiClient.exceptions.ItemNotFoundException;
import org.zaikorea.ZaiClient.exceptions.BatchSizeLimitExceededException;
import org.zaikorea.ZaiClient.exceptions.LoggedEventBatchException;

import java.util.ArrayList;

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
    public ArrayList<Event> getEventList() {
        ArrayList<Event> events = new ArrayList<>();

        for (int i = 0; i < this.itemIds.size(); i++) {
            Event event = new RateEvent(this.userId, this.itemIds.get(i), Double.parseDouble(this.eventValues.get(i)), this.timestamp + Config.epsilon * i);
            events.add(event);
        }
        return events;
    }

    public void addEventItem(String itemId, double rate) throws LoggedEventBatchException, BatchSizeLimitExceededException {
        super.addEventItem(itemId, Double.toString(rate));
    }

    public void deleteEventItem(String itemId) throws LoggedEventBatchException, ItemNotFoundException {
        super.deleteEventItem(itemId);
    }

    public void deleteEventItem(String itemId, double rate) throws LoggedEventBatchException, ItemNotFoundException {
        super.deleteEventItem(itemId, Double.toString(rate));
    }

}

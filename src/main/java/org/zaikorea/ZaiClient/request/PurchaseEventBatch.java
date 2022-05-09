package org.zaikorea.ZaiClient.request;

import org.zaikorea.ZaiClient.configs.Config;
import org.zaikorea.ZaiClient.exceptions.ItemNotFoundException;
import org.zaikorea.ZaiClient.exceptions.ItemSizeLimitExceededException;
import org.zaikorea.ZaiClient.exceptions.LoggedEventBatchException;

import java.util.ArrayList;

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
    public ArrayList<Event> getEventList() {
        ArrayList<Event> events = new ArrayList<>();

        for (int i = 0; i < this.itemIds.size(); i++) {
            Event event = new PurchaseEvent(this.userId, this.itemIds.get(i), Integer.parseInt(this.eventValues.get(i)), this.timestamp + Config.epsilon * i);
            events.add(event);
        }

        return events;
    }

    public void addItem(String itemId, int price) throws LoggedEventBatchException, ItemSizeLimitExceededException {
        super.addItem(itemId, Integer.toString(price));
    }

    public void deleteItem(String itemId) throws LoggedEventBatchException, ItemNotFoundException {
        int idx = itemIds.indexOf(itemId);
        super.deleteItem(itemId, eventValues.get(idx));
    }

    public void deleteItem(String itemId, int price) throws LoggedEventBatchException, ItemNotFoundException {
        super.deleteItem(itemId, Integer.toString(price));
    }

}

package org.zaikorea.ZaiClient.request;

import org.zaikorea.ZaiClient.configs.Config;
import org.zaikorea.ZaiClient.exceptions.ItemNotFoundException;
import org.zaikorea.ZaiClient.exceptions.LoggedEventBatchException;

import java.util.ArrayList;
import java.util.Collections;

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
    public ArrayList<Event> getEventList() {
        ArrayList<Event> events = new ArrayList<>();

        for (int i = 0; i < this.itemIds.size(); i++) {
            Event event = new CartaddEvent(this.userId, this.itemIds.get(i), this.timestamp + Config.epsilon * i);
            events.add(event);
        }
        return events;
    }

    public void addItem(String itemId) throws LoggedEventBatchException {
        super.addItem(itemId, defaultEventValue);
    }

    public void deleteItem(String itemId) throws LoggedEventBatchException, ItemNotFoundException {
        super.deleteItem(itemId, defaultEventValue);
    }
}

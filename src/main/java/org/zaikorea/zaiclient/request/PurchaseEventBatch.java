package org.zaikorea.zaiclient.request;

import java.util.ArrayList;
import java.util.List;

import org.zaikorea.zaiclient.configs.Config;
import org.zaikorea.zaiclient.exceptions.BatchSizeLimitExceededException;
import org.zaikorea.zaiclient.exceptions.ItemNotFoundException;
import org.zaikorea.zaiclient.exceptions.LoggedEventBatchException;

public class PurchaseEventBatch extends EventBatch {

    private static final String defaultEventType = "purchase";

    public PurchaseEventBatch(String userId) {
        this.userId = userId;
        this.itemIds = new ArrayList<>();
        this.timestamp = getCurrentUnixTimestamp();
        this.eventType = defaultEventType;
        this.eventValues = new ArrayList<>();
        this.isZaiRecommendation = new ArrayList<>();
        this.from = new ArrayList<>();
    }

    @Override
    public List<Event> getEventList() {
        List<Event> events = new ArrayList<>();

        for (int i = 0; i < this.itemIds.size(); i++) {
            events.add(new PurchaseEvent(this.userId, this.itemIds.get(i), Integer.parseInt(this.eventValues.get(i)))
                .setTimestamp(this.timestamp + Config.epsilon * i)
                .setIsZaiRec(this.isZaiRecommendation.get(i))
            );
        }

        return events;
    }

    public PurchaseEventBatch setTimestamp(double timestamp) {
        super.setTimeStamp(timestamp);
        return this;
    }

    public void addEventItem(String itemId, int price) throws LoggedEventBatchException, BatchSizeLimitExceededException {
        super.addEventItem(itemId, Integer.toString(price));
    }

    public void addEventItem(String itemId, int price, boolean isZaiRec)
            throws LoggedEventBatchException, BatchSizeLimitExceededException {
        super.addEventItem(itemId, Integer.toString(price), isZaiRec);
    }
    
    public void deleteEventItem(String itemId) throws LoggedEventBatchException, ItemNotFoundException {
        super.deleteEventItem(itemId);
    }

    public void deleteEventItem(String itemId, int price) throws LoggedEventBatchException, ItemNotFoundException {
        super.deleteEventItem(itemId, Integer.toString(price));
    }
}

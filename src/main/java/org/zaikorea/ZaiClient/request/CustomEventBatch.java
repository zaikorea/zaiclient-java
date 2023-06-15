package org.zaikorea.ZaiClient.request;

import java.util.ArrayList;
import java.util.List;

import org.zaikorea.ZaiClient.configs.Config;
import org.zaikorea.ZaiClient.exceptions.BatchSizeLimitExceededException;
import org.zaikorea.ZaiClient.exceptions.ItemNotFoundException;
import org.zaikorea.ZaiClient.exceptions.LoggedEventBatchException;

public class CustomEventBatch extends EventBatch {

    public CustomEventBatch(String userId, String eventType) {
        this.userId = userId;
        this.itemIds = new ArrayList<>();
        this.timestamp = getCurrentUnixTimestamp();
        this.eventType = eventType;
        this.eventValues = new ArrayList<>();
        this.isZaiRecommendation = new ArrayList<>();
        this.from = new ArrayList<>();
    }

    @Override
    public List<Event> getEventList() {
        List<Event> events = new ArrayList<>();

        for (int i = 0; i < this.itemIds.size(); i++) {
            events.add(new CustomEvent(this.userId, this.itemIds.get(i), this.eventType, this.eventValues.get(i))
                    .setTimestamp(this.timestamp + Config.epsilon * i)
                    .setIsZaiRec(this.isZaiRecommendation.get(i))
                    .setFrom(this.from.get(i))
            );
        }
        
        return events;
    }

    public CustomEventBatch setTimestamp(double timestamp) {
        super.setTimeStamp(timestamp);
        return this;
    }

    public void addEventItem(String itemId, String eventValue) throws LoggedEventBatchException, BatchSizeLimitExceededException {
        super.addEventItem(itemId, eventValue);
    }

    public void addEventItem(String itemId, String eventValue, boolean isZaiRec)
            throws LoggedEventBatchException, BatchSizeLimitExceededException {
        super.addEventItem(itemId, eventValue, isZaiRec);
    }

    public void addEventItem(String itemId, String eventValue, String from)
            throws LoggedEventBatchException, BatchSizeLimitExceededException {
        super.addEventItem(itemId, eventValue, from);
    }

    public void addEventItem(String itemId, String eventValue, boolean isZaiRec, String from)
            throws LoggedEventBatchException, BatchSizeLimitExceededException {
        super.addEventItem(itemId, eventValue, isZaiRec, from);
    }

    public void deleteEventItem(String itemId) throws LoggedEventBatchException, ItemNotFoundException {
        super.deleteEventItem(itemId);
    }

    public void deleteEventItem(String itemId, String eventValue) throws LoggedEventBatchException, ItemNotFoundException {
        super.deleteEventItem(itemId, eventValue);
    }
}

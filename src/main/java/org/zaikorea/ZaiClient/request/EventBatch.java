package org.zaikorea.ZaiClient.request;

import org.zaikorea.ZaiClient.configs.Config;
import org.zaikorea.ZaiClient.exceptions.ItemNotFoundException;
import org.zaikorea.ZaiClient.exceptions.BatchSizeLimitExceededException;
import org.zaikorea.ZaiClient.exceptions.LoggedEventBatchException;
import org.zaikorea.ZaiClient.exceptions.ZaiClientException;

import java.util.ArrayList;
import java.util.stream.IntStream;

public class EventBatch {

    protected String userId;

    protected ArrayList<String> itemIds;

    protected String eventType;

    protected ArrayList<String> eventValues;

    protected double timestamp;

    private boolean logFlag = false;

    public static double getCurrentUnixTimestamp() {
        return System.currentTimeMillis() / 1000.d;
    }

    public String getUserId() { return userId; }

    public ArrayList<String> getItemIds() { return itemIds; }

    public String getEventType() {
        return eventType;
    }

    public ArrayList<String> getEventValues() { return eventValues; }

    public double getTimestamp() { return timestamp; }

    public void setLogFlag() { this.logFlag = true; }

    public ArrayList<Event> getEventList() throws ZaiClientException { return new ArrayList<>(); }

    void addEventItem(String itemId, String eventValue) throws LoggedEventBatchException, BatchSizeLimitExceededException {
        if (this.logFlag) {
            throw new LoggedEventBatchException();
        }

        if (this.itemIds.size() == Config.batchRequestCap) {
            throw new BatchSizeLimitExceededException();
        }

        this.itemIds.add(itemId);
        this.eventValues.add(eventValue);
    }

    void deleteEventItem(String itemId) throws LoggedEventBatchException, ItemNotFoundException {
        if (logFlag) throw new LoggedEventBatchException();

        if (!this.itemIds.contains(itemId)) throw new ItemNotFoundException();

        int idx = this.itemIds.indexOf(itemId);
        this.itemIds.remove(idx);
        this.eventValues.remove(idx);
    }

    void deleteEventItem(String itemId, String eventValue) throws LoggedEventBatchException, ItemNotFoundException {
        if (logFlag) throw new LoggedEventBatchException();

        if (!this.itemIds.contains(itemId)) throw new ItemNotFoundException();

        int[] indices = IntStream
                .range(0, itemIds.size())
                .filter(i -> itemIds.get(i).equals(itemId))
                .toArray();
        boolean deleted = false;

        for (int idx: indices) {
            if (eventValue.equals(eventValues.get(idx))) {
                this.itemIds.remove(idx);
                this.eventValues.remove(idx);
                deleted = true;
                break;
            }
        }

        if (!deleted) throw new ItemNotFoundException();
    }
}

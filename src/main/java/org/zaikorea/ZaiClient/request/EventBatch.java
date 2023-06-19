package org.zaikorea.ZaiClient.request;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import org.zaikorea.ZaiClient.configs.Config;
import org.zaikorea.ZaiClient.exceptions.ItemNotFoundException;
import org.zaikorea.ZaiClient.exceptions.BatchSizeLimitExceededException;
import org.zaikorea.ZaiClient.exceptions.LoggedEventBatchException;

public class EventBatch {

    protected String userId;

    protected List<String> itemIds;

    protected String eventType;

    protected List<String> eventValues;

    protected double timestamp;

    protected List<Boolean> isZaiRecommendation;

    protected List<String> from;

    private boolean logFlag = false;

    public String getUserId() {
        return userId;
    }

    public List<String> getItemIds() {
        return itemIds;
    }

    public String getEventType() {
        return eventType;
    }

    public List<String> getEventValues() {
        return eventValues;
    }

    public double getTimestamp() {
        return timestamp;
    }

    public void setLogFlag() {
        this.logFlag = true;
    }

    protected void setTimeStamp(double timestamp) {
        this.timestamp = timestamp;
    }

    public static double getCurrentUnixTimestamp() {
        // Have to track nanosecond because client sometimes calls api multiple times in a millisecond
        // Use nanoTime because jdk 1.8 doesn't support Instant.getNano() function.
        String time = Long.toString(System.nanoTime());
        time = time.substring(time.length()-7);

        long longTime = Long.parseLong(time);
        long currentTime = System.currentTimeMillis();

        return currentTime / 1000.d + longTime / 1e10;
    }

    public List<Event> getEventList() {
        return new ArrayList<>();
    }

    protected void addEventItem(String itemId, String eventValue) throws LoggedEventBatchException, BatchSizeLimitExceededException {
        if (this.logFlag) throw new LoggedEventBatchException();

        if (this.itemIds.size() >= Config.batchRequestCap) {
            throw new BatchSizeLimitExceededException();
        }

        this.itemIds.add(itemId);
        this.eventValues.add(eventValue);
        this.isZaiRecommendation.add(false);
        this.from.add(null);
    }

    protected void addEventItem(String itemId, String eventValue, Boolean isZaiRec) {
        this.addEventItem(itemId, eventValue);
        int idx = this.isZaiRecommendation.size()-1;
        this.isZaiRecommendation.set(idx, isZaiRec);
    }

    protected void addEventItem(String itemId, String eventValue, String from) {
        this.addEventItem(itemId, eventValue);
        int idx = this.from.size()-1;

        if (from.length() == 0)
            this.from.set(idx, null);
        else if (from.length() > 500)
            this.from.set(idx, from.substring(0, 500));
        else
            this.from.set(idx, from);
    }

    protected void addEventItem(String itemId, String eventValue, Boolean isZaiRec, String from) {
        this.addEventItem(itemId, eventValue);
        int idx = this.from.size()-1;
        this.isZaiRecommendation.set(idx, isZaiRec);
        if (from.length() == 0)
            this.from.set(idx, null);
        else if (from.length() > 500)
            this.from.set(idx, from.substring(0, 500));
        else
            this.from.set(idx, from);
    }

    protected void deleteEventItem(String itemId) throws LoggedEventBatchException, ItemNotFoundException {
        if (logFlag) throw new LoggedEventBatchException();

        if (!this.itemIds.contains(itemId)) throw new ItemNotFoundException();

        int idx = this.itemIds.indexOf(itemId);
        this.itemIds.remove(idx);
        this.eventValues.remove(idx);
        this.isZaiRecommendation.remove(idx);
        this.from.remove(idx);
    }

    protected void deleteEventItem(String itemId, String eventValue) throws LoggedEventBatchException, ItemNotFoundException {
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
                this.isZaiRecommendation.remove(idx);
                this.from.remove(idx);
                deleted = true;
                break;
            }
        }

        if (!deleted) throw new ItemNotFoundException();
    }
}

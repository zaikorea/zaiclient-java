package org.zaikorea.ZaiClient.request;

import org.zaikorea.ZaiClient.configs.Config;
import org.zaikorea.ZaiClient.exceptions.ZaiClientException;

import java.util.ArrayList;

public class EventBatch {

    protected String userId;

    protected ArrayList<String> itemIds;

    protected ArrayList<String> eventValues;

    protected double timestamp;

    protected boolean logFlag = false;

    public static double getCurrentUnixTimestamp() {
        return System.currentTimeMillis() / 1000.d;
    }

    public String getUserId() { return userId; }

    public ArrayList<String> getItemIds() { return itemIds; }

    public ArrayList<String> getEventValues() { return eventValues; }

    public double getTimestamp() { return timestamp; }

    public void setLogFlagTrue() { this.logFlag = true; }

    public ArrayList<Event> getEventList() throws ZaiClientException {
        ArrayList<Event> events = new ArrayList<>();
        return events;
    }

    public void addItem(String itemId) throws ZaiClientException {
        if (logFlag) {
            throw new ZaiClientException("Cannot add item after log batch event.", 405);
        }
        this.itemIds.add(itemId);
    }

    public void addItem(String itemId, String eventValue) throws ZaiClientException {
        if (logFlag) {
            throw new ZaiClientException("Cannot add item after log batch event.", 405);
        }
        this.itemIds.add(itemId);
        this.eventValues.add(eventValue);
    }

    public void deleteItem(String itemId) throws ZaiClientException {
        if (logFlag) {
            throw new ZaiClientException("Cannot delete item after log batch event.", 405);
        }

        if (!this.itemIds.contains(itemId)) {
            throw new ZaiClientException("This itemId does not exist in itemIds of the EventBatch object.", 400);
        }

        this.itemIds.removeIf(id -> id.equals(itemId));
    }

    public void deleteItem(String itemId, String eventValue) throws ZaiClientException {
        if (logFlag) {
            throw new ZaiClientException("Cannot delete item after log batch event.", 405);
        }

        if (!this.itemIds.contains(itemId)) {
            throw new ZaiClientException("This itemId does not exist in itemIds of the EventBatch object.", 400);
        }

        this.itemIds.removeIf(id -> id.equals(itemId));
    }
}

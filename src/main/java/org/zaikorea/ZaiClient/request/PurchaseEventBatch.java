package org.zaikorea.ZaiClient.request;

import org.zaikorea.ZaiClient.configs.Config;
import org.zaikorea.ZaiClient.exceptions.ZaiClientException;

import java.util.ArrayList;
import java.util.stream.Collectors;

public class PurchaseEventBatch extends EventBatch {

    private static final String defaultEventType = "purchase";

    public PurchaseEventBatch(String userId, ArrayList<ItemEventValuePair> purchaseItems) {
        this(userId, purchaseItems, EventBatch.getCurrentUnixTimestamp());
    }

    public PurchaseEventBatch(String userId, ArrayList<ItemEventValuePair> purchaseItems, double timestamp) {
        this.userId = userId;
        this.itemIds = purchaseItems.stream().map(ItemEventValuePair::getItemId).collect(Collectors.toCollection(ArrayList::new));
        this.timestamp = timestamp;
        this.eventType = defaultEventType;
        this.eventValues =purchaseItems.stream().map(ItemEventValuePair::getEventValue).collect(Collectors.toCollection(ArrayList::new));

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

    public void addItem(String itemId, int price) throws ZaiClientException {
        super.addItem(itemId, Integer.toString(price));
    }

}

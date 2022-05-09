package org.zaikorea.ZaiClient.request;

import org.zaikorea.ZaiClient.configs.Config;

import java.util.ArrayList;
import java.util.stream.Collectors;

public class RateEventBatch extends EventBatch {

    private static final String defaultEventType = "rate";

    public RateEventBatch(String userId, ArrayList<ItemEventValuePair> purchaseItems) {
        this(userId, purchaseItems, EventBatch.getCurrentUnixTimestamp());
    }

    public RateEventBatch(String userId, ArrayList<ItemEventValuePair> purchaseItems, double timestamp) {
        this.userId = userId;
        this.itemIds = purchaseItems.stream().map(ItemEventValuePair::getItemId).collect(Collectors.toCollection(ArrayList::new));
        this.timestamp = timestamp;
        this.eventType = defaultEventType;
        this.eventValues = purchaseItems.stream().map(ItemEventValuePair::getEventValue).collect(Collectors.toCollection(ArrayList::new));
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
}

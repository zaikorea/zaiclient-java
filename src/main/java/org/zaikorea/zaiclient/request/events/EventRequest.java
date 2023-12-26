package org.zaikorea.zaiclient.request.events;

import java.util.LinkedList;
import java.util.List;

import org.zaikorea.zaiclient.configs.Config;
import org.zaikorea.zaiclient.request.IRequest;

public class EventRequest implements IRequest<List<Event>> {
    protected List<Event> events;

    public EventRequest() {
        events = new LinkedList<Event>();
        // Do nothing
    }

    public void setEvents(List<Event> events) {
        this.events = events;
    }

    public void setEventsToExpire() {
        for (Event event : events) {
            event.setTimeToLive(Config.testEventTimeToLive);
        }
    }

    @Override
    public List<Event> getPayload() {
        return events;
    }
}

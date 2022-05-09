package org.zaikorea.ZaiClient;

import org.zaikorea.ZaiClient.configs.Config;
import org.zaikorea.ZaiClient.request.Event;
import org.zaikorea.ZaiClient.response.EventLoggerResponse;
import retrofit2.Call;
import retrofit2.http.*;

import java.util.ArrayList;

public interface ZaiAPI {

    @POST(Config.eventsApiPath)
    Call<EventLoggerResponse> addEventLog(
        @Body Event event
    );

    @POST(Config.eventsApiPath)
    Call<EventLoggerResponse> addEventLog(
        @Body ArrayList<Event> event
    );

    @PUT(Config.eventsApiPath)
    Call<EventLoggerResponse> updateEventLog(
        @Body Event event
    );

    @HTTP(method="DELETE", path=Config.eventsApiPath, hasBody=true)
    Call<EventLoggerResponse> deleteEventLog(
        @Body Event event
    );

    @HTTP(method="DELETE", path=Config.eventsApiPath, hasBody=true)
    Call<EventLoggerResponse> deleteEventLog(
        @Body ArrayList<Event> event
    );
}

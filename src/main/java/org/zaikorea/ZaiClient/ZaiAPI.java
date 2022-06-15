package org.zaikorea.ZaiClient;

import java.util.List;

import org.zaikorea.ZaiClient.configs.Config;
import org.zaikorea.ZaiClient.request.Event;
import org.zaikorea.ZaiClient.request.RecommendationRequest;
import org.zaikorea.ZaiClient.response.EventLoggerResponse;
import org.zaikorea.ZaiClient.response.RecommendationResponse;

import retrofit2.Call;
import retrofit2.http.*;

public interface ZaiAPI {

    @POST(Config.eventsApiPath)
    Call<EventLoggerResponse> addEventLog(
        @Body Event event
    );

    @POST(Config.eventsApiPath)
    Call<EventLoggerResponse> addEventLog(
        @Body List<Event> event
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
        @Body List<Event> event
    );

    @POST
    Call<RecommendationResponse> getRecommendations(
        @Url String url,
        @Body RecommendationRequest recommendation
    );

}

package org.zaikorea.zaiclient;

import java.util.List;

import org.zaikorea.zaiclient.configs.Config;
import org.zaikorea.zaiclient.request.Event;
import org.zaikorea.zaiclient.request.RecommendationRequest;
import org.zaikorea.zaiclient.response.EventLoggerResponse;
import org.zaikorea.zaiclient.response.RecommendationResponse;

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

    @POST
    Call<RecommendationResponse> getRecommendations(
        @Url String url,
        @Body RecommendationRequest recommendation
    );

}

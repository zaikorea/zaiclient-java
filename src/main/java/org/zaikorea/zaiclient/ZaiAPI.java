package org.zaikorea.zaiclient;

import java.util.List;

import org.zaikorea.zaiclient.configs.Config;
import org.zaikorea.zaiclient.request.events.Event;
import org.zaikorea.zaiclient.request.recommendations.RecommendationQuery;
import org.zaikorea.zaiclient.request.items.Item;
import org.zaikorea.zaiclient.response.EventLoggerResponse;
import org.zaikorea.zaiclient.response.ItemResponse;
import org.zaikorea.zaiclient.response.RecommendationResponse;

import retrofit2.Call;
import retrofit2.http.*;

public interface ZaiAPI {

    @POST(Config.itemsApiPath)
    Call<ItemResponse> addItem(
            @Body List<Item> items);

    @PUT(Config.itemsApiPath)
    Call<ItemResponse> updateItem(
            @Body List<Item> items);

    @DELETE(Config.itemsApiPath)
    Call<ItemResponse> deleteItem(
            @Query("id") List<String> ids);

    @POST(Config.eventsApiPath)
    Call<EventLoggerResponse> addEventLog(
            @Body Event event);

    @POST(Config.eventsApiPath)
    Call<EventLoggerResponse> addEventLog(
            @Body List<Event> event);

    @POST
    Call<RecommendationResponse> getRecommendations(
            @Url String url,
            @Body RecommendationQuery recQuery);

}

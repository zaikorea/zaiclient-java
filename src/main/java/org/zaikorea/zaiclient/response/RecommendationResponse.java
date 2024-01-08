package org.zaikorea.zaiclient.response;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;

public class RecommendationResponse {

    @SerializedName("items")
    private List<String> items;

    @SerializedName("count")
    private int count;

    @SerializedName("timestamp")
    private double timestamp;

    @SerializedName("metadata")
    private String metadata;

    @SerializedName("recommendation_id")
    private String recommendationId;

    public List<String> getItems() {
        return items;
    }

    public int getCount() {
        return count;
    }

    public double getTimestamp() {
        return this.timestamp;
    }

    public Map<String, Object> getMetadata() {
        Gson gson = new Gson();
        try {
            return gson.fromJson(metadata, Map.class);
        } catch (JsonSyntaxException e) {
            System.out.println(String.format(
                    "WARNING: Failed to parse the metadata to object, returning an empty object. metadata: %s",
                    metadata));
            return new HashMap<>();
        }
    }   

    public String getRecommendationId() {
        return recommendationId;
    }
}

package org.zaikorea.ZaiClient.response;

import java.util.List;

import com.google.gson.annotations.SerializedName;

public class RecommendationResponse {
    
    @SerializedName("items")
    private List<String> items;

    @SerializedName("count")
    private int count;

    @SerializedName("timestamp")
    private double timestamp;

    public List<String> getItems() { return items; }

    public int getCount() { return count; }

    public double getTimestamp() { return this.timestamp; }
}

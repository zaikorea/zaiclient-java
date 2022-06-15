package org.zaikorea.ZaiClient.response;

import java.util.ArrayList;

import com.google.gson.annotations.SerializedName;

public class RecommendationResponse {
    
    @SerializedName("items")
    private ArrayList<String> items;

    @SerializedName("count")
    private int count;

    @SerializedName("timestamp")
    private double timestamp;

    public ArrayList<String> getItems() { return items; }

    public int getCount() { return count; }

    public double getTimestamp() { return this.timestamp; }
}

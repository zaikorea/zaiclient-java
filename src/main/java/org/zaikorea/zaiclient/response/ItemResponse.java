package org.zaikorea.zaiclient.response;

import java.util.List;

import org.zaikorea.zaiclient.request.items.Item;

import com.google.gson.annotations.SerializedName;

public class ItemResponse {

    @SerializedName("items")
    private List<Item> items;

    @SerializedName("count")
    private int count;

    @SerializedName("timestamp")
    private double timestamp;

    public List<Item> getItems() {
        return items;
    }

    public int getCount() {
        return count;
    }

    public double getTimestamp() {
        return this.timestamp;
    }
}

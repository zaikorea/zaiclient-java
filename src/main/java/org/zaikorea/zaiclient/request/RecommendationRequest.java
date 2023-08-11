package org.zaikorea.ZaiClient.request;

import java.util.List;

import com.google.gson.annotations.SerializedName;

public class RecommendationRequest {

    @SerializedName("user_id")
    protected String userId;

    @SerializedName("item_id")
    protected String itemId;

    @SerializedName("item_ids")
    protected List<String> itemIds;

    @SerializedName("limit")
    protected int limit;

    @SerializedName("recommendation_type")
    protected String recommendationType;

    @SerializedName("offset")
    protected int offset;

    @SerializedName("options")
    protected String options;

    public String getPath(String clientId) {
        return "";
    };

    public String getUserId() {
        return userId;
    }

    public String getItemId() {
        return itemId;
    }

    public List<String> getItemIds() {
        return itemIds;
    }

    public int getLimit() {
        return limit;
    }

    public int getOffset() {
        return offset;
    }

    public String getRecommendationType() {
        return recommendationType;
    }

    public String getOptions() {
        return options;
    }

}

package org.zaikorea.ZaiClient.request;

import com.google.gson.annotations.SerializedName;

public class RecommendationRequest {

    @SerializedName("user_id")
    protected String userId;

    @SerializedName("item_id")
    protected String itemId;

    @SerializedName("item_ids")
    protected String itemIds;

    @SerializedName("limit")
    protected int limit;
    
    @SerializedName("recommendation_type")
    protected String recommendationType;
    
    @SerializedName("offset")
    protected int offset;

    public String getPath(String clientId) { return ""; };

    public int getLimit() { return limit; }

    public int getOffset() { return offset; }

    public String getRecommendationType() { return recommendationType; }
    
}

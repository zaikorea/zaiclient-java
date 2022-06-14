package org.zaikorea.ZaiClient.request;
import org.zaikorea.ZaiClient.configs.Config;

import com.google.gson.annotations.SerializedName;

public class RecommendItemsToUser {

    private static final String defaultRecommendationType = "homepage";
    private static final String recommenderPath = "/user-recommendations";
    
    @SerializedName("user_id")
    protected String userId;

    @SerializedName("limit")
    protected int limit;
    
    @SerializedName("recommendation_type")
    protected String recommendationType;
    
    @SerializedName("offset")
    protected int offset;

    public RecommendItemsToUser(String userId, int limit, int offset) {
        this(userId, limit, offset, defaultRecommendationType);
    }

    public RecommendItemsToUser(String userId, int limit, int offset, String recommendationType) {
        this.userId = userId;
        this.limit = limit;
        this.recommendationType = defaultRecommendationType;
        this.offset = offset;
    }

    public String getPath(String clientId) {
        return String.format(Config.mlApiEndPoint + 
                             Config.mlApiPathPrefix + 
                             recommenderPath, clientId);
    }

    public String getUserId() {
        return userId;
    }

    public int getLimit() {
        return limit;
    }

    public String getRecommendationType() {
        return recommendationType;
    }

    public int getOffset() {
        return offset;
    }

}

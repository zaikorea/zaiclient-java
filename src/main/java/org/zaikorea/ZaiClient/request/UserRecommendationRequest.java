package org.zaikorea.ZaiClient.request;

import org.zaikorea.ZaiClient.configs.Config;

public class UserRecommendationRequest extends RecommendationRequest {

    private static final int defaultOffset = 0;
    private static final String defaultRecommendationType = "homepage";
    private static final String recommenderPath = "/user-recommendations";

    public UserRecommendationRequest(String userId, int limit) {
        this(userId, limit, defaultOffset, defaultRecommendationType);
    }

    public UserRecommendationRequest(String userId, int limit, String recommendationType) {
        this(userId, limit, defaultOffset, recommendationType);
    }

    public UserRecommendationRequest(String userId, int limit, int offset) {
        this(userId, limit, offset, defaultRecommendationType);
    }

    public UserRecommendationRequest(String userId, int limit, int offset, String recommendationType) {
        this.userId = userId;
        this.limit = limit;
        this.recommendationType = recommendationType;
        this.offset = offset;
    }

    @Override
    public String getPath(String clientId) {
        return String.format(Config.mlApiEndPoint +
                Config.mlApiPathPrefix +
                recommenderPath, clientId);
    }
}

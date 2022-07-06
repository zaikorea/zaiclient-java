package org.zaikorea.ZaiClient.request;

import java.util.List;
import org.zaikorea.ZaiClient.configs.Config;

public class RerankingRecommendationRequest extends RecommendationRequest {

    private static final int defaultOffset = 0;
    private static final String defaultRecommendationType = "category_page";
    private static final String recommenderPath = "/reranking";

    public RerankingRecommendationRequest(String userId, List<String> itemIds) {
        this(userId, itemIds, itemIds.size(), defaultOffset, defaultRecommendationType);
    }

    public RerankingRecommendationRequest(String userId, List<String> itemIds, int limit) {
        this(userId, itemIds, limit, defaultOffset, defaultRecommendationType);
    }

    public RerankingRecommendationRequest(String userId, List<String> itemIds, int limit, int offset) {
        this(userId, itemIds, limit, offset, defaultRecommendationType);
    }

    public RerankingRecommendationRequest(String userId, List<String> itemIds, int limit, String recommendationType) {
        this(userId, itemIds, limit, defaultOffset, recommendationType);
    }

    public RerankingRecommendationRequest(String userId, List<String> itemIds, String recommendationType) {
        this(userId, itemIds, itemIds.size(), defaultOffset, recommendationType);
    }

    public RerankingRecommendationRequest(String userId, List<String> itemIds, int limit, int offset,
            String recommendationType) {
        this.userId = userId;
        this.itemIds = itemIds;
        this.limit = limit;
        this.offset = offset;
        this.recommendationType = recommendationType;
    }

    @Override
    public String getPath(String clientId) {
        return String.format(Config.mlApiEndPoint +
                Config.mlApiPathPrefix +
                recommenderPath, clientId);
    }
}

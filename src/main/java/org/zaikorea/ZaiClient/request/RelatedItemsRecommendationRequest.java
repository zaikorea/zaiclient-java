package org.zaikorea.ZaiClient.request;

import org.zaikorea.ZaiClient.configs.Config;

public class RelatedItemsRecommendationRequest extends RecommendationRequest {

    private static final int defaultOffset = 0;
    private static final String defaultRecommendationType = "product_detail_page";
    private static final String recommenderPath = "/related-items";

    public RelatedItemsRecommendationRequest(String itemId, int limit) {
        this(itemId, limit, defaultOffset, defaultRecommendationType);
    }

    public RelatedItemsRecommendationRequest(String itemId, int limit, String recommendationType) {
        this(itemId, limit, defaultOffset, recommendationType);
    }

    public RelatedItemsRecommendationRequest(String itemId, int limit, int offset) {
        this(itemId, limit, offset, defaultRecommendationType);
    }

    public RelatedItemsRecommendationRequest(String itemId, int limit, int offset, String recommendationType) {
        this.itemId = itemId;
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

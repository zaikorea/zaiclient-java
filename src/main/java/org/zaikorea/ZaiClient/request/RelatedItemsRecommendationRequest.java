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
        if (itemId == null || !(0 < itemId.length() && itemId.length() <= 100)) {
            throw new IllegalArgumentException("Length of item id must be between 1 and 100.");
        }
        if (!(0 < limit && limit <= 1_000_000)) {
            throw new IllegalArgumentException("Limit must be between 1 and 1000,000.");
        }
        if (!(0 <= offset && offset <= 1_000_000)) {
            throw new IllegalArgumentException("Offset must be between 0 and 1000,000.");
        }
        if (!(0 < recommendationType.length() && recommendationType.length() <= 100)) {
            throw new IllegalArgumentException("Length of recommendation type must be between 1 and 100.");
        }
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

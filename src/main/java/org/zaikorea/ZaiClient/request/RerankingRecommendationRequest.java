package org.zaikorea.ZaiClient.request;

import org.zaikorea.ZaiClient.configs.Config;

import java.util.HashMap;
import java.util.Map;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class RerankingRecommendationRequest extends RecommendationRequest {

    private static final int defaultOffset = 0;
    private static final String defaultRecommendationType = "category_page";
    private static final Map<String, String> defaultOptions = new HashMap<>();

    public RerankingRecommendationRequest(String userId, List<String> itemIds) {
        this(userId, itemIds, itemIds.size(), defaultOffset, defaultRecommendationType);
    }

    public RerankingRecommendationRequest(String userId, List<String> itemIds, int limit) {
        this(userId, itemIds, limit, defaultOffset, defaultRecommendationType, defaultOptions);
    }

    public RerankingRecommendationRequest(String userId, List<String> itemIds, int limit, int offset) {
        this(userId, itemIds, limit, offset, defaultRecommendationType, defaultOptions);
    }

    public RerankingRecommendationRequest(String userId, List<String> itemIds, int limit, String recommendationType) {
        this(userId, itemIds, limit, defaultOffset, recommendationType, defaultOptions);
    }

    public RerankingRecommendationRequest(String userId, List<String> itemIds, String recommendationType) {
        this(userId, itemIds, itemIds.size(), defaultOffset, recommendationType, defaultOptions);
    }

    public RerankingRecommendationRequest(String userId, List<String> itemIds, int limit, int offset, String recommendationType) {
        this(userId, itemIds, limit, offset, recommendationType, defaultOptions);
    }

    public RerankingRecommendationRequest(
            String userId,
            List<String> itemIds,
            int limit,
            int offset,
            String recommendationType,
            Map options
    ) {
        if (!(0 < itemIds.size() && itemIds.size() <= 1_000_000)) {
            throw new IllegalArgumentException("Length of item_ids must be between 1 and 1000,000.");
        }
        itemIds.forEach(id -> {
            if (id == null || !(0 < id.length() && id.length() <= 100)) {
                throw new IllegalArgumentException("Length of item id in item id list must be between 1 and 100.");
            }
        });
        if (!(userId == null || (0 < userId.length() && userId.length() <= 100))) {
            throw new IllegalArgumentException("Length of user id must be between 1 and 100.");
        }
        if (!(0 < limit && limit <= 1_000_000)) {
            throw new IllegalArgumentException("Limit must be between 1 and 1000,000.");
        }
        if (!(0 <= offset && offset <= 1_000_000)) {
            throw new IllegalArgumentException("Offset must be between 0 and 1000,000.");
        }
        if (recommendationType == null || !(0 < recommendationType.length() && recommendationType.length() <= 100)) {
            throw new IllegalArgumentException("Length of recommendation type must be between 1 and 100.");
        }

        String jsonString;
        ObjectMapper mapper = new ObjectMapper();
        try {
            jsonString = mapper.writeValueAsString(options);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        if (jsonString.length() > 1000) {
            throw new IllegalArgumentException("Total length of options must be less than 1000");
        }

        this.userId = userId;
        this.itemIds = itemIds;
        this.limit = limit;
        this.offset = offset;
        this.recommendationType = recommendationType;
        this.options = jsonString;
    }

    @Override
    public String getPath(String clientId) {
        return String.format(Config.mlApiEndPoint +
                Config.mlApiPathPrefix +
                Config.rerankingRecommendationPathPrefix, clientId);
    }
}

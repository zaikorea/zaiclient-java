package org.zaikorea.ZaiClient.request;

import org.zaikorea.ZaiClient.configs.Config;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class UserRecommendationRequest extends RecommendationRequest {

    private static final int defaultOffset = 0;
    private static final String defaultRecommendationType = "homepage";

    private static final Map<String, String> defaultOptions = new HashMap<>();

    public UserRecommendationRequest(String userId, int limit) {
        this(userId, limit, defaultOffset, defaultRecommendationType, defaultOptions);
    }

    public UserRecommendationRequest(String userId, int limit, String recommendationType) {
        this(userId, limit, defaultOffset, recommendationType, defaultOptions);
    }

    public UserRecommendationRequest(String userId, int limit, int offset) {
        this(userId, limit, offset, defaultRecommendationType, defaultOptions);
    }

    public UserRecommendationRequest(String userId, int limit, int offset, String recommendationType) {
        this(userId, limit, offset, recommendationType, defaultOptions);
    }

    public UserRecommendationRequest(String userId, int limit, int offset, String recommendationType, Map options) {
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
        this.limit = limit;
        this.recommendationType = recommendationType;
        this.offset = offset;
        this.options = jsonString;
    }

    @Override
    public String getPath(String clientId) {
        return String.format(Config.mlApiEndPoint +
                Config.mlApiPathPrefix +
                Config.userRecommendationPathPrefix, clientId);
    }
}

package org.zaikorea.zaiclient.request;

import org.zaikorea.zaiclient.configs.Config;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class UserRecommendationRequest extends RecommendationRequest {

    private static final int defaultOffset = 0;
    private static final String defaultRecommendationType = "homepage";

    private static final String defaultOptions = null;

    private UserRecommendationRequest(Builder builder) {
        this.userId = builder.userId;
        this.limit = builder.limit;
        this.recommendationType = builder.recommendationType;
        this.offset = builder.offset;
        this.options = builder.options;
    }

    @Override
    public String getPath(String clientId) {
        return String.format(Config.mlApiPathPrefix +
                Config.userRecommendationPathPrefix, clientId);
    }

    public static class Builder {
        private final String userId;
        private final int limit;
        private int offset = defaultOffset;
        private String recommendationType = defaultRecommendationType;
        private String options = defaultOptions;

        public Builder(String userId, int limit) {
            if (!(userId == null || (0 < userId.length() && userId.length() <= 500))) {
                throw new IllegalArgumentException("Length of user id must be between 1 and 500.");
            }
            if (!(0 <= limit && limit <= 10_000)) {
                throw new IllegalArgumentException("Limit must be between 0 and 10,000.");
            }
            this.userId = userId;
            this.limit = limit;
        }

        public Builder offset(int offset) {
            if (!(0 <= offset && offset <= 10_000)) {
                throw new IllegalArgumentException("Offset must be between 0 and 10,000.");
            }
            this.offset = offset;

            return this;
        }

        public Builder recommendationType(String recommendationType) {
            if (recommendationType == null || !(0 < recommendationType.length() && recommendationType.length() <= 500)) {
                throw new IllegalArgumentException("Length of recommendation type must be between 1 and 500.");
            }
            this.recommendationType = recommendationType;

            return this;
        }

        public Builder options(Map options) {
            String jsonString;
            ObjectMapper mapper = new ObjectMapper();
            try {
                jsonString = mapper.writeValueAsString(options);
            } catch (JsonProcessingException e) {
                jsonString = "Error";
            }

            if (jsonString.length() > 1000) {
                throw new IllegalArgumentException("Length of options must be less than or equal to 1000 when converted to string.");
            }
            this.options = jsonString;

            return this;
        }

        public UserRecommendationRequest build() {
            return new UserRecommendationRequest(this);
        }
    }
}
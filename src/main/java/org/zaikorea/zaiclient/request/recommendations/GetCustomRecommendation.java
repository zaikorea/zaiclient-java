package org.zaikorea.zaiclient.request.recommendations;

import java.util.List;
import java.util.Map;

import org.zaikorea.zaiclient.configs.Config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class GetCustomRecommendation extends RecommendationRequest {

    private static final int DEFAULT_OFFSET = 0;
    private static final String DEFAULT_OPTIONS = null;
    private static final Integer DEFAULT_LIMIT = null;
    private static final String DEFAULT_USER_ID = null;
    private static final String DEFAULT_ITEM_ID = null;
    private static final List<String> DEFAULT_ITEM_IDS = null;

    public GetCustomRecommendation(Builder builder) {
        // Additional validation
        if (builder.userId == null && builder.itemId == null && builder.itemIds == null)
            throw new IllegalArgumentException("At least one of userId, itemId, or itemIds must be provided.");

        this.recQuery = new RecommendationQuery()
                .setUserId(builder.userId)
                .setItemId(builder.itemId)
                .setItemIds(builder.itemIds)
                .setRecommendationType(builder.recommendationType)
                .setLimit(builder.limit)
                .setOffset(builder.offset)
                .setOptions(builder.options);
    }

    @Override
    public String getPath(String clientId) {
        return String.format(Config.mlApiPathPrefix, clientId) + Config.customRecommendationPath;
    }

    public static class Builder {
        private final String recommendationType;
        private String userId = DEFAULT_USER_ID;
        private String itemId = DEFAULT_ITEM_ID;
        private List<String> itemIds = DEFAULT_ITEM_IDS;
        private int offset = DEFAULT_OFFSET;
        private Integer limit = DEFAULT_LIMIT;
        private String options = DEFAULT_OPTIONS;


        public Builder(String recommendationType) {
            this.recommendationType = recommendationType;
        }

        public Builder userId(String userId) {
            this.userId = userId;

            return this;
        }

        public Builder itemId(String itemId) {
            this.itemId = itemId;

            return this;
        }

        public Builder itemIds(List<String> itemIds) {
            this.itemIds = itemIds;

            return this;
        }

        public Builder offset(int offset) {
            this.offset = offset;

            return this;
        }

        public Builder limit(Integer limit) {
            this.limit = limit;

            return this;
        }

        public Builder options(Map<String, ?> options) {
            String jsonString;
            ObjectMapper mapper = new ObjectMapper();

            try {
                jsonString = mapper.writeValueAsString(options);
            } catch (JsonProcessingException e) {
                jsonString = "Error";
            }
            this.options = jsonString;

            return this;
        }

        public GetCustomRecommendation build() {
            return new GetCustomRecommendation(this);
        }
    }

}

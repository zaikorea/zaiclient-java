package org.zaikorea.zaiclient.request.recommendations;

import java.util.Map;

import org.zaikorea.zaiclient.configs.Config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class GetRelatedRecommendation extends RecommendationRequest {
    private static final String DEFAULT_REC_TYPE = "product_detail_page";
    private static final int DEFAULT_OFFSET = 0;
    private static final String DEFAULT_OPTIONS = null;

    public GetRelatedRecommendation(Builder builder) {
        this.recQuery = new RecommendationQuery()
            .setUserId(builder.userId)
            .setItemId(builder.itemId)
            .setOffset(builder.offset)
            .setLimit(builder.limit)
            .setRecommendationType(builder.recommendationType)
            .setOptions(builder.options);
    }

    @Override
    public String getPath(String clientId) {
        return String.format(Config.mlApiPathPrefix, clientId) + Config.relatedItemsRecommendationPath;
    }

    public static class Builder {
        private final String itemId;
        private final int limit;
        private final String userId;
        private int offset = DEFAULT_OFFSET;
        private String recommendationType = DEFAULT_REC_TYPE;
        private String options = DEFAULT_OPTIONS;

        public Builder(String itemId, String targetUserId, int limit) {
            this.itemId = itemId;
            this.userId = targetUserId;
            this.limit = limit;
        }

        public Builder offset(int offset) {
            this.offset = offset;

            return this;
        }

        public Builder recommendationType(String recommendationType) {
            this.recommendationType = recommendationType;

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

        public GetRelatedRecommendation build() {
            return new GetRelatedRecommendation(this);
        }
    }
}

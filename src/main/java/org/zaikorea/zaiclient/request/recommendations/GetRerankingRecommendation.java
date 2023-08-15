package org.zaikorea.zaiclient.request.recommendations;

import java.util.List;
import java.util.Map;

import org.zaikorea.zaiclient.configs.Config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class GetRerankingRecommendation extends RecommendationRequest {
    private static final String DEFAULT_REC_TYPE = "category_page";
    private static final int DEFAULT_OFFSET = 0;
    private static final String DEFAULT_OPTIONS = null;

    public GetRerankingRecommendation(Builder builder) {
        this.recQuery = new RecommendationQuery()
            .setUserId(builder.userId)
            .setItemIds(builder.itemIds)
            .setOffset(builder.offset)
            .setLimit(builder.limit)
            .setOptions(builder.options)
            .setRecommendationType(builder.recommendationType)
            .setOptions(builder.options);
    }

    @Override
    public String getPath(String clientId) {
        return String.format(Config.mlApiPathPrefix, clientId) + Config.rerankingRecommendationPath;
    }

    public static class Builder {
        private final String userId;
        private final List<String> itemIds;
        private int offset = DEFAULT_OFFSET;
        private int limit;
        private String recommendationType = DEFAULT_REC_TYPE;
        private String options = DEFAULT_OPTIONS;


        public Builder(String userId, List<String> itemIds) {
            this.userId = userId;
            this.itemIds = itemIds;
            this.limit = itemIds.size();
        }

        public Builder offset(int offset) {
            this.offset = offset;

            return this;
        }

        public Builder limit(int limit) {
            this.limit = limit;

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

        public GetRerankingRecommendation build() {
            return new GetRerankingRecommendation(this);
        }
    }

}

package org.zaikorea.zaiclient.request.recommendations;

import java.util.Map;

import org.zaikorea.zaiclient.configs.Config;
import org.zaikorea.zaiclient.utils.Validator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class GetUserRecommendation extends RecommendationRequest {
    private static final int DEFAULT_OFFSET = 0;
    private static final String DEFUALT_REC_TYPE = "homepage";
    private static final String DEFAULT_OPTIONS = null;

    public GetUserRecommendation(Builder builder) {
        this.recQuery = new RecommendationQuery()
            .setUserId(builder.userId)
            .setLimit(builder.limit)
            .setOffset(builder.offset)
            .setRecommendationType(builder.recommendationType)
            .setOptions(builder.options);
    }

    @Override
    public String getPath(String clientId) {
        return String.format(Config.mlApiPathPrefix, clientId) + Config.userRecommendationPath;
    }

    public static class Builder {
        private final String userId;
        private final int limit;
        private int offset = DEFAULT_OFFSET;
        private String recommendationType = DEFUALT_REC_TYPE;
        private String options = DEFAULT_OPTIONS;

        public Builder(String userId, int limit) {
            this.userId = Validator.validateString(userId, 1, 500, true, "userId");
            this.limit = Validator.validateNumber(limit, 0, 10000, false, "limit");
        }

        public Builder offset(int offset) {
            this.offset = Validator.validateNumber(offset, 0, 10000, false, "offset");

            return this;
        }

        public Builder recommendationType(String recommendationType) {
            this.recommendationType = Validator.validateString(recommendationType, 1, 500, false, "recommendationType");

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

        public GetUserRecommendation build() {
            return new GetUserRecommendation(this);
        }
    }
}

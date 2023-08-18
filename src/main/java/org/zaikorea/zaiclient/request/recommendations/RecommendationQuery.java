package org.zaikorea.zaiclient.request.recommendations;

import java.util.List;

import org.zaikorea.zaiclient.utils.Validator;

import com.google.gson.annotations.SerializedName;

public class RecommendationQuery {

    @SerializedName("user_id")
    protected String userId;

    @SerializedName("item_id")
    protected String itemId;

    @SerializedName("item_ids")
    protected List<String> itemIds;

    @SerializedName("limit")
    protected int limit;

    @SerializedName("recommendation_type")
    protected String recommendationType;

    @SerializedName("offset")
    protected int offset;

    @SerializedName("options")
    protected String options;


    public String getUserId() {
        return userId;
    }

    public String getItemId() {
        return itemId;
    }

    public List<String> getItemIds() {
        return itemIds;
    }

    public int getLimit() {
        return limit;
    }

    public int getOffset() {
        return offset;
    }

    public String getRecommendationType() {
        return recommendationType;
    }

    public String getOptions() {
        return options;
    }

    public RecommendationQuery setUserId(String userId) {
        this.userId = Validator.validateString(userId, 1, 500, true, "userId");
        return this;
    }

    public RecommendationQuery setItemId(String itemId) {
        this.itemId = Validator.validateString(itemId, 1, 500, true, "itemId");
        return this;
    }

    public RecommendationQuery setItemIds(List<String> itemIds) {
        this.itemIds = Validator.validateStringList(itemIds, 0, 10000, true, "itemIds");
        return this;
    }

    public RecommendationQuery setLimit(int limit) {
        this.limit = Validator.validateNumber(limit, 0, 10000, false, "limit");
        return this;
    }

    public RecommendationQuery setOffset(int offset) {
        this.offset = Validator.validateNumber(offset, 0, 10000, false, "offset");
        return this;
    }

    public RecommendationQuery setRecommendationType(String recommendationType) {
        this.recommendationType = Validator.validateString(recommendationType, 1, 500, false, "recommendationType");
        return this;
    }

    public RecommendationQuery setOptions(String options) {
        this.options = Validator.validateString(options, 0, 1000, true, "options");
        return this;
    }
}

package org.zaikorea.zaiclienttest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.zaikorea.zaiclient.ZaiClient;
import org.zaikorea.zaiclient.exceptions.ZaiClientException;
import org.zaikorea.zaiclient.request.recommendations.GetUserRecommendation;
import org.zaikorea.zaiclient.request.recommendations.RecommendationQuery;
import org.zaikorea.zaiclient.request.recommendations.RecommendationRequest;
import org.zaikorea.zaiclient.response.RecommendationResponse;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;

public class ZaiClientGetUserRecommendationTest {
    class Metadata {

        @SerializedName("user_id")
        public String userId;
        @SerializedName("item_id")
        public String itemId;
        @SerializedName("item_ids")
        public List<String> itemIds;
        @SerializedName("limit")
        public Integer limit;
        @SerializedName("offset")
        public Integer offset;
        @SerializedName("options")
        public Map<String, Integer> options;
        @SerializedName("call_type")
        public String callType;
        @SerializedName("recommendation_type")
        public String recommendationType;

        public Metadata() {
            this.offset = 0;
            this.options = new HashMap<>();
            this.callType = "user-recommendations";
            this.recommendationType = "homepage";
        }

        @Override
        public String toString() {
            return "Metadata{" +
                    "userId='" + userId + '\'' +
                    ", itemId='" + itemId + '\'' +
                    ", itemIds=" + itemIds +
                    ", limit=" + limit +
                    ", offset=" + offset +
                    ", options=" + options +
                    ", callType='" + callType + '\'' +
                    ", recommendationType='" + recommendationType + '\'' +
                    '}';
        }

        @Override
        public boolean equals(Object obj) {

            try {
                for (Field field : obj.getClass().getFields()) {
                    if (field.get(obj) != null && field.get(obj).equals(field.get(this)))
                        continue;
                    else {
                        if (field.get(obj) == null && field.get(this) == null)
                            continue;
                        else
                            return false;
                    }
                }
            } catch (IllegalArgumentException | IllegalAccessException e) {
                System.out.println(e.getMessage());

                return false;
            }

            return true;
        }
    }

    private static final String clientId = "test";
    private static final String clientSecret = "KVPzvdHTPWnt0xaEGc2ix-eqPXFCdEV5zcqolBr_h1k"; // this secret key is for
                                                                                              // testing purposes only
    private static final String recLogTableName = "rec_log_test";
    private static final String recLogTablePartitionKey = "user_id";
    private static final String recLogTableSortKey = "timestamp";
    private static final String recLogRecommendationTypeKey = "recommendation_type";
    private static final String recLogOffsetKey = "offset";
    private static final String recLogItemIdKey = "item_id";
    private static final String recLogRecommendationIdKey = "recommendation_id";
    private static final String recLogRecommendationsKey = "recommendations";
    private static final String recLogUserIdKey = "user_id";
    private static final String recLogLimitKey = "limit";
    private static final String recLogItemIdsKey = "item_ids";

    private ZaiClient testClient;

    private static final Region region = Region.AP_NORTHEAST_2;
    private DynamoDbClient ddbClient;

    private String generateUUID() {
        return UUID.randomUUID().toString();
    }

    private int generateRandomInteger(int min, int max) {
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    private Map<String, String> getRecLog(String partitionValue) {

        String partitionAlias = "#pk";

        HashMap<String, String> attrNameAlias = new HashMap<>();
        attrNameAlias.put(partitionAlias, recLogTablePartitionKey);
        HashMap<String, AttributeValue> attrValues = new HashMap<>();
        attrValues.put(":" + recLogTablePartitionKey, AttributeValue.builder()
                .s(partitionValue)
                .build());

        QueryRequest request = QueryRequest.builder()
                .tableName(recLogTableName)
                .keyConditionExpression(partitionAlias + " = :" + recLogTablePartitionKey)
                .expressionAttributeNames(attrNameAlias)
                .expressionAttributeValues(attrValues)
                .build();

        try {
            List<Map<String, AttributeValue>> returnedItems = ddbClient.query(request).items();
            if (returnedItems.size() > 1)
                return null;
            if (returnedItems.size() == 0)
                return new HashMap<>();
            Map<String, AttributeValue> returnedItem = returnedItems.get(0);
            Map<String, String> item = new HashMap<>();
            if (returnedItem != null) {
                for (String key : returnedItem.keySet()) {
                    // Convert AttributeValue to Java Object
                    AttributeValue attributeValue = returnedItem.get(key);
                    String val = Objects.toString(TestUtils.toJavaObject(attributeValue));
                    item.put(key, val);
                }
                return item;
            }
            return null;
        } catch (DynamoDbException e) {
            e.printStackTrace();
            System.err.println(e.getMessage());
            return null;
        }
    }

    private boolean deleteRecLog(String partitionValue) {

        String sortValue = getRecLog(partitionValue).get(recLogTableSortKey);

        HashMap<String, AttributeValue> keyToGet = new HashMap<>();

        keyToGet.put(recLogTablePartitionKey, AttributeValue.builder().s(partitionValue).build());
        keyToGet.put(recLogTableSortKey, AttributeValue.builder().n(sortValue).build());

        DeleteItemRequest deleteReq = DeleteItemRequest.builder()
                .key(keyToGet)
                .tableName(recLogTableName)
                .build();

        try {
            ddbClient.deleteItem(deleteReq);
        } catch (DynamoDbException e) {
            System.err.println(e.getMessage());
            return false;
        }

        return true;
    }

    private void checkSuccessfulGetUserRecommendation(RecommendationRequest recommendation, Metadata expectedMetadata) {
        RecommendationQuery recQuery = recommendation.getPayload();

        String recommendationType = recQuery.getRecommendationType();
        int offset = recQuery.getOffset();
        String userId = recQuery.getUserId();
        String itemId = recQuery.getItemId();
        int limit = recQuery.getLimit();
        List<String> itemIds = recQuery.getItemIds();
        Gson gson = new Gson();

        try {
            RecommendationResponse response = testClient.sendRequest(recommendation);

            // Response Testing
            List<String> responseItems = response.getItems();
            String recommendationId = response.getRecommendationId();
            for (int i = 0; i < limit; i++) {
                String expectedItem = String.format("ITEM_ID_%d", i + offset);
                assertEquals(expectedItem, responseItems.get(i));
            }

            // Metadata Testing
            Metadata metadata = gson.fromJson(response.getMetadata(), Metadata.class);
            assertEquals(expectedMetadata, metadata);
            assertEquals(response.getItems().size(), limit);
            assertEquals(response.getCount(), limit);

            // Log testing unavailable when userId is null
            if (userId == null)
                return;

            // Check log
            Map<String, String> logItem = getRecLog(userId);
            assertNotNull(logItem);
            assertNotEquals(0, logItem.size());
            assertEquals(Objects.toString(recommendationType), logItem.get(recLogRecommendationTypeKey));
            assertEquals(Objects.toString(offset), logItem.get(recLogOffsetKey));
            assertEquals(Objects.toString(itemId), logItem.get(recLogItemIdKey));
            assertEquals(Objects.toString(recommendationId), logItem.get(recLogRecommendationIdKey));
            assertEquals(Objects.toString(responseItems), logItem.get(recLogRecommendationsKey));
            assertEquals(Objects.toString(userId), logItem.get(recLogUserIdKey));
            assertEquals(Objects.toString(limit), logItem.get(recLogLimitKey));
            assertEquals(Objects.toString(itemIds), logItem.get(recLogItemIdsKey));
            assertTrue(deleteRecLog(userId));

        } catch (IOException | ZaiClientException e) {
            fail();
        }
    }

    @Before
    public void setup() {
        testClient = new ZaiClient.Builder(clientId, clientSecret)
                .connectTimeout(20)
                .readTimeout(40)
                .build();
        ddbClient = DynamoDbClient.builder()
                .region(region)
                .build();
    }

    @After
    public void cleanup() {
        ddbClient.close();
    }

    @Test
    public void testGetUserRecommendation_1() {
        Metadata metadata;
        String userId = generateUUID();
        int limit = generateRandomInteger(1, 10);
        int offset = generateRandomInteger(20, 40);
        String recommendationType = "home_page";

        RecommendationRequest recommendation = new GetUserRecommendation.Builder(userId, limit)
                .offset(offset)
                .recommendationType(recommendationType)
                .build();

        try {
            metadata = new Metadata();
            metadata.userId = userId;
            metadata.limit = limit;
            metadata.offset = offset;
            metadata.recommendationType = recommendationType;
            checkSuccessfulGetUserRecommendation(recommendation, metadata);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            fail();
        }
    }

    @Test
    public void testGetUserRecommendation_3() { // With null user (unregistered user)
        Metadata metadata;
        String userId = null;
        int limit = generateRandomInteger(1, 10);
        int offset = generateRandomInteger(20, 40);

        RecommendationRequest recommendation = new GetUserRecommendation.Builder(userId, limit)
                .offset(offset)
                .build();

        try {
            metadata = new Metadata();
            metadata.userId = userId;
            metadata.limit = limit;
            metadata.offset = offset;
            checkSuccessfulGetUserRecommendation(recommendation, metadata);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            fail();
        }
    }

    @Test
    public void testGetUserRecommendation_2() {
        Metadata metadata;
        String userId = generateUUID();
        int limit = generateRandomInteger(1, 10);
        int offset = generateRandomInteger(20, 40);

        RecommendationRequest recommendation = new GetUserRecommendation.Builder(userId, limit)
                .offset(offset)
                .build();

        try {
            metadata = new Metadata();
            metadata.userId = userId;
            metadata.limit = limit;
            metadata.offset = offset;
            checkSuccessfulGetUserRecommendation(recommendation, metadata);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            fail();
        }
    }

}

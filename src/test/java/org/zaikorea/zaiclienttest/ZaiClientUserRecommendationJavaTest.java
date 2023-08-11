package org.zaikorea.zaiclienttest;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.Assert.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.zaikorea.zaiclient.ZaiClient;
import org.zaikorea.zaiclient.exceptions.ZaiClientException;
import org.zaikorea.zaiclient.request.*;
import org.zaikorea.zaiclient.response.RecommendationResponse;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.model.*;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

public class ZaiClientUserRecommendationJavaTest {

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
                for (Field field: obj.getClass().getFields()) {
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
    private static final String recLogRecommendations = "recommendations";

    private static final String userIdExceptionMessage = "Length of user id must be between 1 and 500.";
    private static final String itemIdExceptionMessage = "Length of item id must be between 1 and 500.";
    private static final String recommendationTypeExceptionMessage = "Length of recommendation type must be between 1 and 500.";
    private static final String limitExceptionMessage = "Limit must be between 0 and 10,000.";
    private static final String offsetExceptionMessage = "Offset must be between 0 and 10,000.";
    private static final String optionsExceptionMessage = "Length of options must be less than or equal to 1000 when converted to string.";

    private ZaiClient testClient;
    private ZaiClient incorrectIdClient;
    private ZaiClient incorrectSecretClient;

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
                    String val = returnedItem.get(key).toString();
                    item.put(key, val.substring(17, val.length() - 1));
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

    private void checkSuccessfulGetUserRecommendation(RecommendationRequest recommendation, String userId, Metadata expectedMetadata) {
        int limit = recommendation.getLimit();
        int offset = recommendation.getOffset();
        Gson gson = new Gson();

        try {
            RecommendationResponse response = testClient.getRecommendations(recommendation);

            // Response Testing
            List<String> responseItems = response.getItems();
            for (int i = 0; i < recommendation.getLimit(); i++) {
                String expectedItem = String.format("ITEM_ID_%d", i+offset);
                assertEquals(expectedItem, responseItems.get(i));
            }

            // Metadata Testing
            Metadata metadata = gson.fromJson(response.getMetadata(), Metadata.class);
            assertEquals(expectedMetadata, metadata);
            assertEquals(response.getItems().size(), limit);
            assertEquals(response.getCount(), limit);

            // Log testing unavailable when userId is null
            if (userId == null)
                return ;

            // Check log
            Map<String, String> logItem = getRecLog(userId);
            assertNotNull(logItem);
            assertNotEquals(logItem.size(), 0);
            assertEquals(logItem.get(
                            recLogRecommendations).split(",").length,
                    response.getItems().size()
            );
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
        incorrectIdClient = new ZaiClient.Builder("." + clientId, clientSecret)
                .connectTimeout(0)
                .readTimeout(0)
                .build();
        incorrectSecretClient = new ZaiClient.Builder(clientId, "." + clientSecret)
                .connectTimeout(-1)
                .readTimeout(-1)
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

        RecommendationRequest recommendation = new UserRecommendationRequest.Builder(userId, limit)
                .offset(offset)
                .recommendationType(recommendationType)
                .build();

        try {
            metadata = new Metadata();
            metadata.userId = userId;
            metadata.limit = limit;
            metadata.offset = offset;
            metadata.recommendationType = recommendationType;
            checkSuccessfulGetUserRecommendation(recommendation, userId, metadata);
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

        RecommendationRequest recommendation = new UserRecommendationRequest.Builder(userId, limit)
                .offset(offset)
                .build();

        try {
            metadata = new Metadata();
            metadata.userId = userId;
            metadata.limit = limit;
            metadata.offset = offset;
            checkSuccessfulGetUserRecommendation(recommendation, userId, metadata);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            fail();
        }
    }

    @Test
    public void testGetUserRecommendation_3() {
        Metadata metadata;
        String userId = generateUUID();
        int limit = generateRandomInteger(1, 10);

        RecommendationRequest recommendation = new UserRecommendationRequest.Builder(userId, limit)
                .build();

        try {
            metadata = new Metadata();
            metadata.userId = userId;
            metadata.limit = limit;
            checkSuccessfulGetUserRecommendation(recommendation, userId, metadata);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            fail();
        }
    }

    @Test
    public void testGetUserRecommendation_4() {
        Metadata metadata;
        String userId = generateUUID();
        int limit = generateRandomInteger(1, 10);
        String recommendationType = "home_page";

        RecommendationRequest recommendation = new UserRecommendationRequest.Builder(userId, limit)
                .recommendationType(recommendationType)
                .build();

        try {
            metadata = new Metadata();
            metadata.userId = userId;
            metadata.limit = limit;
            metadata.recommendationType = recommendationType;
            checkSuccessfulGetUserRecommendation(recommendation, userId, metadata);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            fail();
        }
    }

    @Test
    public void testGetUserRecommendation_5() {
        Metadata metadata;
        String userId = generateUUID();
        int limit = generateRandomInteger(1, 10);
        int offset = generateRandomInteger(20, 40);
        String recommendationType = "home_page";

        Map<String, Integer> map = new HashMap<>();
        map.put("call_type", 1);
        map.put("response_type", 2);

        RecommendationRequest recommendation = new UserRecommendationRequest.Builder(userId, limit)
                .offset(offset)
                .recommendationType(recommendationType)
                .options(map)
                .build();

        try {
            metadata = new Metadata();
            metadata.userId = userId;
            metadata.limit = limit;
            metadata.offset = offset;
            metadata.options = map;
            metadata.recommendationType = recommendationType;
            checkSuccessfulGetUserRecommendation(recommendation, userId, metadata);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            fail();
        }
    }

    @Test
    public void testGetNullUserRecommendation_1() {
        Metadata metadata;
        String userId = null;
        int limit = generateRandomInteger(1, 10);
        int offset = generateRandomInteger(20, 40);
        String recommendationType = "home_page";

        RecommendationRequest recommendation = new UserRecommendationRequest.Builder(userId, limit)
                .offset(offset)
                .recommendationType(recommendationType)
                .build();

        try {
            metadata = new Metadata();
            metadata.userId = userId;
            metadata.limit = limit;
            metadata.offset = offset;
            metadata.recommendationType = recommendationType;
            checkSuccessfulGetUserRecommendation(recommendation, userId, metadata);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            fail();
        }
    }

    @Test
    public void testGetNullUserRecommendation_2() {
        Metadata metadata;
        String userId = null;
        int limit = generateRandomInteger(1, 10);
        int offset = generateRandomInteger(20, 40);

        RecommendationRequest recommendation = new UserRecommendationRequest.Builder(userId, limit)
                .offset(offset)
                .build();

        try {
            metadata = new Metadata();
            metadata.userId = userId;
            metadata.limit = limit;
            metadata.offset = offset;
            checkSuccessfulGetUserRecommendation(recommendation, userId, metadata);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            fail();
        }
    }

    @Test
    public void testGetNullUserRecommendation_3() {
        Metadata metadata;
        String userId = null;
        int limit = generateRandomInteger(1, 10);

        RecommendationRequest recommendation = new UserRecommendationRequest.Builder(userId, limit)
                .build();

        try {
            metadata = new Metadata();
            metadata.userId = userId;
            metadata.limit = limit;
            checkSuccessfulGetUserRecommendation(recommendation, userId, metadata);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            fail();
        }
    }

    @Test
    public void testGetNullUserRecommendation_4() {
        Metadata metadata;
        String userId = null;
        int limit = generateRandomInteger(1, 10);

        RecommendationRequest recommendation = new UserRecommendationRequest.Builder(userId, limit)
                .build();

        try {
            metadata = new Metadata();
            metadata.userId = userId;
            metadata.limit = limit;
            checkSuccessfulGetUserRecommendation(recommendation, userId, metadata);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            fail();
        }
    }

    @Test
    public void testGetNullUserRecommendation_5() {
        Metadata metadata;
        String userId = null;
        int limit = generateRandomInteger(1, 10);
        int offset = generateRandomInteger(20, 40);
        String recommendationType = "home_page";

        Map<String, Integer> map = new HashMap<>();
        map.put("call_type", 1);
        map.put("response_type", 2);

        RecommendationRequest recommendation = new UserRecommendationRequest.Builder(userId, limit)
                .offset(offset)
                .recommendationType(recommendationType)
                .options(map)
                .build();

        try {
            metadata = new Metadata();
            metadata.userId = userId;
            metadata.limit = limit;
            metadata.offset = offset;
            metadata.options = map;
            metadata.recommendationType = recommendationType;
            checkSuccessfulGetUserRecommendation(recommendation, userId, metadata);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            fail();
        }
    }

    @Test
    public void testGetUserRecommendationWrongClientId() {
        String userId = generateUUID();
        int limit = generateRandomInteger(1, 10);
        int offset = generateRandomInteger(20, 40);

        RecommendationRequest recommendation = new UserRecommendationRequest.Builder(userId, limit)
                .offset(offset)
                .build();
        try {
            incorrectIdClient.getRecommendations(recommendation);
        } catch (IOException e) {
            fail();
        } catch (ZaiClientException e) {
            assertEquals(401, e.getHttpStatusCode());
        }
    }

    @Test
    public void testGetUserRecommendationWrongSecret() {
        String userId = generateUUID();
        int limit = generateRandomInteger(1, 10);
        int offset = generateRandomInteger(20, 40);

        RecommendationRequest recommendation = new UserRecommendationRequest.Builder(userId, limit)
                .offset(offset)
                .build();
        try {
            incorrectSecretClient.getRecommendations(recommendation);
        } catch (IOException e) {
            fail();
        } catch (ZaiClientException e) {
            assertEquals(401, e.getHttpStatusCode());
        }
    }

    @Test
    public void testGetTooLongUserRecommendation() {
        String userId = String.join("a", Collections.nCopies(501, "a"));
        int limit = generateRandomInteger(1, 10);
        int offset = generateRandomInteger(20, 40);
        try {
            new UserRecommendationRequest.Builder(userId, limit)
                    .offset(offset)
                    .build();
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals(e.getMessage(), userIdExceptionMessage);
        } catch (Error e) {
            fail();
        }
    }

    @Test
    public void testGetTooBigLimitRecommendation() {
        String userId = generateUUID();
        int limit = 10_001;
        int offset = generateRandomInteger(20, 40);

        try {
            new UserRecommendationRequest.Builder(userId, limit)
                    .offset(offset)
                    .build();
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals(e.getMessage(), limitExceptionMessage);
        } catch (Error e) {
            fail();
        }
    }

    @Test
    public void testGetTooBigOffsetRecommendation() {
        String userId = generateUUID();
        int limit = generateRandomInteger(20, 40);
        int offset = 10_001;

        try {
            new UserRecommendationRequest.Builder(userId, limit)
                    .offset(offset)
                    .build();
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals(e.getMessage(), offsetExceptionMessage);
        } catch (Error e) {
            fail();
        }
    }

    @Test
    public void testGetTooLongTypeRecommendation() {
        String userId = generateUUID();
        String recommendationType = String.join("a", Collections.nCopies(501, "a"));
        int limit = generateRandomInteger(1, 10);
        int offset = generateRandomInteger(20, 40);
        try {
            new UserRecommendationRequest.Builder(userId, limit)
                    .offset(offset)
                    .recommendationType(recommendationType)
                    .build();
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals(e.getMessage(), recommendationTypeExceptionMessage);
        } catch (Error e) {
            fail();
        }
    }

    @Test
    public void testGetEmptyUserRecommendation() {
        String userId = "";
        int limit = generateRandomInteger(1, 10);
        int offset = generateRandomInteger(20, 40);
        try {
            new UserRecommendationRequest.Builder(userId, limit)
                    .offset(offset)
                    .build();
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals(e.getMessage(), userIdExceptionMessage);
        } catch (Error e) {
            fail();
        }
    }

    @Test
    public void testGetTooSmallLimitRecommendation() {
        String userId = generateUUID();
        int limit = -1;
        int offset = generateRandomInteger(20, 40);

        try {
            new UserRecommendationRequest.Builder(userId, limit)
                    .offset(offset)
                    .build();
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals(e.getMessage(), limitExceptionMessage);
        } catch (Error e) {
            fail();
        }
    }

    @Test
    public void testGetZeroOffsetRecommendation() {
        Metadata metadata;
        String userId = generateUUID();
        int limit = generateRandomInteger(20, 40);
        int offset = 0;

        RecommendationRequest recommendation = new UserRecommendationRequest.Builder(userId, limit)
                .offset(offset)
                .build();

        try {
            metadata = new Metadata();
            metadata.userId = userId;
            metadata.limit = limit;
            metadata.offset = offset;
            checkSuccessfulGetUserRecommendation(recommendation, userId, metadata);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            fail();
        }
    }

    @Test
    public void testGetTooSmallOffsetRecommendation() {
        String userId = generateUUID();
        int limit = generateRandomInteger(20, 40);
        int offset = -1;

        try {
            new UserRecommendationRequest.Builder(userId, limit)
                    .offset(offset)
                    .build();
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals(e.getMessage(), offsetExceptionMessage);
        } catch (Error e) {
            fail();
        }
    }

    @Test
    public void testGetEmptyTypeRecommendation() {
        String userId = generateUUID();
        String recommendationType = "";
        int limit = generateRandomInteger(1, 10);
        int offset = generateRandomInteger(20, 40);
        try {
            new UserRecommendationRequest.Builder(userId, limit)
                    .offset(offset)
                    .recommendationType(recommendationType)
                    .build();
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals(e.getMessage(), recommendationTypeExceptionMessage);
        } catch (Error e) {
            fail();
        }
    }

    @Test
    public void testGetTooLongOptionsUserRecommendation() {
        String userId = generateUUID();
        int limit = generateRandomInteger(1, 10);
        int offset = generateRandomInteger(20, 40);
        String recommendationType = "home_page";

        Map<String, Integer> map = new HashMap<>();
        map.put("call_type", 1);
        map.put("response_type", 2);
        map.put(
                String.join("a", Collections.nCopies(1000, "a")),
                3
        );

        try {
            new UserRecommendationRequest.Builder(userId, limit)
                    .offset(offset)
                    .recommendationType(recommendationType)
                    .options(map)
                    .build();
            fail();
        } catch(IllegalArgumentException e) {
            assertEquals(e.getMessage(), optionsExceptionMessage);
        } catch(Error e) {
            fail();
        }
    }

}

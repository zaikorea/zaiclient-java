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

public class ZaiClientRerankingRecommendationJavaTest {
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
            this.callType = "reranking";
            this.recommendationType = "category_page";
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
    private static final String itemIdsExceptionMessage = "Length of item_ids must be between 0 and 10,000.";
    private static final String itemIdInListExceptionMessage = "Length of item id in item id list must be between 1 and 500.";
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

    private void checkSuccessfulGetRerankingRecommendation(RecommendationRequest recommendation, String userId, Metadata expectedMetadata) {
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
    public void testGetRerankingRecommendation_1() {
        Metadata metadata;
        String userId = generateUUID();
        List<String> itemIds = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            itemIds.add(String.format("ITEM_ID_%d",i));
        }
        int limit = generateRandomInteger(1, 10);
        int offset = generateRandomInteger(20, 40);
        String recommendationType = "category_page";

        RecommendationRequest recommendation = new RerankingRecommendationRequest.Builder(userId, itemIds)
                .limit(limit)
                .offset(offset)
                .recommendationType(recommendationType)
                .build();

        try {
            metadata = new Metadata();
            metadata.userId = userId;
            metadata.itemIds = itemIds;
            metadata.limit = limit;
            metadata.offset = offset;
            metadata.recommendationType = recommendationType;
            checkSuccessfulGetRerankingRecommendation(recommendation, userId, metadata);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            fail();
        }
    }

    @Test
    public void testGetRerankingRecommendation_2() {
        Metadata metadata;
        String userId = generateUUID();
        List<String> itemIds = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            itemIds.add(String.format("ITEM_ID_%d",i));
        }
        int limit = generateRandomInteger(1, 10);
        int offset = generateRandomInteger(20, 40);

        RecommendationRequest recommendation = new RerankingRecommendationRequest.Builder(userId, itemIds)
                .limit(limit)
                .offset(offset)
                .build();

        try {
            metadata = new Metadata();
            metadata.userId = userId;
            metadata.itemIds = itemIds;
            metadata.limit = limit;
            metadata.offset = offset;
            checkSuccessfulGetRerankingRecommendation(recommendation, userId, metadata);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            fail();
        }
    }

    @Test
    public void testGetRerankingRecommendation_3() {
        Metadata metadata;
        String userId = generateUUID();
        List<String> itemIds = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            itemIds.add(String.format("ITEM_ID_%d",i));
        }
        int limit = generateRandomInteger(1, 10);

        RecommendationRequest recommendation = new RerankingRecommendationRequest.Builder(userId, itemIds)
                .limit(limit)
                .build();

        try {
            metadata = new Metadata();
            metadata.userId = userId;
            metadata.itemIds = itemIds;
            metadata.limit = limit;
            checkSuccessfulGetRerankingRecommendation(recommendation, userId, metadata);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            fail();
        }
    }

    @Test
    public void testGetRerankingRecommendation_4() {
        Metadata metadata;
        String userId = generateUUID();
        List<String> itemIds = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            itemIds.add(String.format("ITEM_ID_%d",i));
        }
        int limit = generateRandomInteger(1, 10);
        String recommendationType = "category_page";

        RecommendationRequest recommendation = new RerankingRecommendationRequest.Builder(userId, itemIds)
                .limit(limit)
                .recommendationType(recommendationType)
                .build();
        try {
            metadata = new Metadata();
            metadata.userId = userId;
            metadata.itemIds = itemIds;
            metadata.limit = limit;
            checkSuccessfulGetRerankingRecommendation(recommendation, userId, metadata);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            fail();
        }
    }

    @Test
    public void testGetRerankingRecommendation_5() {
        Metadata metadata;
        String userId = generateUUID();
        List<String> itemIds = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            itemIds.add(String.format("ITEM_ID_%d",i));
        }
        String recommendationType = "category_page";

        RecommendationRequest recommendation = new RerankingRecommendationRequest.Builder(userId, itemIds)
                .recommendationType(recommendationType)
                .build();

        try {
            metadata = new Metadata();
            metadata.userId = userId;
            metadata.itemIds = itemIds;
            metadata.limit = itemIds.size();
            checkSuccessfulGetRerankingRecommendation(recommendation, userId, metadata);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            fail();
        }
    }

    @Test
    public void testGetRerankingRecommendation_6() {
        Metadata metadata;
        String userId = generateUUID();
        List<String> itemIds = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            itemIds.add(String.format("ITEM_ID_%d",i));
        }

        RecommendationRequest recommendation = new RerankingRecommendationRequest.Builder(userId, itemIds)
                .build();
        try {
            metadata = new Metadata();
            metadata.userId = userId;
            metadata.itemIds = itemIds;
            metadata.limit = itemIds.size();
            checkSuccessfulGetRerankingRecommendation(recommendation, userId, metadata);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            fail();
        }
    }

    @Test
    public void testGetRerankingRecommendation_7() {
        Metadata metadata;
        String userId = generateUUID();
        List<String> itemIds = new ArrayList<>();
        int limit = generateRandomInteger(1, 10);
        int offset = generateRandomInteger(20, 40);

        for (int i = 0; i < offset+limit; i++) {
            itemIds.add(String.format("ITEM_ID_%d",i));
        }
        String recommendationType = "home_page";

        Map<String, Integer> map = new HashMap<>();
        map.put("call_type", 1);
        map.put("response_type", 2);

        RecommendationRequest recommendation = new RerankingRecommendationRequest.Builder(userId, itemIds)
                .offset(offset)
                .limit(limit)
                .recommendationType(recommendationType)
                .options(map)
                .build();

        try {
            metadata = new Metadata();
            metadata.userId = userId;
            metadata.itemIds = itemIds;
            metadata.limit = limit;
            metadata.offset = offset;
            metadata.options = map;
            metadata.recommendationType = recommendationType;
            checkSuccessfulGetRerankingRecommendation(recommendation, userId, metadata);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            fail();
        }
    }

    @Test
    public void testGetNullRerankingRecommendation_1() {
        Metadata metadata;
        String userId = null;
        List<String> itemIds = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            itemIds.add(String.format("ITEM_ID_%d",i));
        }
        int limit = generateRandomInteger(1, 10);
        int offset = generateRandomInteger(20, 40);
        String recommendationType = "category_page";

        RecommendationRequest recommendation = new RerankingRecommendationRequest.Builder(userId, itemIds)
                .limit(limit)
                .offset(offset)
                .recommendationType(recommendationType)
                .build();

        try {
            metadata = new Metadata();
            metadata.userId = userId;
            metadata.itemIds = itemIds;
            metadata.limit = limit;
            metadata.offset = offset;
            checkSuccessfulGetRerankingRecommendation(recommendation, userId, metadata);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            fail();
        }
    }

    @Test
    public void testGetNullRerankingRecommendation_2() {
        Metadata metadata;
        String userId = null;
        List<String> itemIds = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            itemIds.add(String.format("ITEM_ID_%d",i));
        }
        int limit = generateRandomInteger(1, 10);
        int offset = generateRandomInteger(20, 40);

        RecommendationRequest recommendation = new RerankingRecommendationRequest.Builder(userId, itemIds)
                .limit(limit)
                .offset(offset)
                .build();

        try {
            metadata = new Metadata();
            metadata.userId = userId;
            metadata.itemIds = itemIds;
            metadata.limit = limit;
            metadata.offset = offset;
            checkSuccessfulGetRerankingRecommendation(recommendation, userId, metadata);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            fail();
        }
    }

    @Test
    public void testGetNullRerankingRecommendation_3() {
        Metadata metadata;
        String userId = null;
        List<String> itemIds = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            itemIds.add(String.format("ITEM_ID_%d",i));
        }
        int limit = generateRandomInteger(1, 10);
        String recommendationType = "category_page";

        RecommendationRequest recommendation = new RerankingRecommendationRequest.Builder(userId, itemIds)
                .limit(limit)
                .build();

        try {
            metadata = new Metadata();
            metadata.userId = userId;
            metadata.itemIds = itemIds;
            metadata.limit = limit;
            checkSuccessfulGetRerankingRecommendation(recommendation, userId, metadata);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            fail();
        }
    }

    @Test
    public void testGetNullRerankingRecommendation_4() {
        Metadata metadata;
        String userId = null;
        List<String> itemIds = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            itemIds.add(String.format("ITEM_ID_%d",i));
        }
        int limit = generateRandomInteger(1, 10);

        RecommendationRequest recommendation = new RerankingRecommendationRequest.Builder(userId, itemIds)
                .limit(limit)
                .build();

        try {
            metadata = new Metadata();
            metadata.userId = userId;
            metadata.itemIds = itemIds;
            metadata.limit = limit;
            checkSuccessfulGetRerankingRecommendation(recommendation, userId, metadata);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            fail();
        }
    }

    @Test
    public void testGetNullRerankingRecommendation_5() {
        Metadata metadata;
        String userId = null;
        List<String> itemIds = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            itemIds.add(String.format("ITEM_ID_%d",i));
        }
        String recommendationType = "category_page";

        RecommendationRequest recommendation = new RerankingRecommendationRequest.Builder(userId, itemIds)
                .recommendationType(recommendationType)
                .build();

        try {
            metadata = new Metadata();
            metadata.userId = userId;
            metadata.itemIds = itemIds;
            metadata.limit = itemIds.size();
            checkSuccessfulGetRerankingRecommendation(recommendation, userId, metadata);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            fail();
        }
    }

    @Test
    public void testGetNullRerankingRecommendation_6() {
        Metadata metadata;
        String userId = null;
        List<String> itemIds = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            itemIds.add(String.format("ITEM_ID_%d",i));
        }

        RecommendationRequest recommendation = new RerankingRecommendationRequest.Builder(userId, itemIds)
                .build();

        try {
            metadata = new Metadata();
            metadata.userId = userId;
            metadata.itemIds = itemIds;
            metadata.limit = itemIds.size();
            checkSuccessfulGetRerankingRecommendation(recommendation, userId, metadata);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            fail();
        }
    }

    @Test
    public void testGetNullRerankingRecommendation_7() {
        Metadata metadata;
        String userId = null;
        List<String> itemIds = new ArrayList<>();
        int limit = generateRandomInteger(1, 10);
        int offset = generateRandomInteger(20, 40);

        for (int i = 0; i < offset+limit; i++) {
            itemIds.add(String.format("ITEM_ID_%d",i));
        }
        String recommendationType = "home_page";

        Map<String, Integer> map = new HashMap<>();
        map.put("call_type", 1);
        map.put("response_type", 2);

        RecommendationRequest recommendation = new RerankingRecommendationRequest.Builder(userId, itemIds)
                .limit(limit)
                .offset(offset)
                .recommendationType(recommendationType)
                .options(map)
                .build();

        try {
            metadata = new Metadata();
            metadata.userId = userId;
            metadata.itemIds = itemIds;
            metadata.offset = offset;
            metadata.limit = limit;
            metadata.options = map;
            metadata.recommendationType = recommendationType;
            checkSuccessfulGetRerankingRecommendation(recommendation, userId, metadata);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            fail();
        }
    }

    @Test
    public void testGetRerankingRecommendationWrongClientId() {
        String userId = generateUUID();
        List<String> itemIds = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            itemIds.add(String.format("ITEM_ID_%d",i));
        }
        int limit = generateRandomInteger(1, 10);
        int offset = generateRandomInteger(20, 40);

        RecommendationRequest recommendation = new RerankingRecommendationRequest.Builder(userId, itemIds)
                .limit(limit)
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
    public void testGetRerankingRecommendationWrongSecret() {
        String userId = generateUUID();
        List<String> itemIds = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            itemIds.add(String.format("ITEM_ID_%d",i));
        }
        int limit = generateRandomInteger(1, 10);
        int offset = generateRandomInteger(20, 40);

        RecommendationRequest recommendation = new RerankingRecommendationRequest.Builder(userId, itemIds)
                .limit(limit)
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
    public void testGetTooLongRerankingRecommendation() {
        String userId = String.join("a", Collections.nCopies(501, "a"));
        List<String> itemIds = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            itemIds.add(String.format("ITEM_ID_%d",i));
        }
        int limit = generateRandomInteger(1, 10);
        int offset = generateRandomInteger(20, 40);
        try {
             new RerankingRecommendationRequest.Builder(userId, itemIds)
                    .limit(limit)
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
        List<String> itemIds = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            itemIds.add(String.format("ITEM_ID_%d",i));
        }
        int limit = 1_000_001;
        int offset = generateRandomInteger(20, 40);

        try {
            new RerankingRecommendationRequest.Builder(userId, itemIds)
                    .limit(limit)
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
        List<String> itemIds = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            itemIds.add(String.format("ITEM_ID_%d",i));
        }
        int limit = generateRandomInteger(20, 40);
        int offset = 1_000_001;

        try {
            new RerankingRecommendationRequest.Builder(userId, itemIds)
                    .limit(limit)
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
        List<String> itemIds = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            itemIds.add(String.format("ITEM_ID_%d",i));
        }
        String recommendationType = String.join("a", Collections.nCopies(501, "a"));
        int limit = generateRandomInteger(1, 10);
        int offset = generateRandomInteger(20, 40);
        try {
            new RerankingRecommendationRequest.Builder(userId, itemIds)
                    .limit(limit)
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
    public void testGetEmptyRerankingRecommendation() {
        String userId = "";
        List<String> itemIds = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            itemIds.add(String.format("ITEM_ID_%d",i));
        }
        int limit = generateRandomInteger(1, 10);
        int offset = generateRandomInteger(20, 40);
        try {
            new RerankingRecommendationRequest.Builder(userId, itemIds)
                    .limit(limit)
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
        List<String> itemIds = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            itemIds.add(String.format("ITEM_ID_%d",i));
        }
        int limit = -1;
        int offset = generateRandomInteger(20, 40);

        try {
            new RerankingRecommendationRequest.Builder(userId, itemIds)
                    .limit(limit)
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
        List<String> itemIds = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            itemIds.add(String.format("ITEM_ID_%d",i));
        }
        int limit = generateRandomInteger(20, 40);
        int offset = 0;

        RecommendationRequest recommendation = new RerankingRecommendationRequest.Builder(userId, itemIds)
                .limit(limit)
                .offset(offset)
                .build();

        try {
            metadata = new Metadata();
            metadata.userId = userId;
            metadata.itemIds = itemIds;
            metadata.limit = limit;
            metadata.offset = offset;
            checkSuccessfulGetRerankingRecommendation(recommendation, userId, metadata);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            fail();
        }
    }

    @Test
    public void testGetTooSmallOffsetRecommendation() {
        String userId = generateUUID();
        List<String> itemIds = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            itemIds.add(String.format("ITEM_ID_%d",i));
        }
        int limit = generateRandomInteger(20, 40);
        int offset = -1;

        try {
             new RerankingRecommendationRequest.Builder(userId, itemIds)
                    .limit(limit)
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
        List<String> itemIds = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            itemIds.add(String.format("ITEM_ID_%d",i));
        }
        String recommendationType = "";
        int limit = generateRandomInteger(1, 10);
        int offset = generateRandomInteger(20, 40);
        try {
            new RerankingRecommendationRequest.Builder(userId, itemIds)
                    .limit(limit)
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
    public void testGetTooBigItemIdsRecommendation() {
        String userId = generateUUID();
        List<String> itemIds = new ArrayList<>();
        for (int i = 0; i < 1_000_001; i++) {
            itemIds.add(String.format("ITEM_ID_%d",i));
        }
        String recommendationType = "";
        int limit = generateRandomInteger(1, 10);
        int offset = generateRandomInteger(20, 40);
        try {
            new RerankingRecommendationRequest.Builder(userId, itemIds)
                    .limit(limit)
                    .offset(offset)
                    .recommendationType(recommendationType)
                    .build();
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals(e.getMessage(), itemIdsExceptionMessage);
        } catch (Error e) {
            fail();
        }
    }

    @Test
    public void testGetTooLongItemIdsRecommendation() {
        String userId = generateUUID();
        List<String> itemIds = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            itemIds.add(String.format("ITEM_ID_%d",i));
        }
        itemIds.add(String.join("a", Collections.nCopies(501, "a")));
        String recommendationType = "";
        int limit = generateRandomInteger(1, 10);
        int offset = generateRandomInteger(20, 40);
        try {
            new RerankingRecommendationRequest.Builder(userId, itemIds)
                    .limit(limit)
                    .offset(offset)
                    .recommendationType(recommendationType)
                    .build();
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals(e.getMessage(), itemIdInListExceptionMessage);
        } catch (Error e) {
            fail();
        }
    }

    @Test
    public void testGetTooLongOptionsRerankingRecommendation() {
        String userId = generateUUID();
        List<String> itemIds = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            itemIds.add(String.format("ITEM_ID_%d",i));
        }
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
            new RerankingRecommendationRequest.Builder(userId, itemIds)
                    .limit(limit)
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

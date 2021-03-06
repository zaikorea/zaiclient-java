package org.zaikorea.ZaiClientTest;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.zaikorea.ZaiClient.ZaiClient;
import org.zaikorea.ZaiClient.exceptions.ZaiClientException;
import org.zaikorea.ZaiClient.request.*;
import org.zaikorea.ZaiClient.response.RecommendationResponse;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.model.*;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

public class ZaiClientRerankingRecommendationJavaTest {
    private static final String clientId = "test";
    private static final String clientSecret = "KVPzvdHTPWnt0xaEGc2ix-eqPXFCdEV5zcqolBr_h1k"; // this secret key is for
                                                                                              // testing purposes only
    private static final String recLogTableName = "rec_log_test";
    private static final String recLogTablePartitionKey = "user_id";
    private static final String recLogTableSortKey = "timestamp";
    private static final String recLogRecommendations = "recommendations";

    private static final String userIdExceptionMessage = "Length of user id must be between 1 and 100.";
    private static final String itemIdsExceptionMessage = "Length of item_ids must be between 1 and 1000,000.";
    private static final String itemIdInListExceptionMessage = "Length of item id in item id list must be between 1 and 100.";
    private static final String recommendationTypeExceptionMessage = "Length of recommendation type must be between 1 and 100.";
    private static final String limitExceptionMessage = "Limit must be between 1 and 1000,000.";
    private static final String offsetExceptionMessage = "Offset must be between 0 and 1000,000.";

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

    private void checkSuccessfulGetRerankingRecommendation(RecommendationRequest recommendation, String userId) {
        try {
            RecommendationResponse response = testClient.getRecommendations(recommendation);
            int limit = recommendation.getLimit();

            Map<String, String> logItem = getRecLog(userId);

            if (userId == null) {
                userId = "null";
                logItem = getRecLog("null");
            }

            // assertNotNull(logItem);
            // assertNotEquals(logItem.size(), 0);
            // assertEquals(logItem.get(recLogRecommendations).split(",").length,
            // response.getItems().size());
            assertEquals(response.getItems().size(), limit);
            assertEquals(response.getCount(), limit);
            // assertTrue(deleteRecLog(userId));

        } catch (IOException | ZaiClientException e) {
            fail();
        }
    }

    @Before
    public void setup() {
        testClient = new ZaiClient(clientId, clientSecret);
        incorrectIdClient = new ZaiClient("." + clientId, clientSecret);
        incorrectSecretClient = new ZaiClient(clientId, "." + clientSecret);
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
        String userId = generateUUID();
        List<String> itemIds = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            itemIds.add(generateUUID());
        }
        int limit = generateRandomInteger(1, 10);
        int offset = generateRandomInteger(20, 40);
        String recommendationType = "category_page";

        RecommendationRequest recommendation = new RerankingRecommendationRequest(userId, itemIds, limit, offset,
                recommendationType);
        checkSuccessfulGetRerankingRecommendation(recommendation, userId);
    }

    @Test
    public void testGetRerankingRecommendation_2() {
        String userId = generateUUID();
        List<String> itemIds = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            itemIds.add(generateUUID());
        }
        int limit = generateRandomInteger(1, 10);
        int offset = generateRandomInteger(20, 40);

        RecommendationRequest recommendation = new RerankingRecommendationRequest(userId, itemIds, limit, offset);
        checkSuccessfulGetRerankingRecommendation(recommendation, userId);
    }

    @Test
    public void testGetRerankingRecommendation_3() {
        String userId = generateUUID();
        List<String> itemIds = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            itemIds.add(generateUUID());
        }
        int limit = generateRandomInteger(1, 10);

        RecommendationRequest recommendation = new RerankingRecommendationRequest(userId, itemIds, limit);
        checkSuccessfulGetRerankingRecommendation(recommendation, userId);
    }

    @Test
    public void testGetRerankingRecommendation_4() {
        String userId = generateUUID();
        List<String> itemIds = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            itemIds.add(generateUUID());
        }
        int limit = generateRandomInteger(1, 10);
        String recommendationType = "category_page";

        RecommendationRequest recommendation = new RerankingRecommendationRequest(userId, itemIds, limit,
                recommendationType);
        checkSuccessfulGetRerankingRecommendation(recommendation, userId);
    }

    @Test
    public void testGetRerankingRecommendation_5() {
        String userId = generateUUID();
        List<String> itemIds = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            itemIds.add(generateUUID());
        }
        String recommendationType = "category_page";

        RecommendationRequest recommendation = new RerankingRecommendationRequest(userId, itemIds, recommendationType);
        checkSuccessfulGetRerankingRecommendation(recommendation, userId);
    }

    @Test
    public void testGetRerankingRecommendation_6() {
        String userId = generateUUID();
        List<String> itemIds = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            itemIds.add(generateUUID());
        }

        RecommendationRequest recommendation = new RerankingRecommendationRequest(userId, itemIds);
        checkSuccessfulGetRerankingRecommendation(recommendation, userId);
    }

    @Test
    public void testGetNullRerankingRecommendation_1() {
        String userId = null;
        List<String> itemIds = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            itemIds.add(generateUUID());
        }
        int limit = generateRandomInteger(1, 10);
        int offset = generateRandomInteger(20, 40);
        String recommendationType = "category_page";

        RecommendationRequest recommendation = new RerankingRecommendationRequest(userId, itemIds, limit, offset,
                recommendationType);
        checkSuccessfulGetRerankingRecommendation(recommendation, userId);
    }

    @Test
    public void testGetNullRerankingRecommendation_2() {
        String userId = null;
        List<String> itemIds = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            itemIds.add(generateUUID());
        }
        int limit = generateRandomInteger(1, 10);
        int offset = generateRandomInteger(20, 40);

        RecommendationRequest recommendation = new RerankingRecommendationRequest(userId, itemIds, limit, offset);
        checkSuccessfulGetRerankingRecommendation(recommendation, userId);
    }

    @Test
    public void testGetNullRerankingRecommendation_3() {
        String userId = null;
        List<String> itemIds = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            itemIds.add(generateUUID());
        }
        int limit = generateRandomInteger(1, 10);
        String recommendationType = "category_page";

        RecommendationRequest recommendation = new RerankingRecommendationRequest(userId, itemIds, limit,
                recommendationType);
        checkSuccessfulGetRerankingRecommendation(recommendation, userId);
    }

    @Test
    public void testGetNullRerankingRecommendation_4() {
        String userId = null;
        List<String> itemIds = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            itemIds.add(generateUUID());
        }
        int limit = generateRandomInteger(1, 10);

        RecommendationRequest recommendation = new RerankingRecommendationRequest(userId, itemIds, limit);
        checkSuccessfulGetRerankingRecommendation(recommendation, userId);
    }

    @Test
    public void testGetNullRerankingRecommendation_5() {
        String userId = null;
        List<String> itemIds = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            itemIds.add(generateUUID());
        }
        String recommendationType = "category_page";

        RecommendationRequest recommendation = new RerankingRecommendationRequest(userId, itemIds, recommendationType);
        checkSuccessfulGetRerankingRecommendation(recommendation, userId);
    }

    @Test
    public void testGetNullRerankingRecommendation_6() {
        String userId = null;
        List<String> itemIds = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            itemIds.add(generateUUID());
        }

        RecommendationRequest recommendation = new RerankingRecommendationRequest(userId, itemIds);
        checkSuccessfulGetRerankingRecommendation(recommendation, userId);
    }

    @Test
    public void testGetRerankingRecommendationWrongClientId() {
        String userId = generateUUID();
        List<String> itemIds = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            itemIds.add(generateUUID());
        }
        int limit = generateRandomInteger(1, 10);
        int offset = generateRandomInteger(20, 40);

        RecommendationRequest recommendation = new RerankingRecommendationRequest(userId, itemIds, limit, offset);
        try {
            incorrectIdClient.getRecommendations(recommendation);
        } catch (IOException e) {
            fail();
        } catch (ZaiClientException e) {
            assertEquals(e.getHttpStatusCode(), 404);
        }
    }

    @Test
    public void testGetRerankingRecommendationWrongSecret() {
        String userId = generateUUID();
        List<String> itemIds = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            itemIds.add(generateUUID());
        }
        int limit = generateRandomInteger(1, 10);
        int offset = generateRandomInteger(20, 40);

        RecommendationRequest recommendation = new RerankingRecommendationRequest(userId, itemIds, limit, offset);
        try {
            incorrectSecretClient.getRecommendations(recommendation);
        } catch (IOException e) {
            fail();
        } catch (ZaiClientException e) {
            assertEquals(e.getHttpStatusCode(), 401);
        }
    }

    @Test
    public void testGetTooLongRerankingRecommendation() {
        String userId = String.join("a", Collections.nCopies(101, "a"));
        List<String> itemIds = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            itemIds.add(generateUUID());
        }
        int limit = generateRandomInteger(1, 10);
        int offset = generateRandomInteger(20, 40);
        try {
            new RerankingRecommendationRequest(userId, itemIds, limit, offset);
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
            itemIds.add(generateUUID());
        }
        int limit = 1_000_001;
        int offset = generateRandomInteger(20, 40);

        try {
            new RerankingRecommendationRequest(userId, itemIds, limit, offset);
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
            itemIds.add(generateUUID());
        }
        int limit = generateRandomInteger(20, 40);
        int offset = 1_000_001;

        try {
            new RerankingRecommendationRequest(userId, itemIds, limit, offset);
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
            itemIds.add(generateUUID());
        }
        String recommendationType = String.join("a", Collections.nCopies(101, "a"));
        int limit = generateRandomInteger(1, 10);
        int offset = generateRandomInteger(20, 40);
        try {
            new RerankingRecommendationRequest(userId, itemIds, limit, offset,
                    recommendationType);
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
            itemIds.add(generateUUID());
        }
        int limit = generateRandomInteger(1, 10);
        int offset = generateRandomInteger(20, 40);
        try {
            new RerankingRecommendationRequest(userId, itemIds, limit, offset);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals(e.getMessage(), userIdExceptionMessage);
        } catch (Error e) {
            fail();
        }
    }

    @Test
    public void testGetZeroLimitRecommendation() {
        String userId = generateUUID();
        List<String> itemIds = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            itemIds.add(generateUUID());
        }
        int limit = 0;
        int offset = generateRandomInteger(20, 40);

        try {
            new RerankingRecommendationRequest(userId, itemIds, limit, offset);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals(e.getMessage(), limitExceptionMessage);
        } catch (Error e) {
            fail();
        }
    }

    @Test
    public void testGetTooSmallLimitRecommendation() {
        String userId = generateUUID();
        List<String> itemIds = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            itemIds.add(generateUUID());
        }
        int limit = -1;
        int offset = generateRandomInteger(20, 40);

        try {
            new RerankingRecommendationRequest(userId, itemIds, limit, offset);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals(e.getMessage(), limitExceptionMessage);
        } catch (Error e) {
            fail();
        }
    }

    @Test
    public void testGetZeroOffsetRecommendation() {
        String userId = generateUUID();
        List<String> itemIds = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            itemIds.add(generateUUID());
        }
        int limit = generateRandomInteger(20, 40);
        int offset = 0;

        RecommendationRequest recommendation = new RerankingRecommendationRequest(userId, itemIds, limit, offset);
        checkSuccessfulGetRerankingRecommendation(recommendation, userId);
    }

    @Test
    public void testGetTooSmallOffsetRecommendation() {
        String userId = generateUUID();
        List<String> itemIds = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            itemIds.add(generateUUID());
        }
        int limit = generateRandomInteger(20, 40);
        int offset = -1;

        try {
            new RerankingRecommendationRequest(userId, itemIds, limit, offset);
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
            itemIds.add(generateUUID());
        }
        String recommendationType = "";
        int limit = generateRandomInteger(1, 10);
        int offset = generateRandomInteger(20, 40);
        try {
            new RerankingRecommendationRequest(userId, itemIds, limit, offset,
                    recommendationType);
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
            itemIds.add(generateUUID());
        }
        String recommendationType = "";
        int limit = generateRandomInteger(1, 10);
        int offset = generateRandomInteger(20, 40);
        try {
            new RerankingRecommendationRequest(userId, itemIds, limit, offset,
                    recommendationType);
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
            itemIds.add(generateUUID());
        }
        itemIds.add(String.join("a", Collections.nCopies(101, "a")));
        String recommendationType = "";
        int limit = generateRandomInteger(1, 10);
        int offset = generateRandomInteger(20, 40);
        try {
            new RerankingRecommendationRequest(userId, itemIds, limit, offset,
                    recommendationType);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals(e.getMessage(), itemIdInListExceptionMessage);
        } catch (Error e) {
            fail();
        }
    }

}

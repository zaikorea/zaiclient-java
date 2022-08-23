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

public class ZaiClientRelatedItemsRecommendationJavaTest {
    private static final String clientId = "test";
    private static final String clientSecret = "KVPzvdHTPWnt0xaEGc2ix-eqPXFCdEV5zcqolBr_h1k"; // this secret key is for
                                                                                              // testing purposes only
    private static final String recLogTableName = "rec_log_test";
    private static final String recLogTablePartitionKey = "user_id";
    private static final String recLogTableSortKey = "timestamp";
    private static final String recLogRecommendations = "recommendations";

    private static final String userIdExceptionMessage = "Length of user id must be between 1 and 100.";
    private static final String itemIdExceptionMessage = "Length of item id must be between 1 and 100.";
    private static final String recommendationTypeExceptionMessage = "Length of recommendation type must be between 1 and 100.";
    private static final String limitExceptionMessage = "Limit must be between 1 and 1000,000.";
    private static final String offsetExceptionMessage = "Offset must be between 0 and 1000,000.";
    private static final String optionsExceptionMessage = "Length of options must be less than 1000 when converted to string.";

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

    private void checkSuccessfulGetRelatedRecommendation(RecommendationRequest recommendation, String itemId) {
        try {
            RecommendationResponse response = testClient.getRecommendations(recommendation);
            int limit = recommendation.getLimit();

            Map<String, String> logItem = getRecLog(itemId);

            if (itemId == null) {
                itemId = "null";
                logItem = getRecLog("null");
            }

            // assertNotNull(logItem);
            // assertNotEquals(logItem.size(), 0);
            // assertEquals(logItem.get(recLogRecommendations).split(",").length,
            // response.getItems().size());
            assertEquals(response.getItems().size(), limit);
            assertEquals(response.getCount(), limit);
            // assertTrue(deleteRecLog(itemId));

        } catch (IOException | ZaiClientException e) {
            fail();
        }
    }

    @Before
    public void setup() {
        testClient = new ZaiClient.Builder(clientId, clientSecret)
                .connectTimeout(30)
                .readTimeout(10)
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
    public void testGetRelatedRecommendation_1() {
        String itemId = generateUUID();
        int limit = generateRandomInteger(1, 10);
        int offset = generateRandomInteger(20, 40);
        String recommendationType = "product_detail_page";

        RecommendationRequest recommendation = new RelatedItemsRecommendationRequest.Builder(itemId, limit)
                .offset(offset)
                .recommendationType(recommendationType)
                .build();
        checkSuccessfulGetRelatedRecommendation(recommendation, itemId);
    }

    @Test
    public void testGetRelatedRecommendation_2() {
        String itemId = generateUUID();
        int limit = generateRandomInteger(1, 10);
        int offset = generateRandomInteger(20, 40);

        RecommendationRequest recommendation = new RelatedItemsRecommendationRequest.Builder(itemId, limit)
                .offset(offset)
                .build();
        checkSuccessfulGetRelatedRecommendation(recommendation, itemId);
    }

    @Test
    public void testGetRelatedRecommendation_3() {
        String itemId = generateUUID();
        int limit = generateRandomInteger(1, 10);

        RecommendationRequest recommendation = new RelatedItemsRecommendationRequest.Builder(itemId, limit)
                .build();
        checkSuccessfulGetRelatedRecommendation(recommendation, itemId);
    }

    @Test
    public void testGetRelatedRecommendation_4() {
        String itemId = generateUUID();
        int limit = generateRandomInteger(1, 10);
        String recommendationType = "product_detail_page";

        RecommendationRequest recommendation = new RelatedItemsRecommendationRequest.Builder(itemId, limit)
                .recommendationType(recommendationType)
                .build();
        checkSuccessfulGetRelatedRecommendation(recommendation, itemId);
    }

    @Test
    public void testGetRelatedRecommendation_5() {
        String itemId = generateUUID();
        int limit = generateRandomInteger(1, 10);
        int offset = generateRandomInteger(20, 40);
        String recommendationType = "home_page";

        Map<String, Integer> map = new HashMap<>();
        map.put("call_type", 1);
        map.put("response_type", 2);

        RecommendationRequest recommendation = new RelatedItemsRecommendationRequest.Builder(itemId, limit)
                .offset(offset)
                .recommendationType(recommendationType)
                .options(map)
                .build();
        checkSuccessfulGetRelatedRecommendation(recommendation, itemId);
    }

    @Test
    public void testGetNullRelatedRecommendation() {
        String itemId = null;
        int limit = generateRandomInteger(1, 10);
        int offset = generateRandomInteger(20, 40);
        String recommendationType = "product_detail_page";

        try {
            new RelatedItemsRecommendationRequest.Builder(itemId, limit)
                    .offset(offset)
                    .recommendationType(recommendationType)
                    .build();
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals(e.getMessage(), itemIdExceptionMessage);
        } catch (Error e) {
            fail();
        }
    }

    @Test
    public void testGetRelatedRecommendationWrongClientId() {
        String itemId = generateUUID();
        int limit = generateRandomInteger(1, 10);
        int offset = generateRandomInteger(20, 40);

        RecommendationRequest recommendation = new RelatedItemsRecommendationRequest.Builder(itemId, limit)
                .offset(offset)
                .build();
        try {
            incorrectIdClient.getRecommendations(recommendation);
        } catch (IOException e) {
            fail();
        } catch (ZaiClientException e) {
            assertEquals(e.getHttpStatusCode(), 404);
        }
    }

    @Test
    public void testGetRelatedRecommendationWrongSecret() {
        String itemId = generateUUID();
        int limit = generateRandomInteger(1, 10);
        int offset = generateRandomInteger(20, 40);

        RecommendationRequest recommendation = new RelatedItemsRecommendationRequest.Builder(itemId, limit)
                .offset(offset)
                .build();
        try {
            incorrectSecretClient.getRecommendations(recommendation);
        } catch (IOException e) {
            fail();
        } catch (ZaiClientException e) {
            assertEquals(e.getHttpStatusCode(), 401);
        }
    }

    @Test
    public void testGetTooLongRelatedItemRecommendation() {
        String itemId = String.join("a", Collections.nCopies(101, "a"));
        int limit = generateRandomInteger(1, 10);
        int offset = generateRandomInteger(20, 40);
        try {
            new RelatedItemsRecommendationRequest.Builder(itemId, limit)
                    .offset(offset)
                    .build();
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals(e.getMessage(), itemIdExceptionMessage);
        } catch (Error e) {
            fail();
        }
    }

    @Test
    public void testGetTooBigLimitRecommendation() {
        String itemId = generateUUID();
        int limit = 1_000_001;
        int offset = generateRandomInteger(20, 40);

        try {
            new RelatedItemsRecommendationRequest.Builder(itemId, limit)
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
        String itemId = generateUUID();
        int limit = generateRandomInteger(20, 40);
        int offset = 1_000_001;

        try {
            new RelatedItemsRecommendationRequest.Builder(itemId, limit)
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
        String itemId = generateUUID();
        String recommendationType = String.join("a", Collections.nCopies(101, "a"));
        int limit = generateRandomInteger(1, 10);
        int offset = generateRandomInteger(20, 40);
        try {
            new RelatedItemsRecommendationRequest.Builder(itemId, limit)
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
    public void testGetEmptyRelatedRecommendation() {
        String itemId = "";
        int limit = generateRandomInteger(1, 10);
        int offset = generateRandomInteger(20, 40);
        try {
            new RelatedItemsRecommendationRequest.Builder(itemId, limit)
                    .offset(offset)
                    .build();
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals(e.getMessage(), itemIdExceptionMessage);
        } catch (Error e) {
            fail();
        }
    }

    @Test
    public void testGetZeroLimitRecommendation() {
        String itemId = generateUUID();
        int limit = 0;
        int offset = generateRandomInteger(20, 40);

        try {
            new RelatedItemsRecommendationRequest.Builder(itemId, limit)
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
    public void testGetTooSmallLimitRecommendation() {
        String itemId = generateUUID();
        int limit = -1;
        int offset = generateRandomInteger(20, 40);

        try {
            new RelatedItemsRecommendationRequest.Builder(itemId, limit)
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
        String itemId = generateUUID();
        int limit = generateRandomInteger(20, 40);
        int offset = 0;

        RecommendationRequest recommendation = new RelatedItemsRecommendationRequest.Builder(itemId, limit)
                .offset(offset)
                .build();
        checkSuccessfulGetRelatedRecommendation(recommendation, itemId);
    }

    @Test
    public void testGetTooSmallOffsetRecommendation() {
        String itemId = generateUUID();
        int limit = generateRandomInteger(20, 40);
        int offset = -1;

        try {
            new RelatedItemsRecommendationRequest.Builder(itemId, limit)
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
        String itemId = generateUUID();
        String recommendationType = "";
        int limit = generateRandomInteger(1, 10);
        int offset = generateRandomInteger(20, 40);
        try {
            new RelatedItemsRecommendationRequest.Builder(itemId, limit)
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

    public void testGetTooLongOptionsRelatedItemsRecommendation() {
        String itemId = generateUUID();
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
            new RelatedItemsRecommendationRequest.Builder(itemId, limit)
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

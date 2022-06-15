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


public class ZaiClientRecommenderJavaTest {

    private static final String clientId = "test";
    private static final String clientSecret = "KVPzvdHTPWnt0xaEGc2ix-eqPXFCdEV5zcqolBr_h1k"; // this secret key is for testing purposes only
    private static final String recLogTableName = "rec_log_test";
    private static final String recLogTablePartitionKey = "user_id";
    private static final String recLogTableSortKey = "timestamp";
    private static final String recLogRecommendations = "recommendations";

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

        HashMap<String,String> attrNameAlias = new HashMap<>();
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

    private void checkSuccessfulGetUserRecommendation(RecommendationRequest recommendation, String userId) {
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
            // assertEquals(logItem.get(recLogRecommendations).split(",").length, response.getItems().size());
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
    public void testGetUserRecommendation() {
        String userId = generateUUID();
        int limit = generateRandomInteger(1, 10);
        int offset = generateRandomInteger(20, 40);
        
        RecommendationRequest recommendation = new UserRecommendationRequest(userId, limit, offset);
        checkSuccessfulGetUserRecommendation(recommendation, userId);
    }

    @Test
    public void testGetNullUserRecommendation() {
        String userId = null;
        int limit = generateRandomInteger(1, 10);
        int offset = generateRandomInteger(20, 40);

        RecommendationRequest recommendation = new UserRecommendationRequest(userId, limit, offset);
        checkSuccessfulGetUserRecommendation(recommendation, userId);
    }

    @Test
    public void testGetUserRecommendationWithRecommendationType() {
        String userId = generateUUID();
        int limit = generateRandomInteger(1, 10);
        int offset = generateRandomInteger(20, 40);

        RecommendationRequest recommendation = new UserRecommendationRequest(userId, limit, offset, "home_page");
        checkSuccessfulGetUserRecommendation(recommendation, userId);
    }
    
    @Test
    public void testGetUserRecommendationWrongClientId() {
        String userId = generateUUID();
        int limit = generateRandomInteger(1, 10);
        int offset = generateRandomInteger(20, 40);

        RecommendationRequest recommendation = new UserRecommendationRequest(userId, limit, offset);
        try {
            incorrectIdClient.getRecommendations(recommendation);
        } catch (IOException e) {
            fail();
        } catch (ZaiClientException e) {
            assertEquals(e.getHttpStatusCode(), 404);
        }
    }

    @Test
    public void testGetUserRecommendationWrongSecret() {
        String userId = generateUUID();
        int limit = generateRandomInteger(1, 10);
        int offset = generateRandomInteger(20, 40);

        RecommendationRequest recommendation = new UserRecommendationRequest(userId, limit, offset);
        try {
            incorrectSecretClient.getRecommendations(recommendation);
        } catch (IOException e) {
            fail();
        } catch (ZaiClientException e) {
            assertEquals(e.getHttpStatusCode(), 401);
        }
    }

    @Test
    public void testGetTooLongUserRecommendation() {
        String userId = String.join("a", Collections.nCopies(101, "a"));
        int limit = generateRandomInteger(1, 10);
        int offset = generateRandomInteger(20, 40);

        RecommendationRequest recommendation = new UserRecommendationRequest(userId, limit, offset);
        
        try {
            testClient.getRecommendations(recommendation);
        } catch (IOException e) {
            fail();
        } catch (ZaiClientException e) {
            assertEquals(e.getHttpStatusCode(), 422);
        }
    }

    @Test
    public void testGetTooBigLimitRecommendation() {
        String userId = generateUUID();
        int limit = 1_000_001;
        int offset = generateRandomInteger(20, 40);

        RecommendationRequest recommendation = new UserRecommendationRequest(userId, limit, offset);
        
        try {
            testClient.getRecommendations(recommendation);
        } catch (IOException e) {
            fail();
        } catch (ZaiClientException e) {
            assertEquals(e.getHttpStatusCode(), 422);
        }
    }

    @Test
    public void testGetTooBigOffsetRecommendation() {
        String userId = generateUUID();
        int limit = generateRandomInteger(20, 40);
        int offset = 1_000_001;

        RecommendationRequest recommendation = new UserRecommendationRequest(userId, limit, offset);
        
        try {
            testClient.getRecommendations(recommendation);
        } catch (IOException e) {
            fail();
        } catch (ZaiClientException e) {
            assertEquals(e.getHttpStatusCode(), 422);
        }
    }

}

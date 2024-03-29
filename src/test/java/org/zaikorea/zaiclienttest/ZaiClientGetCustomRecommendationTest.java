package org.zaikorea.zaiclienttest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.zaikorea.zaiclient.ZaiClient;
import org.zaikorea.zaiclient.exceptions.ZaiClientException;
import org.zaikorea.zaiclient.request.recommendations.GetCustomRecommendation;
import org.zaikorea.zaiclient.request.recommendations.RecommendationQuery;
import org.zaikorea.zaiclient.request.recommendations.RecommendationRequest;
import org.zaikorea.zaiclient.response.RecommendationResponse;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;

public class ZaiClientGetCustomRecommendationTest {

    private static final String clientId = "test";
    private static final String clientSecret = "KVPzvdHTPWnt0xaEGc2ix-eqPXFCdEV5zcqolBr_h1k"; // this secret key is for

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

    private static final String nullLimitExceptionMessage = "The value of limit must not be null";

    private ZaiClient testClientToDevEndpoint; // TODO: Figure out to map dev endpoint with environment variable
    private static final Region region = Region.AP_NORTHEAST_2;
    private DynamoDbClient ddbClient;

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

    private void checkSuccessfulGetCustomRecommendation(RecommendationRequest recommendation,
            Map<String, Object> expectedMetadata) {
        RecommendationQuery recQuery = recommendation.getPayload();

        String recommendationType = recQuery.getRecommendationType();
        int offset = recQuery.getOffset();
        String userId = recQuery.getUserId();
        String itemId = recQuery.getItemId();
        int limit = recQuery.getLimit();
        List<String> itemIds = recQuery.getItemIds();

        try {
            RecommendationResponse response = testClientToDevEndpoint.sendRequest(recommendation);

            // Response Testing
            List<String> responseItems = response.getItems();
            String recommendationId = response.getRecommendationId();
            for (int i = 0; i < limit; i++) {
                String expectedItem = String.format("ITEM_ID_%d", i + offset);
                assertEquals(expectedItem, responseItems.get(i));
            }

            // Metadata Testing
            Map<String, Object> metadata = response.getMetadata();
            for (String key : expectedMetadata.keySet()) {
                if (expectedMetadata.get(key) instanceof Integer && metadata.get(key) instanceof Double)
                    assertEquals(expectedMetadata.get(key), ((Double) metadata.get(key)).intValue());    
                else
                    assertEquals(expectedMetadata.get(key), metadata.get(key));    
            }
            
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
        testClientToDevEndpoint = new ZaiClient.Builder(clientId, clientSecret)
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
    public void testGetCustomRecommendation_1() {
        String userId = TestUtils.generateUUID();

        int limit = TestUtils.generateRandomInteger(1, 10);
        int offset = TestUtils.generateRandomInteger(20, 40);

        String recommendationType = "product-widget-recommendations";

        RecommendationRequest recommendation = new GetCustomRecommendation.Builder(recommendationType)
                .userId(userId)
                .offset(offset)
                .limit(limit)
                .build();

        try {
            Map<String, Object> expectedMetadata = new HashMap<String, Object>();
            expectedMetadata.put("user_id", userId);
            expectedMetadata.put("limit", limit);
            expectedMetadata.put("offset", offset);
            expectedMetadata.put("recommendation_type", recommendationType);
            expectedMetadata.put("call_type", recommendationType);
            checkSuccessfulGetCustomRecommendation(recommendation, expectedMetadata);
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testGetCustomRecommendation_2() {
        String itemId = TestUtils.generateUUID();

        int limit = TestUtils.generateRandomInteger(1, 10);

        String recommendationType = "homepage-main-recommendations";

        RecommendationRequest recommendation = new GetCustomRecommendation.Builder(recommendationType)
                .limit(limit)
                .itemId(itemId)
                .build();

        try {
            Map<String, Object> expectedMetadata = new HashMap<String, Object>();
            expectedMetadata.put("item_id", itemId);
            expectedMetadata.put("limit", limit);
            expectedMetadata.put("recommendation_type", recommendationType);
            expectedMetadata.put("call_type", recommendationType);
            checkSuccessfulGetCustomRecommendation(recommendation, expectedMetadata);
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testGetCustomRecommendation_3() {
        int limit = TestUtils.generateRandomInteger(1, 10);

        List<String> itemIds = new ArrayList<>();
        for (int i = 0; i < limit; i++) {
            itemIds.add(TestUtils.generateUUID());
        }

        String recommendationType = "category-widget2-recommendations";

        RecommendationRequest recommendation = new GetCustomRecommendation.Builder(recommendationType)
                .itemIds(itemIds)
                .limit(itemIds.size())
                .build();

        try {
            Map<String, Object> expectedMetadata = new HashMap<String, Object>();
            expectedMetadata.put("item_ids", itemIds);
            expectedMetadata.put("limit", itemIds.size());
            expectedMetadata.put("recommendation_type", recommendationType);
            expectedMetadata.put("call_type", recommendationType);
            checkSuccessfulGetCustomRecommendation(recommendation, expectedMetadata);
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testGetCustomRecommendation_4() {
        String userId = TestUtils.generateUUID();
        String itemId = TestUtils.generateUUID();
        List<String> itemIds = new ArrayList<>();
        int itemsCount = TestUtils.generateRandomInteger(1, 10);

        for (int i = 0; i < itemsCount; i++) {
            itemIds.add(TestUtils.generateUUID());
        }

        int limit = TestUtils.generateRandomInteger(1, 10);
        String recommendationType = "category-widget2-recommendations";

        RecommendationRequest recommendation = new GetCustomRecommendation.Builder(recommendationType)
                .userId(userId)
                .itemId(itemId)
                .itemIds(itemIds)
                .limit(limit)
                .build();

        try {
            Map<String, Object> expectedMetadata = new HashMap<String, Object>();
            expectedMetadata.put("user_id", userId);
            expectedMetadata.put("item_id", itemId);
            expectedMetadata.put("item_ids", itemIds);
            expectedMetadata.put("limit", limit);
            expectedMetadata.put("recommendation_type", recommendationType);
            expectedMetadata.put("call_type", recommendationType);
            checkSuccessfulGetCustomRecommendation(recommendation, expectedMetadata);
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testGetCustomRecommendation_5() {
        String userId = TestUtils.generateUUID();
        String itemId = TestUtils.generateUUID();
        List<String> itemIds = new ArrayList<>();
        int itemsCount = TestUtils.generateRandomInteger(1, 10);

        for (int i = 0; i < itemsCount; i++) {
            itemIds.add(TestUtils.generateUUID());
        }

        int limit = TestUtils.generateRandomInteger(1, 10);
        String recommendationType = "category-widget2-recommendations";

        Map<String, String> options = new HashMap<>();
        options.put("option1", "value1");
        options.put("option2", "value2");

        RecommendationRequest recommendation = new GetCustomRecommendation.Builder(recommendationType)
                .userId(userId)
                .itemId(itemId)
                .itemIds(itemIds)
                .limit(limit)
                .options(options)
                .build();

        try {
            Map<String, Object> expectedMetadata = new HashMap<String, Object>();
            expectedMetadata.put("user_id", userId);
            expectedMetadata.put("item_id", itemId);
            expectedMetadata.put("item_ids", itemIds);
            expectedMetadata.put("limit", limit);
            expectedMetadata.put("recommendation_type", recommendationType);
            expectedMetadata.put("call_type", recommendationType);
            expectedMetadata.put("options", options);
            checkSuccessfulGetCustomRecommendation(recommendation, expectedMetadata);
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testGetCustomRecommendation_6() {
        String userId = TestUtils.generateUUID();
        String itemId = TestUtils.generateUUID();
        List<String> itemIds = new ArrayList<>();
        int itemsCount = TestUtils.generateRandomInteger(1, 10);

        for (int i = 0; i < itemsCount; i++) {
            itemIds.add(TestUtils.generateUUID());
        }

        int limit = TestUtils.generateRandomInteger(1, 10);
        String recommendationType = "category-widget2-recommendations";

        Map<String, Integer> options = new HashMap<>();
        options.put("option1", 1);
        options.put("option2", 2);

        RecommendationRequest recommendation = new GetCustomRecommendation.Builder(recommendationType)
                .userId(userId)
                .itemId(itemId)
                .itemIds(itemIds)
                .limit(limit)
                .options(options)
                .build();

        try {
            Map<String, Object> expectedMetadata = new HashMap<String, Object>();
            expectedMetadata.put("user_id", userId);
            expectedMetadata.put("item_id", itemId);
            expectedMetadata.put("item_ids", itemIds);
            expectedMetadata.put("limit", limit);
            expectedMetadata.put("recommendation_type", recommendationType);
            expectedMetadata.put("call_type", recommendationType);
            checkSuccessfulGetCustomRecommendation(recommendation, expectedMetadata);
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testGetCustomRecommendation_7() {
        int limit = TestUtils.generateRandomInteger(1, 10);
        String recommendationType = "category-widget2-recommendations";

        RecommendationRequest recommendation = new GetCustomRecommendation.Builder(recommendationType)
                .limit(limit)
                .build();

        try {
            Map<String, Object> expectedMetadata = new HashMap<String, Object>();
            expectedMetadata.put("limit", limit);
            expectedMetadata.put("recommendation_type", recommendationType);
            expectedMetadata.put("call_type", recommendationType);
            checkSuccessfulGetCustomRecommendation(recommendation, expectedMetadata);
        } catch (Exception e) {
            fail();
        }
    }

    // Unhappy path
    @Test
    public void testGetCustomRecommendationWithoutLimit() {
        int itemCount = TestUtils.generateRandomInteger(1, 10);

        List<String> itemIds = new ArrayList<>();
        for (int i = 0; i < itemCount; i++) {
            itemIds.add(TestUtils.generateUUID());
        }

        

        try {
            RecommendationRequest recommendation = new GetCustomRecommendation.Builder("homepage-main-recommendations")
                .itemIds(itemIds)
                .build();
            Map<String, Object> expectedMetadata = new HashMap<String, Object>();
            expectedMetadata.put("item_ids", itemIds);
            expectedMetadata.put("offset", 0);
            checkSuccessfulGetCustomRecommendation(recommendation, expectedMetadata);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals(nullLimitExceptionMessage, e.getMessage());
        } catch (Error e) {
            fail();
        }
    }

    @Test
    public void testGetCustomRecommendationWithWrongRecommendationTypeFormat() {
        String recommendationType = "homepage-main-rec%&$";
        String userId = TestUtils.generateUUID();
        int limit = TestUtils.generateRandomInteger(1, 10);

        try {
            new GetCustomRecommendation.Builder(recommendationType)
                    .userId(userId)
                    .limit(limit)
                    .build();
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("Recommendation Type must be in the format of [0-9a-zA-Z-]+-recommendations", e.getMessage());
        } catch (Error e) {
            fail();
        }
    }
}

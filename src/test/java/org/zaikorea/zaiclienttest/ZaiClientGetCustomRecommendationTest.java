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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.zaikorea.zaiclient.ZaiClient;
import org.zaikorea.zaiclient.exceptions.ZaiClientException;
import org.zaikorea.zaiclient.request.recommendations.GetCustomRecommendation;
import org.zaikorea.zaiclient.request.recommendations.RecommendationQuery;
import org.zaikorea.zaiclient.request.recommendations.RecommendationRequest;
import org.zaikorea.zaiclient.response.RecommendationResponse;

import com.google.gson.Gson;

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
    private static final String recLogRecommendations = "recommendations";

    private static final String illegalAccessExceptionMessage = "At least one of userId, itemId, or itemIds must be provided.";
    private static final String nullLimitExceptionMessage = "The value of limit must not be null";
    private static final String unprocessibleEntityExceptionMessage = "Unprocessable Entity";

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

    private void checkSuccessfulGetCustomRecommendation(RecommendationRequest recommendation, String userId,
            Metadata expectedMetadata) {
        RecommendationQuery recQuery = recommendation.getPayload();

        int limit = recQuery.getLimit();
        int offset = recQuery.getOffset();
        Gson gson = new Gson();

        try {
            RecommendationResponse response = testClientToDevEndpoint.sendRequest(recommendation);

            // Response Testing
            List<String> responseItems = response.getItems();
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
            assertNotEquals(logItem.size(), 0);
            assertEquals(logItem.get(
                    recLogRecommendations).split(",").length,
                    response.getItems().size());
            assertTrue(deleteRecLog(userId));

        } catch (IOException | ZaiClientException e) {
            fail();
        }
    }

    @Before
    public void setup() {
        testClientToDevEndpoint = new ZaiClient.Builder(clientId, clientSecret)
                .customEndpoint("dev")
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
            Metadata expectedMetadata = new Metadata();

            expectedMetadata.userId = userId;
            expectedMetadata.limit = limit;
            expectedMetadata.offset = offset;
            expectedMetadata.recommendationType = recommendationType;
            expectedMetadata.callType = recommendationType;

            checkSuccessfulGetCustomRecommendation(recommendation, userId, expectedMetadata);
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testGetRelatedRecommendation_2() {
        String itemId = TestUtils.generateUUID();

        int limit = TestUtils.generateRandomInteger(1, 10);

        String recommendationType = "homepage-main-recommendations";

        RecommendationRequest recommendation = new GetCustomRecommendation.Builder(recommendationType)
                .limit(limit)
                .itemId(itemId)
                .build();

        try {
            Metadata expectedMetadata = new Metadata(); // call_type and recommendation_type are same for custom
                                                        // recommendations
            expectedMetadata.itemId = itemId;
            expectedMetadata.limit = limit;
            expectedMetadata.recommendationType = recommendationType;
            expectedMetadata.callType = recommendationType;
            checkSuccessfulGetCustomRecommendation(recommendation, null, expectedMetadata);
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testGetRelatedRecommendation_3() {
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
            Metadata expectedMetadata = new Metadata();
            expectedMetadata.itemIds = itemIds;
            expectedMetadata.limit = itemIds.size();
            expectedMetadata.recommendationType = recommendationType;
            expectedMetadata.callType = recommendationType;

            checkSuccessfulGetCustomRecommendation(recommendation, null, expectedMetadata);
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testGetRelatedRecommendation_4() {
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
            Metadata expectedMetadata = new Metadata();
            expectedMetadata.userId = userId;
            expectedMetadata.itemId = itemId;
            expectedMetadata.itemIds = itemIds;
            expectedMetadata.limit = limit;
            expectedMetadata.recommendationType = recommendationType;
            expectedMetadata.callType = recommendationType;

            checkSuccessfulGetCustomRecommendation(recommendation, userId, expectedMetadata);
        } catch (Exception e) {
            fail();
        }
    }

    // Unhappy path
    @Test
    public void testGetRelatedRecommendationWithoutAnyInput() { // No ItemId, No UserId, No ItemIds
        try {
            new GetCustomRecommendation.Builder("homepage-main-recommendations")
                    .build();
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals(illegalAccessExceptionMessage, e.getMessage());
        } catch (Error e) {
            fail();
        }
    }

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
            Metadata expectedMetadata = new Metadata();
            expectedMetadata.itemIds = itemIds;
            expectedMetadata.offset = 0;
            checkSuccessfulGetCustomRecommendation(recommendation, null, expectedMetadata);

            fail();
        } catch (IllegalArgumentException e) {
            assertEquals(nullLimitExceptionMessage, e.getMessage());
        } catch (Error e) {
            fail();
        }
    }

    @Test
    public void testGetCustomRecommendationWithWrongRecommendationTypeFormat() {
        String recommendationType = "homepage-main-rec";

        try {
            new GetCustomRecommendation.Builder(recommendationType)
                    .build();
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("Recommendation Type must be in the format of [0-9a-zA-Z-]+-recommendations", e.getMessage());
        } catch (Error e) {
            fail();
        }
    }
}

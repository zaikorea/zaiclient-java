package org.zaikorea.zaiclienttest;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;

import java.util.concurrent.ThreadLocalRandom;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.zaikorea.zaiclient.ZaiClient;
import org.zaikorea.zaiclient.exceptions.ZaiClientException;
import org.zaikorea.zaiclient.request.events.AddCartaddEvent;
import org.zaikorea.zaiclient.request.events.AddCustomEvent;
import org.zaikorea.zaiclient.request.events.AddLikeEvent;
import org.zaikorea.zaiclient.request.events.AddPageViewEvent;
import org.zaikorea.zaiclient.request.events.AddProductDetailViewEvent;
import org.zaikorea.zaiclient.request.events.AddPurchaseEvent;
import org.zaikorea.zaiclient.request.events.AddRateEvent;
import org.zaikorea.zaiclient.request.events.AddSearchEvent;
import org.zaikorea.zaiclient.request.events.Event;
import org.zaikorea.zaiclient.request.events.EventRequest;
import org.zaikorea.zaiclient.response.EventLoggerResponse;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;

public class ZaiCilentEventTest {
    private static final String clientId = "test";
    private static final String clientSecret = "KVPzvdHTPWnt0xaEGc2ix-eqPXFCdEV5zcqolBr_h1k"; // this secret key is for
                                                                                              // testing purposes only
    private static final String eventTableName = "events_test";
    private static final String eventTablePartitionKey = "user_id";
    private static final String eventTableSortKey = "timestamp";
    private static final String eventTableItemIdKey = "item_id";
    private static final String eventTableEventTypeKey = "event_type";
    private static final String eventTableEventValueKey = "event_value";
    private static final String eventTableExpirationTimeKey = "expiration_time";
    private static final String eventTableIsZaiRecommendationKey = "is_zai_recommendation";
    private static final String eventTableFromKey = "from";
    private static final String eventTableUrlKey = "url";
    private static final String eventTableRefKey = "ref";
    private static final String eventTableRecommendationIdKey = "recommendation_id";
    private static final String eventTableEventPropertiesKey = "event_properties";
    private static final String eventTableUserPropertiesKey = "user_properties";

    private static final int defaultDataExpirationSeconds = 60 * 60 * 24 * 365; // 1 year

    private ZaiClient testClient;

    private static final Region region = Region.AP_NORTHEAST_2;
    private DynamoDbClient ddbClient;

    private String getUnixTimestatmp() {
        long utcnow = Instant.now().getEpochSecond();
        return Long.toString(utcnow);
    }

    private String generateUUID() {
        return UUID.randomUUID().toString();
    }

    private String generatePageType() {

        String[] products = { "homepage", "category", "today's pick" };
        int randomIndex = new Random().nextInt(products.length);

        return products[randomIndex];
    }

    private String generateSearchQuery() {

        String[] products = { "waterproof camera", "headphones with NAC", "book for coding" };
        int randomIndex = new Random().nextInt(products.length);

        return products[randomIndex];

    }

    private int generateRandomInteger(int min, int max) {
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    private double generateRandomDouble(int min, int max) {
        return ThreadLocalRandom.current().nextDouble(min, max);
    }

    private String generateRandomString(int n) {
        int index;
        char randomChar;
        String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < n; i++) {

            index = generateRandomInteger(0, alphabet.length() - 1);

            randomChar = alphabet.charAt(index);

            sb.append(randomChar);
        }

        return sb.toString();
    }

    private Map<String, String> getEventLog(String partitionValue) {

        String partitionAlias = "#pk";

        HashMap<String, String> attrNameAlias = new HashMap<>();
        attrNameAlias.put(partitionAlias, eventTablePartitionKey);
        HashMap<String, AttributeValue> attrValues = new HashMap<>();
        attrValues.put(":" + eventTablePartitionKey, AttributeValue.builder()
                .s(partitionValue)
                .build());

        QueryRequest request = QueryRequest.builder()
                .tableName(eventTableName)
                .keyConditionExpression(partitionAlias + " = :" + eventTablePartitionKey)
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

    private Map<String, String> getEventLogWithTimeStamp(String partitionValue, double sortValue) {
        HashMap<String, AttributeValue> keyToGet = new HashMap<>();
        BigDecimal sortValue_ = new BigDecimal(sortValue);
        keyToGet.put(eventTablePartitionKey, AttributeValue.builder()
                .s(partitionValue)
                .build());
        keyToGet.put(eventTableSortKey, AttributeValue.builder()
                .n(String.valueOf(sortValue_))
                .build());
        GetItemRequest request = GetItemRequest.builder().tableName(eventTableName).key(keyToGet).build();

        try {
            GetItemResponse returnedItems = ddbClient.getItem(request);
            Map<String, AttributeValue> returnedItem = returnedItems.item();
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

    private boolean deleteEventLog(String partitionValue) {

        String sortValue = getEventLog(partitionValue).get(eventTableSortKey);

        HashMap<String, AttributeValue> keyToGet = new HashMap<>();

        keyToGet.put(eventTablePartitionKey, AttributeValue.builder().s(partitionValue).build());
        keyToGet.put(eventTableSortKey, AttributeValue.builder().n(sortValue).build());

        DeleteItemRequest deleteReq = DeleteItemRequest.builder()
                .key(keyToGet)
                .tableName(eventTableName)
                .build();

        try {
            ddbClient.deleteItem(deleteReq);
        } catch (DynamoDbException e) {
            System.err.println(e.getMessage());
            return false;
        }

        return true;
    }

    private boolean deleteEventLogWithTimestamp(String partitionValue, double sortValue) {

        HashMap<String, AttributeValue> keyToGet = new HashMap<>();
        BigDecimal sortValue_ = new BigDecimal(sortValue);

        keyToGet.put(eventTablePartitionKey, AttributeValue.builder().s(partitionValue).build());
        keyToGet.put(eventTableSortKey, AttributeValue.builder().n(String.valueOf(sortValue_)).build());

        DeleteItemRequest deleteReq = DeleteItemRequest.builder()
                .key(keyToGet)
                .tableName(eventTableName)
                .build();

        try {
            ddbClient.deleteItem(deleteReq);
        } catch (DynamoDbException e) {
            System.err.println(e.getMessage());
            return false;
        }

        return true;
    }

    private void checkSuccessfulEventAdd(EventRequest eventRequest) {
        List<Event> events = eventRequest.getPayload();
        try {
            testClient.sendRequest(eventRequest);
        } catch (IOException | ZaiClientException e) {
            fail(e.getMessage());
        }

        for (Event event : events) {
            String userId = event.getUserId();
            double timestamp = event.getTimestamp();
            String itemId = event.getItemId();
            String eventType = event.getEventType();
            String eventValue = event.getEventValue();
            boolean isZaiRecommendation = event.getIsZaiRecommendation();
            String from = event.getFrom();
            String url = event.getUrl();
            String ref = event.getRef();
            String recommendationId = event.getRecommendationId();
            Map<String, ?> eventProperties = event.getEventProperties();
            Map<String, ?> userProperties = event.getUserProperties();

            // Map<String, String> logItem = getEventLog(userId);
            Map<String, String> logItem = getEventLogWithTimeStamp(userId, timestamp);

            assertNotNull(logItem);
            assertNotEquals(logItem.size(), 0);
            assertEquals(logItem.get(eventTablePartitionKey), userId);
            assertEquals(logItem.get(eventTableItemIdKey), itemId);
            assertEquals(Double.parseDouble(logItem.get(eventTableSortKey)), timestamp, 0.0001);
            assertEquals(logItem.get(eventTableEventTypeKey), eventType);
            assertEquals(logItem.get(eventTableEventValueKey), eventValue);
            assertEquals(Boolean.parseBoolean(logItem.get(eventTableIsZaiRecommendationKey)), isZaiRecommendation);
            assertEquals(logItem.get(eventTableFromKey), Objects.toString(from));
            assertEquals(logItem.get(eventTableUrlKey), Objects.toString(url));
            assertEquals(logItem.get(eventTableRefKey), Objects.toString(ref));
            assertEquals(logItem.get(eventTableRecommendationIdKey), Objects.toString(recommendationId));
            assertEquals(logItem.get(eventTableEventPropertiesKey), Objects.toString(eventProperties));
            assertEquals(logItem.get(eventTableUserPropertiesKey), Objects.toString(userProperties));
            assertTrue(deleteEventLogWithTimestamp(userId, timestamp));
        }
    }

    private void checkSuccessfulEventAdd(EventRequest eventRequest, boolean isTest) {
        List<Event> events = eventRequest.getPayload();
        EventLoggerResponse response;

        try {
            response = testClient.sendRequest(eventRequest, isTest);
        } catch (IOException | ZaiClientException e) {
            fail(e.getMessage());
            return;
        }

        for (Event event : events) {
            String userId = event.getUserId();
            double timestamp = event.getTimestamp();
            String itemId = event.getItemId();
            String eventType = event.getEventType();
            String eventValue = event.getEventValue();
            double serverTimestamp = response.getTimestamp();
            Integer timeToLive = event.getTimeToLive();
            boolean isZaiRecommendation = event.getIsZaiRecommendation();
            String from = event.getFrom();
            String url = event.getUrl();
            String ref = event.getRef();
            String recommendationId = event.getRecommendationId();
            Map<String, ?> eventProperties = event.getEventProperties();
            Map<String, ?> userProperties = event.getUserProperties();

            Map<String, String> logItem = getEventLog(userId);
            assertNotNull(logItem);
            assertNotEquals(logItem.size(), 0);
            assertEquals(logItem.get(eventTablePartitionKey), userId);
            assertEquals(logItem.get(eventTableItemIdKey), itemId);
            assertEquals(Double.parseDouble(logItem.get(eventTableSortKey)), timestamp, 0.0001);
            assertEquals(logItem.get(eventTableEventTypeKey), eventType);
            assertEquals(logItem.get(eventTableEventValueKey), eventValue);
            if (isTest) {
                assertEquals(Integer.parseInt(logItem.get(eventTableExpirationTimeKey)),
                        (int) (serverTimestamp + timeToLive));
            } else {
                assertEquals(Integer.parseInt(logItem.get(eventTableExpirationTimeKey)),
                        (int) (serverTimestamp + defaultDataExpirationSeconds));
            }
            assertEquals(Boolean.parseBoolean(logItem.get(eventTableIsZaiRecommendationKey)), isZaiRecommendation);
            assertEquals(logItem.get(eventTableFromKey), Objects.toString(from));
            assertEquals(logItem.get(eventTableUrlKey), Objects.toString(url));
            assertEquals(logItem.get(eventTableRefKey), Objects.toString(ref));
            assertEquals(logItem.get(eventTableRecommendationIdKey), Objects.toString(recommendationId));
            assertEquals(logItem.get(eventTableEventPropertiesKey), Objects.toString(eventProperties));
            assertEquals(logItem.get(eventTableUserPropertiesKey), Objects.toString(userProperties));
            assertTrue(deleteEventLogWithTimestamp(userId, timestamp));
        }
    }

    @Before
    public void setup() {
        testClient = new ZaiClient.Builder(clientId, clientSecret)
                .customEndpoint("dev")
                .connectTimeout(10)
                .readTimeout(30)
                .build();
        ddbClient = DynamoDbClient.builder()
                .region(region)
                .build();
    }

    @After
    public void cleanup() {
        ddbClient.close();
    }

    /**********************************
     *     AddProductDetailView     *
     **********************************/
    @Test
    public void testAddProductDetailView_1() {
        AddProductDetailViewEvent eventRequest = new AddProductDetailViewEvent.Builder(generateUUID(), generateUUID())
                .build();
        checkSuccessfulEventAdd(eventRequest);
    }

    @Test
    public void testAddProductDetailView_2() {
        AddProductDetailViewEvent eventRequest = new AddProductDetailViewEvent.Builder(generateUUID(), generateUUID())
                .build();
        checkSuccessfulEventAdd(eventRequest, true);
    }

    /**********************************
     *          AddPageView           *
     **********************************/
    @Test
    public void testAddPageViewEvent() {
        AddPageViewEvent eventRequest = new AddPageViewEvent.Builder(generateUUID(), generatePageType())
                .build();

        checkSuccessfulEventAdd(eventRequest);
    }

    /**********************************
     *          AddCartAddEvent       *
     **********************************/
    @Test
    public void testAddCartAddEvent() {
        AddCartaddEvent eventRequest = new AddCartaddEvent.Builder(generateUUID(), generateUUID())
                .build();

        checkSuccessfulEventAdd(eventRequest);
    }

    @Test
    public void testAddCartAddEventWithFrom() {
        AddCartaddEvent eventRequest = new AddCartaddEvent.Builder(generateUUID(), generateUUID())
                .from(generateRandomString(10))
                .build();

        checkSuccessfulEventAdd(eventRequest);
    }

    @Test
    public void testAddCartAddEventWithNewSchema() {
        Map<String, String> eventProperties = new HashMap<>();
        Map<String, String> userProperties = new HashMap<>();

        eventProperties.put("event_props_key1", "event_props_value1");
        eventProperties.put("event_props_key2", "event_props_value2");
        userProperties.put("user_props_key1", "user_props_value1");
        userProperties.put("user_props_key2", "user_props_value2");

        AddCartaddEvent eventRequest = new AddCartaddEvent.Builder(generateUUID(), generateUUID())
                .from(generateRandomString(10))
                .url("https://www.blux.ai/")
                .ref("https://www.zaikorea.org/")
                .recommendationId(generateUUID())
                .eventProperties(eventProperties)
                .userProperties(userProperties)
                .build();

        checkSuccessfulEventAdd(eventRequest);
    }

    /**********************************
     *          AddLikeEvent        *
     **********************************/
    @Test
    public void testAddLikeEvent() {
        AddLikeEvent eventRequest = new AddLikeEvent.Builder(generateUUID(), generateUUID())
                .build();

        checkSuccessfulEventAdd(eventRequest);
    }

    @Test
    public void testAddLikeEventWithFrom() {
        AddLikeEvent eventRequest = new AddLikeEvent.Builder(generateUUID(), generateUUID())
                .from(generateRandomString(10))
                .build();

        checkSuccessfulEventAdd(eventRequest);
    }

    @Test
    public void testAddLikeEventWithIsZaiRec() {
        AddLikeEvent eventRequest = new AddLikeEvent.Builder(generateUUID(), generateUUID())
                .isZaiRecommendation(true)
                .build();

        checkSuccessfulEventAdd(eventRequest);
    }

    @Test
    public void testAddLikeEventWithOptions() {
        AddLikeEvent eventRequest = new AddLikeEvent.Builder(generateUUID(), generateUUID())
                .from(generateRandomString(10))
                .isZaiRecommendation(true)
                .build();

        checkSuccessfulEventAdd(eventRequest);
    }

    /**********************************
     *           AddRateEvent         *
     **********************************/
    @Test
    public void testAddRateEvent() {
        AddRateEvent eventRequest = new AddRateEvent.Builder(generateUUID(), generateUUID(), generateRandomDouble(1, 5))
                .build();

        checkSuccessfulEventAdd(eventRequest);
    }

    /**********************************
     *         AddSearchEvent         *
     **********************************/
    @Test
    public void testSearchEvent() {
        AddSearchEvent eventRequest = new AddSearchEvent.Builder(generateUUID(), generateSearchQuery())
                .build();

        checkSuccessfulEventAdd(eventRequest);
    }

    /**********************************
     *        AddPurchaseEvent        *
     **********************************/
    @Test
    public void testAddPurchaseEvent() {
        AddPurchaseEvent eventRequest = new AddPurchaseEvent.Builder(generateUUID())
                .addPurchase(generateUUID(), generateRandomInteger(1, 1000000))
                .build();

        checkSuccessfulEventAdd(eventRequest);
    }

    @Test
    public void testAddPurchaseEventInBatch() {
        AddPurchaseEvent.Builder purchaseEventRequestBuilder = new AddPurchaseEvent.Builder(generateUUID());

        int numEvents = generateRandomInteger(1, 10);

        for (int i = 0; i < numEvents; i++) {
            purchaseEventRequestBuilder.addPurchase(generateUUID(), generateRandomInteger(1, 1000000));
        }

        AddPurchaseEvent purchaseEventRequest = purchaseEventRequestBuilder.build();

        checkSuccessfulEventAdd(purchaseEventRequest);
    }

    /**********************************
     *         AddCustomEvent         *
     **********************************/
    @Test
    public void testAddCustomEvent() {
        AddCustomEvent eventRequest = new AddCustomEvent.Builder(generateUUID(), generateRandomString(10))
                .addEventItem(generateUUID(), generateRandomString(10))
                .build();

        checkSuccessfulEventAdd(eventRequest);
    }

    @Test
    public void testAddCustomEventInBatch() {
        AddCustomEvent.Builder customEventRequestBuilder = new AddCustomEvent.Builder(generateUUID(),
                generateRandomString(10));

        int numEvents = generateRandomInteger(1, 10);

        for (int i = 0; i < numEvents; i++) {
            customEventRequestBuilder.addEventItem(generateUUID(), generateRandomString(10));
        }

        AddCustomEvent customEventRequest = customEventRequestBuilder.build();

        checkSuccessfulEventAdd(customEventRequest);
    }
}

package org.zaikorea.ZaiClientTest;

import java.io.IOException;
import java.security.InvalidParameterException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.zaikorea.ZaiClient.ZaiClient;
import org.zaikorea.ZaiClient.exceptions.ZaiClientException;
import org.zaikorea.ZaiClient.request.*;
import org.zaikorea.ZaiClient.response.EventLoggerResponse;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.model.*;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;


public class ZaiClientJavaTest {

    private static final String clientId = "test";
    private static final String clientSecret = "KVPzvdHTPWnt0xaEGc2ix-eqPXFCdEV5zcqolBr_h1k"; // this secret key is for testing purposes only
    private static final String eventTableName = "events_test";
    private static final String eventTablePartitionKey = "user_id";
    private static final String eventTableSortKey = "timestamp";
    private static final String eventTableItemIdKey = "item_id";
    private static final String eventTableEventTypeKey = "event_type";
    private static final String eventTableEventValueKey = "event_value";
    private static final String eventTableExpirationTimeKey = "expiration_time";
    private static final String eventTableIsZaiRecommendationKey = "is_zai_recommendation";
    private static final String eventTableFromKey = "from";

    private static final String incorrectCustomEndpointMsg = "Only alphanumeric characters are allowed for custom endpoint.";
    private static final String longLengthCustomEndpointMsg = "Custom endpoint should be less than or equal to 10.";

    private static final int defaultDataExpirationSeconds = 60 * 60 * 24 * 365; // 1 year

    private ZaiClient testClient;
    private ZaiClient incorrectIdClient;
    private ZaiClient incorrectSecretClient;

    private static final Region region = Region.AP_NORTHEAST_2;
    private DynamoDbClient ddbClient;

    private String getUnixTimestamp() {
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

        for(int i = 0; i < n; i++) {

            index = generateRandomInteger(0, alphabet.length()-1);

            randomChar = alphabet.charAt(index);

            sb.append(randomChar);
        }

        return sb.toString();
    }

    private Map<String, String> getEventLog(String partitionValue) {

        String partitionAlias = "#pk";

        HashMap<String,String> attrNameAlias = new HashMap<>();
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
                    String val = returnedItem.get(key).toString();
                    item.put(key, val.substring(val.indexOf("=") + 1, val.length() - 1));
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

    private void checkSuccessfulEventAdd(Event event) {
        try {
            testClient.addEventLog(event);
            String userId = event.getUserId();
            double timestamp = event.getTimestamp();
            String itemId = event.getItemId();
            String eventType = event.getEventType();
            String eventValue = event.getEventValue();
            boolean isZaiRecommendation = event.getIsZaiRecommendation();
            String from = event.getFrom();

            Map<String, String> logItem = getEventLog(userId);
            assertNotNull(logItem);
            assertNotEquals(logItem.size(), 0);
            assertEquals(logItem.get(eventTablePartitionKey), userId);
            assertEquals(logItem.get(eventTableItemIdKey), itemId);
            assertEquals(Double.parseDouble(logItem.get(eventTableSortKey)), timestamp, 0.0001);
            assertEquals(logItem.get(eventTableEventTypeKey), eventType);
            assertEquals(logItem.get(eventTableEventValueKey), eventValue);
            assertEquals(Boolean.parseBoolean(logItem.get(eventTableIsZaiRecommendationKey)), isZaiRecommendation);
            assertEquals(logItem.get(eventTableFromKey), from);
            assertTrue(deleteEventLog(userId));
        } catch (IOException | ZaiClientException e) {
            fail();
        }
    }

    private void checkSuccessfulEventAdd(Event event, boolean isTest) {
        try {
            EventLoggerResponse response = testClient.addEventLog(event, isTest);
            String userId = event.getUserId();
            double timestamp = event.getTimestamp();
            String itemId = event.getItemId();
            String eventType = event.getEventType();
            String eventValue = event.getEventValue();
            double serverTimestamp = response.getTimestamp();
            Integer timeToLive = event.getTimeToLive();
            boolean isZaiRecommendation = event.getIsZaiRecommendation();
            String from = event.getFrom();

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
            }
            else {
                assertEquals(Integer.parseInt(logItem.get(eventTableExpirationTimeKey)),
                        (int) (serverTimestamp + defaultDataExpirationSeconds));
            }
            assertEquals(Boolean.parseBoolean(logItem.get(eventTableIsZaiRecommendationKey)), isZaiRecommendation);
            assertEquals(logItem.get(eventTableFromKey), from);
            assertTrue(deleteEventLog(userId));
        } catch (IOException | ZaiClientException e) {
            fail();
        }
    }

    @Before
    public void setup() {
        testClient = new ZaiClient.Builder(clientId, clientSecret)
                .connectTimeout(10)
                .readTimeout(30)
                .customEndpoint("dev")
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
    public void testIncorrectCustomEndpointClient_1() {
        try {
           ZaiClient incorrectCustomEndpointClient =  new ZaiClient.Builder(clientId, clientSecret)
                   .customEndpoint("-@dev")
                   .build();
           fail();
        } catch(InvalidParameterException e) {
            assertEquals(e.getMessage(), incorrectCustomEndpointMsg);
        }
    }

    @Test
    public void testIncorrectCustomEndpointClient_2() {
        try {
            ZaiClient incorrectCustomEndpointClient =  new ZaiClient.Builder(clientId, clientSecret)
                    .customEndpoint("abcdefghijklmnop")
                    .build();
            fail();
        } catch(InvalidParameterException e) {
            assertEquals(e.getMessage(), longLengthCustomEndpointMsg);
        }
    }


    /**********************************
     *     ProductDetailViewEvent     *
     **********************************/
    @Test
    public void testAddProductDetailViewEvent() {
        String userId = generateUUID();
        String itemId = generateUUID();

        Event event = new ProductDetailViewEvent(userId, itemId);
        checkSuccessfulEventAdd(event);
    }

    @Test
    public void testAddProductDetailViewEventWithIsZaiRec() {
        String userId = generateUUID();
        String itemId = generateUUID();

        Event event = new ProductDetailViewEvent(userId, itemId).setIsZaiRec(true);
        checkSuccessfulEventAdd(event);
    }

    @Test
    public void testAddProductDetailViewEventWithFrom() {
        String userId = generateUUID();
        String itemId = generateUUID();

        Event event = new ProductDetailViewEvent(userId, itemId).setFrom("home");
        checkSuccessfulEventAdd(event);
    }

    @Test
    public void testAddTrueTestProductDetailViewEvent() {
        String userId = generateUUID();
        String itemId = generateUUID();

        Event event = new ProductDetailViewEvent(userId, itemId);
        checkSuccessfulEventAdd(event, true);
    }

    @Test
    public void testAddFalseTestProductDetailViewEvent() {
        String userId = generateUUID();
        String itemId = generateUUID();

        Event event = new ProductDetailViewEvent(userId, itemId);
        checkSuccessfulEventAdd(event, false);
    }

    @Test
    public void testAddProductDetailViewEventManualTime() {
        String userId = generateUUID();
        String itemId = generateUUID();
        long timestamp = Long.parseLong(getUnixTimestamp());

        Event event = new ProductDetailViewEvent(userId, itemId).setTimestamp(timestamp);
        checkSuccessfulEventAdd(event);
    }

    @Test
    public void testAddProductDetailViewEventWrongClientId() {
        String userId = generateUUID();
        String itemId = generateUUID();
        long timestamp = Long.parseLong(getUnixTimestamp());

        Event event = new ProductDetailViewEvent(userId, itemId).setTimestamp(timestamp);
        try {
            incorrectIdClient.addEventLog(event);
        } catch (IOException e) {
            fail();
        } catch (ZaiClientException e) {
            assertEquals(e.getHttpStatusCode(), 401);
        }
    }

    @Test
    public void testAddProductDetailViewEventWrongSecret() {
        String userId = generateUUID();
        String itemId = generateUUID();
        long timestamp = Long.parseLong(getUnixTimestamp());

        Event event = new ProductDetailViewEvent(userId, itemId).setTimestamp(timestamp);
        try {
            incorrectSecretClient.addEventLog(event);
        } catch (IOException e) {
            fail();
        } catch (ZaiClientException e) {
            assertEquals(e.getHttpStatusCode(), 401);
        }
    }

    /**********************************
     *           LikeEvent            *
     **********************************/
    @Test
    public void testAddLikeEvent() {
        String userId = generateUUID();
        String itemId = generateUUID();

        Event event = new LikeEvent(userId, itemId);
        checkSuccessfulEventAdd(event);
    }

    @Test
    public void testAddLikeEventWithIsZaiRec() {
        String userId = generateUUID();
        String itemId = generateUUID();

        Event event = new LikeEvent(userId, itemId).setIsZaiRec(true);
        checkSuccessfulEventAdd(event);
    }

    @Test
    public void testAddLikeEventWithFrom() {
        String userId = generateUUID();
        String itemId = generateUUID();

        Event event = new LikeEvent(userId, itemId).setFrom("home");
        checkSuccessfulEventAdd(event);
    }

    @Test
    public void testAddTrueTestLikeEvent() {
        String userId = generateUUID();
        String itemId = generateUUID();

        Event event = new LikeEvent(userId, itemId);
        checkSuccessfulEventAdd(event, true);
    }

    @Test
    public void testAddFalseTestLikeEvent() {
        String userId = generateUUID();
        String itemId = generateUUID();

        Event event = new LikeEvent(userId, itemId);
        checkSuccessfulEventAdd(event, false);
    }

    @Test
    public void testAddLikeEventManualTime() {
        String userId = generateUUID();
        String itemId = generateUUID();
        long timestamp = Long.parseLong(getUnixTimestamp());

        Event event = new LikeEvent(userId, itemId).setTimestamp(timestamp);
        checkSuccessfulEventAdd(event);
    }

    @Test
    public void testAddLikeEventWrongClientId() {
        String userId = generateUUID();
        String itemId = generateUUID();
        long timestamp = Long.parseLong(getUnixTimestamp());

        Event event = new LikeEvent(userId, itemId).setTimestamp(timestamp);
        try {
            incorrectIdClient.addEventLog(event);
        } catch (IOException e) {
            fail();
        } catch (ZaiClientException e) {
            assertEquals(e.getHttpStatusCode(), 401);
        }
    }

    @Test
    public void testAddLikeEventWrongSecret() {
        String userId = generateUUID();
        String itemId = generateUUID();
        long timestamp = Long.parseLong(getUnixTimestamp());

        Event event = new LikeEvent(userId, itemId).setTimestamp(timestamp);
        try {
            incorrectSecretClient.addEventLog(event);
        } catch (IOException e) {
            fail();
        } catch (ZaiClientException e) {
            assertEquals(e.getHttpStatusCode(), 401);
        }
    }

    /**********************************
     *         PageViewEvent          *
     **********************************/
    @Test
    public void testAddPageViewEvent() {
        String userId = generateUUID();
        String pageType = generatePageType();

        Event event = new PageViewEvent(userId, pageType);
        checkSuccessfulEventAdd(event);
    }

    @Test
    public void testAddPageViewEventWithContainsZaiRec() {
        String userId = generateUUID();
        String pageType = generatePageType();

        Event event = new PageViewEvent(userId, pageType).setContainsZaiRec(true);
        checkSuccessfulEventAdd(event);
    }
    
    @Test
    public void testAddTrueTestPageViewEvent() {
        String userId = generateUUID();
        String itemId = generateUUID();

        Event event = new PageViewEvent(userId, itemId);
        checkSuccessfulEventAdd(event, true);
    }

    @Test
    public void testAddFalseTestPageViewEvent() {
        String userId = generateUUID();
        String itemId = generateUUID();

        Event event = new PageViewEvent(userId, itemId);
        checkSuccessfulEventAdd(event, false);
    }

    @Test
    public void testAddPageViewEventManualTime() {
        String userId = generateUUID();
        String pageType = generatePageType();
        long timestamp = Long.parseLong(getUnixTimestamp());

        Event event = new PageViewEvent(userId, pageType).setTimestamp(timestamp);
        checkSuccessfulEventAdd(event);
    }

    @Test
    public void testAddPageViewEventWrongClientId() {
        String userId = generateUUID();
        String pageType = generatePageType();
        long timestamp = Long.parseLong(getUnixTimestamp());

        Event event = new PageViewEvent(userId, pageType).setTimestamp(timestamp);
        try {
            incorrectIdClient.addEventLog(event);
        } catch (IOException e) {
            fail();
        } catch (ZaiClientException e) {
            assertEquals(e.getHttpStatusCode(), 401);
        }
    }

    @Test
    public void testAddPageViewEventWrongSecret() {
        String userId = generateUUID();
        String pageType = generatePageType();
        long timestamp = Long.parseLong(getUnixTimestamp());

        Event event = new PageViewEvent(userId, pageType).setTimestamp(timestamp);
        try {
            incorrectSecretClient.addEventLog(event);
        } catch (IOException e) {
            fail();
        } catch (ZaiClientException e) {
            assertEquals(e.getHttpStatusCode(), 401);
        }
    }

    /**********************************
     *          SearchEvent           *
     **********************************/
    @Test
    public void testAddSearchEvent() {
        String userId = generateUUID();
        String searchQuery = generateSearchQuery();

        Event event = new SearchEvent(userId, searchQuery);
        checkSuccessfulEventAdd(event);
    }

    @Test
    public void testAddSearchEventWithIsZaiRec() {
        String userId = generateUUID();
        String searchQuery = generateSearchQuery();

        Event event = new SearchEvent(userId, searchQuery).setIsZaiRec(true);
        checkSuccessfulEventAdd(event);
    }

    @Test
    public void testAddTrueTestSearchEvent() {
        String userId = generateUUID();
        String itemId = generateUUID();

        Event event = new SearchEvent(userId, itemId);
        checkSuccessfulEventAdd(event, true);
    }

    @Test
    public void testAddFalseTestSearchEvent() {
        String userId = generateUUID();
        String itemId = generateUUID();

        Event event = new SearchEvent(userId, itemId);
        checkSuccessfulEventAdd(event, false);
    }

    @Test
    public void testAddSearchEventManualTime() {
        String userId = generateUUID();
        String searchQuery = generateSearchQuery();
        long timestamp = Long.parseLong(getUnixTimestamp());

        Event event = new SearchEvent(userId, searchQuery).setTimestamp(timestamp);
        checkSuccessfulEventAdd(event);
    }

    @Test
    public void testAddSearchEventWrongClientId() {
        String userId = generateUUID();
        String searchQuery = generateSearchQuery();
        long timestamp = Long.parseLong(getUnixTimestamp());

        Event event = new SearchEvent(userId, searchQuery).setTimestamp(timestamp);
        try {
            incorrectIdClient.addEventLog(event);
        } catch (IOException e) {
            fail();
        } catch (ZaiClientException e) {
            assertEquals(e.getHttpStatusCode(), 401);
        }
    }

    @Test
    public void testAddSearchEventWrongSecret() {
        String userId = generateUUID();
        String searchQuery = generateSearchQuery();
        long timestamp = Long.parseLong(getUnixTimestamp());

        Event event = new SearchEvent(userId, searchQuery).setTimestamp(timestamp);
        try {
            incorrectSecretClient.addEventLog(event);
        } catch (IOException e) {
            fail();
        } catch (ZaiClientException e) {
            assertEquals(e.getHttpStatusCode(), 401);
        }
    }

    /**********************************
     *          CartaddEvent          *
     **********************************/
    @Test
    public void testAddCartaddEvent() {
        String userId = generateUUID();
        String itemId = generateUUID();

        Event event = new CartaddEvent(userId, itemId);
        checkSuccessfulEventAdd(event);
    }

    @Test
    public void testAddCartaddEventWithIsZaiRec() {
        String userId = generateUUID();
        String itemId = generateUUID();

        Event event = new CartaddEvent(userId, itemId).setIsZaiRec(true);
        checkSuccessfulEventAdd(event);
    }

    @Test
    public void testAddCartaddEventWithFrom() {
        String userId = generateUUID();
        String itemId = generateUUID();

        Event event = new CartaddEvent(userId, itemId).setFrom("home");
        checkSuccessfulEventAdd(event);
    }

    @Test
    public void testAddTrueTestCartaddEvent() {
        String userId = generateUUID();
        String itemId = generateUUID();

        Event event = new CartaddEvent(userId, itemId);
        checkSuccessfulEventAdd(event, true);
    }

    @Test
    public void testAddFalseTestCartaddEvent() {
        String userId = generateUUID();
        String itemId = generateUUID();

        Event event = new CartaddEvent(userId, itemId);
        checkSuccessfulEventAdd(event, false);
    }

    @Test
    public void testAddCartaddEventManualTime() {
        String userId = generateUUID();
        String itemId = generateUUID();
        long timestamp = Long.parseLong(getUnixTimestamp());

        Event event = new CartaddEvent(userId, itemId).setTimestamp(timestamp);
        checkSuccessfulEventAdd(event);
    }

    @Test
    public void testAddCartaddEventWrongClientId() {
        String userId = generateUUID();
        String itemId = generateUUID();
        long timestamp = Long.parseLong(getUnixTimestamp());

        Event event = new CartaddEvent(userId, itemId).setTimestamp(timestamp);
        try {
            incorrectIdClient.addEventLog(event);
        } catch (IOException e) {
            fail();
        } catch (ZaiClientException e) {
            assertEquals(e.getHttpStatusCode(), 401);
        }
    }

    @Test
    public void testAddCartaddEventWrongSecret() {
        String userId = generateUUID();
        String itemId = generateUUID();
        long timestamp = Long.parseLong(getUnixTimestamp());

        Event event = new CartaddEvent(userId, itemId).setTimestamp(timestamp);
        try {
            incorrectSecretClient.addEventLog(event);
        } catch (IOException e) {
            fail();
        } catch (ZaiClientException e) {
            assertEquals(e.getHttpStatusCode(), 401);
        }
    }

    /**********************************
     *           RateEvent            *
     **********************************/
    @Test
    public void testAddRateEvent() {
        String userId = generateUUID();
        String itemId = generateUUID();
        double rating = generateRandomDouble(0, 5);

        Event event = new RateEvent(userId, itemId, rating);
        checkSuccessfulEventAdd(event);
    }

    @Test
    public void testAddRateEventWithIsZaiRec() {
        String userId = generateUUID();
        String itemId = generateUUID();
        double rating = generateRandomDouble(0, 5);

        Event event = new RateEvent(userId, itemId, rating).setIsZaiRec(true);
        checkSuccessfulEventAdd(event);
    }

    @Test
    public void testAddTrueTestRateEvent() {
        String userId = generateUUID();
        String itemId = generateUUID();
        double rating = generateRandomDouble(0, 5);

        Event event = new RateEvent(userId, itemId, rating);
        checkSuccessfulEventAdd(event, true);
    }

    @Test
    public void testAddFalseTestRateEvent() {
        String userId = generateUUID();
        String itemId = generateUUID();
        double rating = generateRandomDouble(0, 5);

        Event event = new RateEvent(userId, itemId, rating);
        checkSuccessfulEventAdd(event, false);
    }

    @Test
    public void testAddRateEventManualTime() {
        String userId = generateUUID();
        String itemId = generateUUID();
        double rating = generateRandomDouble(0, 5);
        long timestamp = Long.parseLong(getUnixTimestamp());

        Event event = new RateEvent(userId, itemId, rating).setTimestamp(timestamp);
        checkSuccessfulEventAdd(event);
    }

    @Test
    public void testAddRateEventWrongClientId() {
        String userId = generateUUID();
        String itemId = generateUUID();
        double rating = generateRandomDouble(0, 5);
        long timestamp = Long.parseLong(getUnixTimestamp());

        Event event = new RateEvent(userId, itemId, rating).setTimestamp(timestamp);
        try {
            incorrectIdClient.addEventLog(event);
        } catch (IOException e) {
            fail();
        } catch (ZaiClientException e) {
            assertEquals(e.getHttpStatusCode(), 401);
        }
    }

    @Test
    public void testAddRateEventWrongSecret() {
        String userId = generateUUID();
        String itemId = generateUUID();
        double rating = generateRandomDouble(0, 5);
        long timestamp = Long.parseLong(getUnixTimestamp());

        Event event = new RateEvent(userId, itemId, rating).setTimestamp(timestamp);
        try {
            incorrectSecretClient.addEventLog(event);
        } catch (IOException e) {
            fail();
        } catch (ZaiClientException e) {
            assertEquals(e.getHttpStatusCode(), 401);
        }
    }

    /**********************************
     *         PurchaseEvent          *
     **********************************/
    @Test
    public void testAddPurchaseEvent() {
        String userId = generateUUID();
        String itemId = generateUUID();
        int price = generateRandomInteger(10000, 100000);

        Event event = new PurchaseEvent(userId, itemId, price);
        checkSuccessfulEventAdd(event);
    }

    @Test
    public void testAddPurchaseEventWithIsZaiRec() {
        String userId = generateUUID();
        String itemId = generateUUID();
        int price = generateRandomInteger(10000, 100000);

        Event event = new PurchaseEvent(userId, itemId, price).setIsZaiRec(true);
        checkSuccessfulEventAdd(event);
    }

    @Test
    public void testAddTrueTestPurchaseEvent() {
        String userId = generateUUID();
        String itemId = generateUUID();
        int price = generateRandomInteger(10000, 100000);

        Event event = new PurchaseEvent(userId, itemId, price);
        checkSuccessfulEventAdd(event, true);
    }

    @Test
    public void testAddFalseTestPurchaseEvent() {
        String userId = generateUUID();
        String itemId = generateUUID();
        int price = generateRandomInteger(10000, 100000);

        Event event = new PurchaseEvent(userId, itemId, price);
        checkSuccessfulEventAdd(event, false);
    }

    @Test
    public void testAddPurchaseEventManualTime() {
        String userId = generateUUID();
        String itemId = generateUUID();
        int price = generateRandomInteger(10000, 100000);
        long timestamp = Long.parseLong(getUnixTimestamp());

        Event event = new PurchaseEvent(userId, itemId, price).setTimestamp(timestamp);
        checkSuccessfulEventAdd(event);
    }

    @Test
    public void testAddPurchaseEventWrongClientId() {
        String userId = generateUUID();
        String itemId = generateUUID();
        int price = generateRandomInteger(10000, 100000);
        long timestamp = Long.parseLong(getUnixTimestamp());

        Event event = new PurchaseEvent(userId, itemId, price).setTimestamp(timestamp);
        try {
            incorrectIdClient.addEventLog(event);
        } catch (IOException e) {
            fail();
        } catch (ZaiClientException e) {
            assertEquals(e.getHttpStatusCode(), 401);
        }
    }

    @Test
    public void testAddPurchaseEventWrongSecret() {
        String userId = generateUUID();
        String itemId = generateUUID();
        int price = generateRandomInteger(10000, 100000);
        long timestamp = Long.parseLong(getUnixTimestamp());

        Event event = new PurchaseEvent(userId, itemId, price).setTimestamp(timestamp);
        try {
            incorrectSecretClient.addEventLog(event);
        } catch (IOException e) {
            fail();
        } catch (ZaiClientException e) {
            assertEquals(e.getHttpStatusCode(), 401);
        }
    }

    /**********************************
     *           CustomEvent          *
     **********************************/
    @Test
    public void testAddCustomEvent() {
        String userId = generateUUID();
        String itemId = generateUUID();
        String eventType = "customEventType";
        String eventValue = "customEventValue";

        Event event = new CustomEvent(userId, itemId, eventType, eventValue);
        checkSuccessfulEventAdd(event);
    }

    @Test
    public void testAddCustomEventWithIsZaiRec() {
        String userId = generateUUID();
        String itemId = generateUUID();
        String eventType = "customEventType";
        String eventValue = "customEventValue";

        Event event = new CustomEvent(userId, itemId, eventType, eventValue).setIsZaiRec(true);
        checkSuccessfulEventAdd(event);
    }

    @Test
    public void testAddCustomEventWithFrom() {
        String userId = generateUUID();
        String itemId = generateUUID();
        String eventType = "customEventType";
        String eventValue = "customEventValue";

        Event event = new CustomEvent(userId, itemId, eventType, eventValue).setFrom("home");
        checkSuccessfulEventAdd(event);
    }

    @Test
    public void testAddTrueTestCustomEvent() {
        String userId = generateUUID();
        String itemId = generateUUID();
        String eventType = "customEventType";
        String eventValue = "customEventValue";

        Event event = new CustomEvent(userId, itemId, eventType, eventValue);
        checkSuccessfulEventAdd(event, true);
    }

    @Test
    public void testAddFalseCustomEvent() {
        String userId = generateUUID();
        String itemId = generateUUID();
        String eventType = "customEventType";
        String eventValue = "customEventValue";

        Event event = new CustomEvent(userId, itemId, eventType, eventValue);
        checkSuccessfulEventAdd(event, false);
    }

    @Test
    public void testAddCustomEventManualTime() {
        String userId = generateUUID();
        String itemId = generateUUID();
        String eventType = "customEventType";
        String eventValue = "customEventValue";
        long timestamp = Long.parseLong(getUnixTimestamp());

        Event event = new CustomEvent(userId, itemId, eventType, eventValue).setTimestamp(timestamp);
        checkSuccessfulEventAdd(event);
    }

    @Test
    public void testLongUserId() {
        String userId = generateRandomString(501);
        String itemId = generateUUID();
        String eventType = "customEventType";
        String eventValue = "customEventValue";
        try {
            Event event = new CustomEvent(userId, itemId, eventType, eventValue);
        } catch(InvalidParameterException e) {
            return ;
        }
        fail();
    }

    @Test
    public void testZeroLengthUserId() {
        String userId = "";
        String itemId = generateUUID();
        String eventType = generateUUID();
        String eventValue = generateUUID();
        try {
            Event event = new CustomEvent(userId, itemId, eventType, eventValue);
        } catch(InvalidParameterException e) {
            return ;
        }
        fail();
    }

    @Test
    public void testLongItemId() {
        String userId = generateUUID();
        String itemId = generateRandomString(501);
        String eventType = generateUUID();
        String eventValue = generateUUID();
        try {
            Event event = new CustomEvent(userId, itemId, eventType, eventValue);
        } catch(InvalidParameterException e) {
            return ;
        }
        fail();
    }

    @Test
    public void testZeroLengthItemId() {
        String userId = generateUUID();
        String itemId = "";
        String eventType = "customEventType";
        String eventValue = "customEventValue";
        try {
            Event event = new CustomEvent(userId, itemId, eventType, eventValue);
        } catch(InvalidParameterException e) {
            return ;
        }
        fail();
    }

    @Test
    public void testLongEventType() {
        String userId = generateUUID();
        String itemId = generateUUID();
        String eventType = generateRandomString(501);
        String eventValue = generateUUID();
        try {
            Event event = new CustomEvent(userId, itemId, eventType, eventValue);
        } catch(InvalidParameterException e) {
            return ;
        }
        fail();
    }

    @Test
    public void testZeroLengthEventType() {
        String userId = generateUUID();
        String itemId = generateUUID();
        String eventType = "";
        String eventValue = generateUUID();
        try {
            Event event = new CustomEvent(userId, itemId, eventType, eventValue);
        } catch(InvalidParameterException e) {
            return ;
        }
        fail();
    }
        @Test
    public void testLongEventValue() {
        String userId = generateUUID();
        String itemId = generateUUID();
        String eventType = generateUUID();
        String eventValue = generateRandomString(501);
        Event event = new CustomEvent(userId, itemId, eventType, eventValue);
        assertEquals(500, event.getEventValue().length());
    }

    @Test
    public void testZeroLengthEventValue() {
        String userId = generateUUID();
        String itemId = generateUUID();
        String eventType = generateUUID();
        String eventValue = "";
        try {
            Event event = new CustomEvent(userId, itemId, eventType, eventValue);
        } catch(InvalidParameterException e) {
            return ;
        }
        fail();
    }

    @Test
    public void testNegativeTimeToLive() {
        String userId = generateUUID();
        String itemId = generateUUID();
        String eventType = generateUUID();
        String eventValue = "";
        try {
            Event event = new CustomEvent(userId, itemId, eventType, eventValue);
            event.setTimeToLive(-defaultDataExpirationSeconds);
        } catch(InvalidParameterException e) {
            return ;
        }
        fail();
    }

    @Test
    public void testLongFromValue() {
        String userId = generateUUID();
        String itemId = generateUUID();
        String eventType = generateUUID();
        String eventValue = generateUUID();
        String from = generateRandomString(501);
        try {
            Event event = new CustomEvent(userId, itemId, eventType, eventValue).setFrom(from);
        } catch(InvalidParameterException e) {
            return ;
        }
        fail();
    }
}

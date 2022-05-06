package org.zaikorea.ZaiClientTest;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.HashMap;
import java.util.UUID;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.zaikorea.ZaiClient.ZaiClient;
import org.zaikorea.ZaiClient.exceptions.ZaiClientException;
import org.zaikorea.ZaiClient.request.*;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.model.*;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;


public class ZaiClientTest {

    private static final String clientId = "test";
    private static final String clientSecret = "KVPzvdHTPWnt0xaEGc2ix-eqPXFCdEV5zcqolBr_h1k"; // this secret key is for testing purposes only
    private static final String eventTableName = "events_test";
    private static final String eventTablePartitionKey = "user_id";
    private static final String eventTableSortKey = "timestamp";
    private static final String eventTableItemIdKey = "item_id";
    private static final String eventTableEventTypeKey = "event_type";
    private static final String eventTableEventValueKey = "event_value";

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

    private int generateRandomInteger(int min, int max) {
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    private double generateRandomDouble(int min, int max) {
        return ThreadLocalRandom.current().nextDouble(min, max);
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

            Map<String, String> logItem = getEventLog(userId);
            assertNotNull(logItem);
            assertNotEquals(logItem.size(), 0);
            assertEquals(logItem.get(eventTablePartitionKey), userId);
            assertEquals(logItem.get(eventTableItemIdKey), itemId);
            assertEquals(Double.parseDouble(logItem.get(eventTableSortKey)), timestamp, 0.0001);
            assertEquals(logItem.get(eventTableEventTypeKey), eventType);
            assertEquals(logItem.get(eventTableEventValueKey), eventValue);
            assertTrue(deleteEventLog(userId));
        } catch (IOException | ZaiClientException e) {
            fail();
        }
    }

    private void checkSuccessfulEventUpdate(Event oldEvent, Event newEvent) {
        assertEquals(oldEvent.getUserId(), newEvent.getUserId());
        assertEquals(oldEvent.getTimestamp(), newEvent.getTimestamp(), 0.0001);

        try {
            testClient.addEventLog(oldEvent);
            String userId = oldEvent.getUserId();
            double timestamp = oldEvent.getTimestamp();
            String itemId = oldEvent.getItemId();
            String eventType = oldEvent.getEventType();
            String eventValue = oldEvent.getEventValue();

            Map<String, String> logItem = getEventLog(userId);
            assertNotNull(logItem);
            assertNotEquals(logItem.size(), 0);
            assertEquals(logItem.get(eventTablePartitionKey), userId);
            assertEquals(logItem.get(eventTableItemIdKey), itemId);
            assertEquals(Double.parseDouble(logItem.get(eventTableSortKey)), timestamp, 0.0001);
            assertEquals(logItem.get(eventTableEventTypeKey), eventType);
            assertEquals(logItem.get(eventTableEventValueKey), eventValue);

            testClient.updateEventLog(newEvent);
            userId = newEvent.getUserId();
            timestamp = newEvent.getTimestamp();
            itemId = newEvent.getItemId();
            eventType = newEvent.getEventType();
            eventValue = newEvent.getEventValue();

            logItem = getEventLog(userId);
            assertNotNull(logItem);
            assertNotEquals(logItem.size(), 0);
            assertEquals(logItem.get(eventTablePartitionKey), userId);
            assertEquals(logItem.get(eventTableItemIdKey), itemId);
            assertEquals(Double.parseDouble(logItem.get(eventTableSortKey)), timestamp, 0.0001);
            assertEquals(logItem.get(eventTableEventTypeKey), eventType);
            assertEquals(logItem.get(eventTableEventValueKey), eventValue);

            assertTrue(deleteEventLog(userId));
        } catch (IOException | ZaiClientException e) {
            fail();
        }
    }

    private void checkSuccessfulEventDelete(Event event) {
        try {
            testClient.addEventLog(event);
            String userId = event.getUserId();
            double timestamp = event.getTimestamp();
            String itemId = event.getItemId();
            String eventType = event.getEventType();
            String eventValue = event.getEventValue();

            Map<String, String> logItem = getEventLog(userId);
            assertNotNull(logItem);
            assertNotEquals(logItem.size(), 0);
            assertEquals(logItem.get(eventTablePartitionKey), userId);
            assertEquals(logItem.get(eventTableItemIdKey), itemId);
            assertEquals(Double.parseDouble(logItem.get(eventTableSortKey)), timestamp, 0.0001);
            assertEquals(logItem.get(eventTableEventTypeKey), eventType);
            assertEquals(logItem.get(eventTableEventValueKey), eventValue);

            testClient.deleteEventLog(event);

            Map<String, String> newLogItem = getEventLog(userId);
            assertNotNull(newLogItem);
            assertEquals(newLogItem.size(), 0);
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
    public void testAddViewEvent() {
        String userId = generateUUID();
        String itemId = generateUUID();

        Event event = new ViewEvent(userId, itemId);
        checkSuccessfulEventAdd(event);
    }

    @Test
    public void testAddViewEventManualTime() {
        String userId = generateUUID();
        String itemId = generateUUID();
        long timestamp = Long.parseLong(getUnixTimestamp());

        Event event = new ViewEvent(userId, itemId, timestamp);
        checkSuccessfulEventAdd(event);
    }

    @Test
    public void testAddViewEventWrongClientId() {
        String userId = generateUUID();
        String itemId = generateUUID();
        long timestamp = Long.parseLong(getUnixTimestamp());

        Event event = new ViewEvent(userId, itemId, timestamp);
        try {
            incorrectIdClient.addEventLog(event);
        } catch (IOException e) {
            fail();
        } catch (ZaiClientException e) {
            assertEquals(e.getHttpStatusCode(), 401);
        }
    }

    @Test
    public void testAddViewEventWrongSecret() {
        String userId = generateUUID();
        String itemId = generateUUID();
        long timestamp = Long.parseLong(getUnixTimestamp());

        Event event = new ViewEvent(userId, itemId, timestamp);
        try {
            incorrectSecretClient.addEventLog(event);
        } catch (IOException e) {
            fail();
        } catch (ZaiClientException e) {
            assertEquals(e.getHttpStatusCode(), 401);
        }
    }

    @Test
    public void testUpdateViewEvent() {
        String userId = generateUUID();
        String oldItemId = generateUUID();
        String newItemId = generateUUID();

        Event oldEvent = new ViewEvent(userId, oldItemId);
        Event newEvent = new ViewEvent(userId, newItemId, oldEvent.getTimestamp());
        checkSuccessfulEventUpdate(oldEvent, newEvent);
    }

    @Test
    public void testDeleteViewEvent() {
        String userId = generateUUID();
        String itemId = generateUUID();

        Event event = new ViewEvent(userId, itemId);
        checkSuccessfulEventDelete(event);
    }

    @Test
    public void testAddLikeEvent() {
        String userId = generateUUID();
        String itemId = generateUUID();

        Event event = new LikeEvent(userId, itemId);
        checkSuccessfulEventAdd(event);
    }

    @Test
    public void testAddLikeEventManualTime() {
        String userId = generateUUID();
        String itemId = generateUUID();
        long timestamp = Long.parseLong(getUnixTimestamp());

        Event event = new LikeEvent(userId, itemId, timestamp);
        checkSuccessfulEventAdd(event);
    }

    @Test
    public void testAddLikeEventWrongClientId() {
        String userId = generateUUID();
        String itemId = generateUUID();
        long timestamp = Long.parseLong(getUnixTimestamp());

        Event event = new LikeEvent(userId, itemId, timestamp);
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

        Event event = new LikeEvent(userId, itemId, timestamp);
        try {
            incorrectSecretClient.addEventLog(event);
        } catch (IOException e) {
            fail();
        } catch (ZaiClientException e) {
            assertEquals(e.getHttpStatusCode(), 401);
        }
    }

    @Test
    public void testUpdateLikeEvent() {
        String userId = generateUUID();
        String oldItemId = generateUUID();
        String newItemId = generateUUID();

        Event oldEvent = new LikeEvent(userId, oldItemId);
        Event newEvent = new LikeEvent(userId, newItemId, oldEvent.getTimestamp());
        checkSuccessfulEventUpdate(oldEvent, newEvent);
    }

    @Test
    public void testDeleteLikeEvent() {
        String userId = generateUUID();
        String itemId = generateUUID();

        Event event = new LikeEvent(userId, itemId);
        checkSuccessfulEventDelete(event);
    }

    @Test
    public void testAddCartaddEvent() {
        String userId = generateUUID();
        String itemId = generateUUID();

        Event event = new CartaddEvent(userId, itemId);
        checkSuccessfulEventAdd(event);
    }

    @Test
    public void testAddCartaddEventManualTime() {
        String userId = generateUUID();
        String itemId = generateUUID();
        long timestamp = Long.parseLong(getUnixTimestamp());

        Event event = new CartaddEvent(userId, itemId, timestamp);
        checkSuccessfulEventAdd(event);
    }

    @Test
    public void testAddCartaddEventWrongClientId() {
        String userId = generateUUID();
        String itemId = generateUUID();
        long timestamp = Long.parseLong(getUnixTimestamp());

        Event event = new CartaddEvent(userId, itemId, timestamp);
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

        Event event = new CartaddEvent(userId, itemId, timestamp);
        try {
            incorrectSecretClient.addEventLog(event);
        } catch (IOException e) {
            fail();
        } catch (ZaiClientException e) {
            assertEquals(e.getHttpStatusCode(), 401);
        }
    }

    @Test
    public void testUpdateCartaddEvent() {
        String userId = generateUUID();
        String oldItemId = generateUUID();
        String newItemId = generateUUID();

        Event oldEvent = new CartaddEvent(userId, oldItemId);
        Event newEvent = new CartaddEvent(userId, newItemId, oldEvent.getTimestamp());
        checkSuccessfulEventUpdate(oldEvent, newEvent);
    }

    @Test
    public void testDeleteCartaddEvent() {
        String userId = generateUUID();
        String itemId = generateUUID();

        Event event = new CartaddEvent(userId, itemId);
        checkSuccessfulEventDelete(event);
    }

    @Test
    public void testAddRateEvent() {
        String userId = generateUUID();
        String itemId = generateUUID();
        double rating = generateRandomDouble(0, 5);

        Event event = new RateEvent(userId, itemId, rating);
        checkSuccessfulEventAdd(event);
    }

    @Test
    public void testAddRateEventManualTime() {
        String userId = generateUUID();
        String itemId = generateUUID();
        double rating = generateRandomDouble(0, 5);
        long timestamp = Long.parseLong(getUnixTimestamp());

        Event event = new RateEvent(userId, itemId, rating, timestamp);
        checkSuccessfulEventAdd(event);
    }

    @Test
    public void testAddRateEventWrongClientId() {
        String userId = generateUUID();
        String itemId = generateUUID();
        double rating = generateRandomDouble(0, 5);
        long timestamp = Long.parseLong(getUnixTimestamp());

        Event event = new RateEvent(userId, itemId, rating, timestamp);
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

        Event event = new RateEvent(userId, itemId, rating, timestamp);
        try {
            incorrectSecretClient.addEventLog(event);
        } catch (IOException e) {
            fail();
        } catch (ZaiClientException e) {
            assertEquals(e.getHttpStatusCode(), 401);
        }
    }

    @Test
    public void testUpdateRateEvent() {
        String userId = generateUUID();
        double oldRating = generateRandomDouble(0, 5);
        String oldItemId = generateUUID();
        double newRating = generateRandomDouble(0, 5);
        String newItemId = generateUUID();

        Event oldEvent = new RateEvent(userId, oldItemId, oldRating);
        Event newEvent = new RateEvent(userId, newItemId, newRating, oldEvent.getTimestamp());
        checkSuccessfulEventUpdate(oldEvent, newEvent);
    }

    @Test
    public void testDeleteRateEvent() {
        String userId = generateUUID();
        String itemId = generateUUID();
        double rating = generateRandomDouble(0, 5);

        Event event = new RateEvent(userId, itemId, rating);
        checkSuccessfulEventDelete(event);
    }

    @Test
    public void testAddPurchaseEvent() {
        String userId = generateUUID();
        String itemId = generateUUID();
        int price = generateRandomInteger(10000, 100000);

        Event event = new PurchaseEvent(userId, itemId, price);
        checkSuccessfulEventAdd(event);
    }

    @Test
    public void testAddPurchaseEventManualTime() {
        String userId = generateUUID();
        String itemId = generateUUID();
        int price = generateRandomInteger(10000, 100000);
        long timestamp = Long.parseLong(getUnixTimestamp());

        Event event = new PurchaseEvent(userId, itemId, price, timestamp);
        checkSuccessfulEventAdd(event);
    }

    @Test
    public void testAddPurchaseEventWrongClientId() {
        String userId = generateUUID();
        String itemId = generateUUID();
        int price = generateRandomInteger(10000, 100000);
        long timestamp = Long.parseLong(getUnixTimestamp());

        Event event = new PurchaseEvent(userId, itemId, price, timestamp);
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

        Event event = new PurchaseEvent(userId, itemId, price, timestamp);
        try {
            incorrectSecretClient.addEventLog(event);
        } catch (IOException e) {
            fail();
        } catch (ZaiClientException e) {
            assertEquals(e.getHttpStatusCode(), 401);
        }
    }

    @Test
    public void testUpdatePurchaseEvent() {
        String userId = generateUUID();
        int oldPrice = generateRandomInteger(10000, 100000);
        String oldItemId = generateUUID();
        int newPrice = generateRandomInteger(10000, 100000);
        String newItemId = generateUUID();

        Event oldEvent = new PurchaseEvent(userId, oldItemId, oldPrice);
        Event newEvent = new PurchaseEvent(userId, newItemId, newPrice, oldEvent.getTimestamp());
        checkSuccessfulEventUpdate(oldEvent, newEvent);
    }

    @Test
    public void testDeletePurchaseEvent() {
        String userId = generateUUID();
        String itemId = generateUUID();
        int price = generateRandomInteger(10000, 100000);

        Event event = new PurchaseEvent(userId, itemId, price);
        checkSuccessfulEventDelete(event);
    }
}

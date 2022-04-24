package org.zaikorea.ZaiClientTest;

import java.io.IOException;
import java.time.Instant;
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

    private Map<String, String> getEventLog(String partitionValue, String sortValue) {

        HashMap<String, AttributeValue> keyToGet = new HashMap<>();

        keyToGet.put(eventTablePartitionKey, AttributeValue.builder().s(partitionValue).build());
        keyToGet.put(eventTableSortKey, AttributeValue.builder().n(sortValue).build());

        GetItemRequest request = GetItemRequest.builder()
                .key(keyToGet)
                .tableName(eventTableName)
                .consistentRead(false)
                .build();

        try {
            Map<String, AttributeValue> returnedItem = ddbClient.getItem(request).item();
            Map<String, String> item = new HashMap<>();
            if (returnedItem != null) {
                for (String key : returnedItem.keySet()) {
                    String val = returnedItem.get(key).toString();
                    item.put(key, val.substring(17, val.length() - 1));
                }
            }
            return item;
        } catch (DynamoDbException e) {
            System.err.println(e.getMessage());
            return null;
        }
    }

    private boolean deleteEventLog(String partitionValue, String sortValue) {

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
            String timestamp = Float.toString(event.getTimestamp());
            String itemId = event.getItemId();
            String eventType = event.getEventType();
            String eventValue = event.getEventValue();

            Map<String, String> logItem = getEventLog(userId, timestamp);
            assertNotEquals(logItem.size(), 0);
            assertEquals(logItem.get(eventTablePartitionKey), userId);
            assertEquals(logItem.get(eventTableItemIdKey), itemId);
            assertEquals(Double.parseDouble(logItem.get(eventTableSortKey)), Double.parseDouble(timestamp), 0.0001);
            assertEquals(logItem.get(eventTableEventTypeKey), eventType);
            assertEquals(logItem.get(eventTableEventValueKey), eventValue);
            assertTrue(deleteEventLog(userId, timestamp));
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
            assertEquals(e.getHttpStatusCode(), 403);
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
            assertEquals(e.getHttpStatusCode(), 403);
        }
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
            assertEquals(e.getHttpStatusCode(), 403);
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
            assertEquals(e.getHttpStatusCode(), 403);
        }
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
            assertEquals(e.getHttpStatusCode(), 403);
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
            assertEquals(e.getHttpStatusCode(), 403);
        }
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
            assertEquals(e.getHttpStatusCode(), 403);
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
            assertEquals(e.getHttpStatusCode(), 403);
        }
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
            assertEquals(e.getHttpStatusCode(), 403);
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
            assertEquals(e.getHttpStatusCode(), 403);
        }
    }

    @Test
    public void testSample2() {
    }
}

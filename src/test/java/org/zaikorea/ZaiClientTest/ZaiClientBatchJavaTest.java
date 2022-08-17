package org.zaikorea.ZaiClientTest;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.zaikorea.ZaiClient.ZaiClient;
import org.zaikorea.ZaiClient.configs.Config;
import org.zaikorea.ZaiClient.exceptions.BatchSizeLimitExceededException;
import org.zaikorea.ZaiClient.exceptions.EmptyBatchException;
import org.zaikorea.ZaiClient.exceptions.ZaiClientException;
import org.zaikorea.ZaiClient.request.*;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.model.*;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

public class ZaiClientBatchJavaTest {

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

    private String generateUUID() {
        return UUID.randomUUID().toString();
    }

    private int generateRandomInteger(int min, int max) {
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    private double generateRandomDouble(int min, int max) {
        return ThreadLocalRandom.current().nextDouble(min, max);
    }

    private Map<String, String> getEventLogWithTimestamp(String partitionValue, double sortValue) {
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

    private void checkSuccessfulEventBatchAdd(EventBatch eventBatch) {
        try {
            testClient.addEventLog(eventBatch);

            List<Event> events = eventBatch.getEventList();

            for (Event event : events) {

                String userId = event.getUserId();
                double timestamp = event.getTimestamp();
                String itemId = event.getItemId();
                String eventType = event.getEventType();
                String eventValue = event.getEventValue();

                Map<String, String> logItem = getEventLogWithTimestamp(userId, timestamp);
                assertNotNull(logItem);
                assertNotEquals(logItem.size(), 0);
                assertEquals(logItem.get(eventTablePartitionKey), userId);
                assertEquals(logItem.get(eventTableItemIdKey), itemId);
                assertEquals(Double.parseDouble(logItem.get(eventTableSortKey)), timestamp, 0.0001);
                assertEquals(logItem.get(eventTableEventTypeKey), eventType);
                assertEquals(logItem.get(eventTableEventValueKey), eventValue);
                assertTrue(deleteEventLogWithTimestamp(userId, timestamp));
            }
        } catch (IOException | ZaiClientException | EmptyBatchException e) {
            e.printStackTrace();
            fail();
        }
    }

    private void checkSuccessfulEventBatchDelete(EventBatch eventBatch) {
        try {
            testClient.addEventLog(eventBatch);

            List<Event> events = eventBatch.getEventList();

            for (Event event : events) {

                String userId = event.getUserId();
                double timestamp = event.getTimestamp();
                String itemId = event.getItemId();
                String eventType = event.getEventType();
                String eventValue = event.getEventValue();

                Map<String, String> logItem = getEventLogWithTimestamp(userId, timestamp);
                assertNotNull(logItem);
                assertNotEquals(logItem.size(), 0);
                assertEquals(logItem.get(eventTablePartitionKey), userId);
                assertEquals(logItem.get(eventTableItemIdKey), itemId);
                assertEquals(Double.parseDouble(logItem.get(eventTableSortKey)), timestamp, 0.0001);
                assertEquals(logItem.get(eventTableEventTypeKey), eventType);
                assertEquals(logItem.get(eventTableEventValueKey), eventValue);
            }

            testClient.deleteEventLog(eventBatch);

            for (Event event : events) {
                String userId = event.getUserId();
                double timestamp = event.getTimestamp();

                Map<String, String> newLogItem = getEventLogWithTimestamp(userId, timestamp);
                assertNotNull(newLogItem);
                assertEquals(newLogItem.size(), 0);
            }
        } catch (IOException | ZaiClientException | EmptyBatchException e) {
            fail();
        }
    }

    public static String getUnixTimestamp() {
        long utcnow = Instant.now().getEpochSecond();
        return Long.toString(utcnow);
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
    public void testAddPurchaseEventBatch() {
        String userId = generateUUID();

        try {
            PurchaseEventBatch eventBatch = new PurchaseEventBatch(userId);

            final int NUM = 10;

            for (int i = 0; i < NUM ; i++) {
                String itemId = generateUUID();
                int price = generateRandomInteger(10000, 100000);

                eventBatch.addEventItem(itemId, price);
            }
            checkSuccessfulEventBatchAdd(eventBatch);
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testAddPurchaseEventBatchManualTime() {
        String userId = generateUUID();
        long timestamp = Long.parseLong(getUnixTimestamp());

        try {
            PurchaseEventBatch eventBatch = new PurchaseEventBatch(userId, timestamp);

            final int NUM = 10;

            for (int i = 0; i < NUM ; i++) {
                String itemId = generateUUID();
                int price = generateRandomInteger(10000, 100000);

                eventBatch.addEventItem(itemId, price);
            }
            checkSuccessfulEventBatchAdd(eventBatch);
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testDeleteItemPurchaseEventBatch() {
        String userId = generateUUID();
        String itemId = generateUUID();
        int price;

        try {
            PurchaseEventBatch eventBatch = new PurchaseEventBatch(userId);

            final int NUM = 10;

            for (int i = 0; i < NUM ; i++) {
                itemId = generateUUID();
                price = generateRandomInteger(10000, 100000);
                eventBatch.addEventItem(itemId, price);
            }

            eventBatch.deleteEventItem(itemId);
            checkSuccessfulEventBatchAdd(eventBatch);

        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testDeleteItemPurchaseEventBatchWithPrice() {
        String userId = generateUUID();
        String itemId = generateUUID();
        int price = 0;

        try {
            PurchaseEventBatch eventBatch = new PurchaseEventBatch(userId);

            final int NUM = 10;

            for (int i = 0; i < NUM ; i++) {
                itemId = generateUUID();
                price = generateRandomInteger(10000, 100000);
                eventBatch.addEventItem(itemId, price);
            }

            eventBatch.deleteEventItem(itemId, price);
            checkSuccessfulEventBatchAdd(eventBatch);

        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testDeletePurchaseEventBatch() {
        String userId = generateUUID();

        try {
            PurchaseEventBatch eventBatch = new PurchaseEventBatch(userId);

            final int NUM = 10;

            for (int i = 0; i < NUM ; i++) {
                String itemId = generateUUID();
                int price = generateRandomInteger(10000, 100000);

                eventBatch.addEventItem(itemId, price);
            }
            checkSuccessfulEventBatchDelete(eventBatch);
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testAddPurchaseEventBatchExceedMaxLimit() {
        String userId = generateUUID();

        try {
            PurchaseEventBatch eventBatch = new PurchaseEventBatch(userId);

            final int NUM = Config.batchRequestCap + 1;

            for (int i = 0; i < NUM ; i++) {
                String itemId = generateUUID();
                int price = generateRandomInteger(10000, 100000);

                eventBatch.addEventItem(itemId, price);
            }
            checkSuccessfulEventBatchAdd(eventBatch);
        } catch (Exception e) {
            assertTrue(e instanceof BatchSizeLimitExceededException);
        }
    }

    @Test
    public void testAddCustomEventBatch() {
        String userId = generateUUID();
        String eventType = "customEventType";

        try {
            CustomEventBatch eventBatch = new CustomEventBatch(userId, eventType);

            final int NUM = 10;

            for (int i = 0; i < NUM ; i++) {
                String itemId = generateUUID();
                double rate = generateRandomDouble(0, 5);

                eventBatch.addEventItem(itemId, Double.toString(rate));
            }
            checkSuccessfulEventBatchAdd(eventBatch);
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testAddCustomEventBatchManualTime() {
        String userId = generateUUID();
        String eventType = "customEventType";
        long timestamp = Long.parseLong(getUnixTimestamp());

        try {
            CustomEventBatch eventBatch = new CustomEventBatch(userId, eventType, timestamp);

            final int NUM = 10;

            for (int i = 0; i < NUM ; i++) {
                String itemId = generateUUID();
                double rate = generateRandomDouble(0, 5);

                eventBatch.addEventItem(itemId, Double.toString(rate));
            }
            checkSuccessfulEventBatchAdd(eventBatch);
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testDeleteItemCustomEventBatch() {
        String userId = generateUUID();
        String itemId = generateUUID();
        String eventType = "customEventType";
        String eventValue = "";
        long timestamp = Long.parseLong(getUnixTimestamp());

        try {
            CustomEventBatch eventBatch = new CustomEventBatch(userId, eventType, timestamp);

            final int NUM = 10;

            for (int i = 0; i < NUM ; i++) {
                itemId = generateUUID();
                eventValue = Double.toString(generateRandomDouble(0, 5));

                eventBatch.addEventItem(itemId, eventValue);
            }
            eventBatch.deleteEventItem(itemId);

            checkSuccessfulEventBatchAdd(eventBatch);
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testDeleteItemCustomEventBatchWithValue() {
        String userId = generateUUID();
        String itemId = generateUUID();
        String eventType = "customEventType";
        String eventValue = "";
        long timestamp = Long.parseLong(getUnixTimestamp());

        try {
            CustomEventBatch eventBatch = new CustomEventBatch(userId, eventType, timestamp);

            final int NUM = 10;

            for (int i = 0; i < NUM ; i++) {
                itemId = generateUUID();
                eventValue = Double.toString(generateRandomDouble(0, 5));

                eventBatch.addEventItem(itemId, eventValue);
            }
            eventBatch.deleteEventItem(itemId, eventValue);

            checkSuccessfulEventBatchAdd(eventBatch);
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testDeleteCustomEventBatch() {
        String userId = generateUUID();
        String eventType = "customEventType";

        try {
            CustomEventBatch eventBatch = new CustomEventBatch(userId, eventType);

            final int NUM = 10;

            for (int i = 0; i < NUM ; i++) {
                String itemId = generateUUID();
                double rate = generateRandomDouble(0, 5);

                eventBatch.addEventItem(itemId, Double.toString(rate));
            }
            checkSuccessfulEventBatchDelete(eventBatch);
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testAddCustomEventBatchExceedMaxLimit() {
        String userId = generateUUID();
        String eventType = "customEventType";

        try {
            CustomEventBatch eventBatch = new CustomEventBatch(userId, eventType);

            final int NUM = Config.batchRequestCap;

            for (int i = 0; i < NUM ; i++) {
                String itemId = generateUUID();
                double rate = generateRandomDouble(0, 5);

                eventBatch.addEventItem(itemId, Double.toString(rate));
            }
            checkSuccessfulEventBatchAdd(eventBatch);
        } catch (Exception e) {
            assertTrue(e instanceof BatchSizeLimitExceededException);
        }
    }

}

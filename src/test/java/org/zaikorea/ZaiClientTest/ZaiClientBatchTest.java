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
import org.zaikorea.ZaiClient.exceptions.ItemNotFoundException;
import org.zaikorea.ZaiClient.exceptions.LoggedEventBatchException;
import org.zaikorea.ZaiClient.exceptions.ZaiClientException;
import org.zaikorea.ZaiClient.request.*;
import org.zaikorea.ZaiClient.response.EventLoggerResponse;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.model.*;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

public class ZaiClientBatchTest {

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

            ArrayList<Event> events = eventBatch.getEventList();

            for (int i = 0; i < events.size(); i++) {
                Event event = events.get(i);

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
        } catch (IOException | ZaiClientException e) {
            fail();
        }
    }

//    private void checkSuccessfulEventDelete(Event event) {
//        try {
//            testClient.addEventLog(event);
//            String userId = event.getUserId();
//            double timestamp = event.getTimestamp();
//            String itemId = event.getItemId();
//            String eventType = event.getEventType();
//            String eventValue = event.getEventValue();
//
//            Map<String, String> logItem = getEventLog(userId);
//            assertNotNull(logItem);
//            assertNotEquals(logItem.size(), 0);
//            assertEquals(logItem.get(eventTablePartitionKey), userId);
//            assertEquals(logItem.get(eventTableItemIdKey), itemId);
//            assertEquals(Double.parseDouble(logItem.get(eventTableSortKey)), timestamp, 0.0001);
//            assertEquals(logItem.get(eventTableEventTypeKey), eventType);
//            assertEquals(logItem.get(eventTableEventValueKey), eventValue);
//
//            testClient.deleteEventLog(event);
//
//            Map<String, String> newLogItem = getEventLog(userId);
//            assertNotNull(newLogItem);
//            assertEquals(newLogItem.size(), 0);
//        } catch (IOException | ZaiClientException e) {
//            fail();
//        }
//    }

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
    public void testAddViewEventBatch() {
        String userId = generateUUID();

        try {
            ViewEventBatch eventBatch = new ViewEventBatch(userId);

            final int NUM = 10;

            for (int i = 0; i < NUM ; i++) {
                String itemId = generateUUID();
                eventBatch.addItem(itemId);
            }
            checkSuccessfulEventBatchAdd(eventBatch);
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testAddViewEventBatchManualTime() {

        String userId = generateUUID();
        long timestamp = Long.parseLong(getUnixTimestamp());

        try {
            ViewEventBatch eventBatch = new ViewEventBatch(userId, timestamp);

            final int NUM = 10;

            for (int i = 0; i < NUM ; i++) {
                String itemId = generateUUID();
                eventBatch.addItem(itemId);
            }
            checkSuccessfulEventBatchAdd(eventBatch);
        } catch (Exception e) {
            fail();
        }

    }

    @Test
    public void testDeleteItemViewEventBatch() {
        String userId = generateUUID();
        String itemId = generateUUID();

        try {
            ViewEventBatch eventBatch = new ViewEventBatch(userId);

            final int NUM = 10;

            for (int i = 0; i < NUM ; i++) {
                itemId = generateUUID();
                eventBatch.addItem(itemId);
            }

            eventBatch.deleteItem(itemId);

            checkSuccessfulEventBatchAdd(eventBatch);
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testAddLikeEventBatch() {
        String userId = generateUUID();

        try {
            LikeEventBatch eventBatch = new LikeEventBatch(userId);

            final int NUM = 10;

            for (int i = 0; i < NUM ; i++) {
                String itemId = generateUUID();
                eventBatch.addItem(itemId);
            }

            checkSuccessfulEventBatchAdd(eventBatch);
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testAddLikeEventBatchManualTime() {

        String userId = generateUUID();
        long timestamp = Long.parseLong(getUnixTimestamp());

        try {
            LikeEventBatch eventBatch = new LikeEventBatch(userId, timestamp);

            final int NUM = 10;

            for (int i = 0; i < NUM ; i++) {
                String itemId = generateUUID();
                eventBatch.addItem(itemId);
            }

            checkSuccessfulEventBatchAdd(eventBatch);
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testDeleteItemLikeEventBatch() {
        String userId = generateUUID();
        String itemId = generateUUID();

        try {
            LikeEventBatch eventBatch = new LikeEventBatch(userId);

            final int NUM = 10;

            for (int i = 0; i < NUM ; i++) {
                itemId = generateUUID();
                eventBatch.addItem(itemId);
            }

            eventBatch.deleteItem(itemId);

            checkSuccessfulEventBatchAdd(eventBatch);
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testAddCartaddEventBatch() {
        String userId = generateUUID();

        try {
            CartaddEventBatch eventBatch = new CartaddEventBatch(userId);

            final int NUM = 10;

            for (int i = 0; i < NUM ; i++) {
                String itemId = generateUUID();
                eventBatch.addItem(itemId);
            }

            checkSuccessfulEventBatchAdd(eventBatch);
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testAddCartaddEventBatchManualTime() {

        String userId = generateUUID();
        long timestamp = Long.parseLong(getUnixTimestamp());

        try {
            CartaddEventBatch eventBatch = new CartaddEventBatch(userId, timestamp);

            final int NUM = 10;

            for (int i = 0; i < NUM ; i++) {
                String itemId = generateUUID();
                eventBatch.addItem(itemId);
            }

            checkSuccessfulEventBatchAdd(eventBatch);
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testDeleteItemCartaddEventBatch() {
        String userId = generateUUID();
        String itemId = generateUUID();

        try {
            CartaddEventBatch eventBatch = new CartaddEventBatch(userId);

            final int NUM = 10;

            for (int i = 0; i < NUM ; i++) {
                itemId = generateUUID();
                eventBatch.addItem(itemId);
            }

            eventBatch.deleteItem(itemId);

            checkSuccessfulEventBatchAdd(eventBatch);
        } catch (Exception e) {
            fail();
        }
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

                eventBatch.addItem(itemId, price);
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

                eventBatch.addItem(itemId, price);
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
                eventBatch.addItem(itemId, price);
            }

            eventBatch.deleteItem(itemId);
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
                eventBatch.addItem(itemId, price);
            }

            eventBatch.deleteItem(itemId, price);
            checkSuccessfulEventBatchAdd(eventBatch);

        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testAddRateEventBatch() {
        String userId = generateUUID();

        try {
            RateEventBatch eventBatch = new RateEventBatch(userId);

            final int NUM = 10;

            for (int i = 0; i < NUM ; i++) {
                String itemId = generateUUID();
                double rate = generateRandomDouble(0, 5);

                eventBatch.addItem(itemId, rate);
            }

            checkSuccessfulEventBatchAdd(eventBatch);
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testAddRateEventBatchManualTime() {
        String userId = generateUUID();
        long timestamp = Long.parseLong(getUnixTimestamp());

        try {
            RateEventBatch eventBatch = new RateEventBatch(userId, timestamp);

            final int NUM = 10;

            for (int i = 0; i < NUM; i++) {
                String itemId = generateUUID();
                double rate = generateRandomDouble(0, 5);

                eventBatch.addItem(itemId, rate);
            }

            checkSuccessfulEventBatchAdd(eventBatch);
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testDeleteItemRateEventBatch() {
        String userId = generateUUID();
        String itemId = generateUUID();
        double rate;

        try {
            RateEventBatch eventBatch = new RateEventBatch(userId);

            final int NUM = 10;

            for (int i = 0; i < NUM ; i++) {
                itemId = generateUUID();
                rate = generateRandomDouble(0, 5);
                eventBatch.addItem(itemId, rate);
            }

            eventBatch.deleteItem(itemId);
            checkSuccessfulEventBatchAdd(eventBatch);

        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testDeleteItemRateEventBatchWithRate() {
        String userId = generateUUID();
        String itemId = generateUUID();
        double rate = generateRandomDouble(0, 5);

        try {
            RateEventBatch eventBatch = new RateEventBatch(userId);

            final int NUM = 10;

            for (int i = 0; i < NUM ; i++) {
                itemId = generateUUID();
                rate = generateRandomDouble(0,5);
                eventBatch.addItem(itemId, rate);
            }

            eventBatch.deleteItem(itemId, rate);
            checkSuccessfulEventBatchAdd(eventBatch);

        } catch (Exception e) {
            fail();
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

                eventBatch.addItem(itemId, Double.toString(rate));
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

                eventBatch.addItem(itemId, Double.toString(rate));
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

                eventBatch.addItem(itemId, eventValue);
            }
            eventBatch.deleteItem(itemId);

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

                eventBatch.addItem(itemId, eventValue);
            }
            eventBatch.deleteItem(itemId, eventValue);

            checkSuccessfulEventBatchAdd(eventBatch);
        } catch (Exception e) {
            fail();
        }
    }


}

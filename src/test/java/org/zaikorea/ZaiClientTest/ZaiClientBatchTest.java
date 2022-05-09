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
import org.zaikorea.ZaiClient.exceptions.ZaiClientException;
import org.zaikorea.ZaiClient.request.*;
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

        ArrayList<String> itemIds = new ArrayList<>();

        final int NUM = 10;

        for (int i = 0; i < NUM ; i++) {
            String itemId = generateUUID();
            itemIds.add(itemId);
        }

        EventBatch eventBatch = new ViewEventBatch(userId, itemIds);
        checkSuccessfulEventBatchAdd(eventBatch);
    }

    @Test
    public void testAddViewEventBatchManualTime() {

        String userId = generateUUID();
        long timestamp = Long.parseLong(getUnixTimestamp());

        ArrayList<String> itemIds = new ArrayList<>();

        final int NUM = 10;

        for (int i = 0; i < NUM ; i++) {
            String itemId = generateUUID();
            itemIds.add(itemId);
        }

        EventBatch eventBatch = new ViewEventBatch(userId, itemIds, timestamp);
        checkSuccessfulEventBatchAdd(eventBatch);
    }

    @Test
    public void testAddItemToViewEventBatch() {
        String userId = generateUUID();

        ArrayList<String> itemIds = new ArrayList<>();

        final int NUM = 10;

        for (int i = 0; i < NUM ; i++) {
            String itemId = generateUUID();
            itemIds.add(itemId);
        }

        EventBatch eventBatch = new ViewEventBatch(userId, itemIds);

        try {
            String newItemId = generateUUID();
            eventBatch.addItem(newItemId);
        } catch (Exception e) {
            fail();
        }

        checkSuccessfulEventBatchAdd(eventBatch);
    }

    @Test
    public void testDeleteItemToViewEventBatch() {
        String userId = generateUUID();
        String itemId = generateUUID();

        ArrayList<String> itemIds = new ArrayList<>();

        final int NUM = 10;

        for (int i = 0; i < NUM ; i++) {
            itemId = generateUUID();
            itemIds.add(itemId);
        }

        EventBatch eventBatch = new ViewEventBatch(userId, itemIds);

        try {
            eventBatch.deleteItem(itemId);
        } catch (Exception e) {
            fail();
        }

        checkSuccessfulEventBatchAdd(eventBatch);
    }

    @Test
    public void testAddLikeEventBatch() {
        String userId = generateUUID();

        ArrayList<String> itemIds = new ArrayList<>();

        final int NUM = 10;

        for (int i = 0; i < NUM ; i++) {
            String itemId = generateUUID();
            itemIds.add(itemId);
        }

        EventBatch eventBatch = new LikeEventBatch(userId, itemIds);
        checkSuccessfulEventBatchAdd(eventBatch);
    }

    @Test
    public void testAddLikeEventBatchManualTime() {

        String userId = generateUUID();
        long timestamp = Long.parseLong(getUnixTimestamp());

        ArrayList<String> itemIds = new ArrayList<>();

        final int NUM = 10;

        for (int i = 0; i < NUM ; i++) {
            String itemId = generateUUID();
            itemIds.add(itemId);
        }

        EventBatch eventBatch = new LikeEventBatch(userId, itemIds, timestamp);
        checkSuccessfulEventBatchAdd(eventBatch);
    }

    @Test
    public void testAddItemToLikeEventBatch() {
        String userId = generateUUID();

        ArrayList<String> itemIds = new ArrayList<>();

        final int NUM = 10;

        for (int i = 0; i < NUM ; i++) {
            String itemId = generateUUID();
            itemIds.add(itemId);
        }

        EventBatch eventBatch = new LikeEventBatch(userId, itemIds);

        try {
            String newItemId = generateUUID();
            eventBatch.addItem(newItemId);
        } catch (Exception e) {
            fail();
        }

        checkSuccessfulEventBatchAdd(eventBatch);
    }

    @Test
    public void testDeleteItemToLikeEventBatch() {
        String userId = generateUUID();
        String itemId = generateUUID();

        ArrayList<String> itemIds = new ArrayList<>();

        final int NUM = 10;

        for (int i = 0; i < NUM ; i++) {
            itemId = generateUUID();
            itemIds.add(itemId);
        }

        EventBatch eventBatch = new LikeEventBatch(userId, itemIds);

        try {
            eventBatch.deleteItem(itemId);
        } catch (Exception e) {
            fail();
        }

        checkSuccessfulEventBatchAdd(eventBatch);
    }

    @Test
    public void testAddCartaddEventBatch() {
        String userId = generateUUID();

        ArrayList<String> itemIds = new ArrayList<>();

        final int NUM = 10;

        for (int i = 0; i < NUM ; i++) {
            String itemId = generateUUID();
            itemIds.add(itemId);
        }

        EventBatch eventBatch = new CartaddEventBatch(userId, itemIds);
        checkSuccessfulEventBatchAdd(eventBatch);
    }

    @Test
    public void testAddCartaddEventBatchManualTime() {

        String userId = generateUUID();
        long timestamp = Long.parseLong(getUnixTimestamp());

        ArrayList<String> itemIds = new ArrayList<>();

        final int NUM = 10;

        for (int i = 0; i < NUM ; i++) {
            String itemId = generateUUID();
            itemIds.add(itemId);
        }

        EventBatch eventBatch = new CartaddEventBatch(userId, itemIds, timestamp);
        checkSuccessfulEventBatchAdd(eventBatch);
    }

    @Test
    public void testAddItemToCartaddEventBatch() {
        String userId = generateUUID();

        ArrayList<String> itemIds = new ArrayList<>();

        final int NUM = 10;

        for (int i = 0; i < NUM ; i++) {
            String itemId = generateUUID();
            itemIds.add(itemId);
        }

        EventBatch eventBatch = new CartaddEventBatch(userId, itemIds);

        try {
            String newItemId = generateUUID();
            eventBatch.addItem(newItemId);
        } catch (Exception e) {
            fail();
        }

        checkSuccessfulEventBatchAdd(eventBatch);
    }

    @Test
    public void testDeleteItemToCartaddEventBatch() {
        String userId = generateUUID();
        String itemId = generateUUID();

        ArrayList<String> itemIds = new ArrayList<>();

        final int NUM = 10;

        for (int i = 0; i < NUM ; i++) {
            itemId = generateUUID();
            itemIds.add(itemId);
        }

        EventBatch eventBatch = new CartaddEventBatch(userId, itemIds);

        try {
            eventBatch.deleteItem(itemId);
        } catch (Exception e) {
            fail();
        }

        checkSuccessfulEventBatchAdd(eventBatch);
    }

    @Test
    public void testAddPurchaseEventBatch() {
        String userId = generateUUID();

        ArrayList<ItemEventValuePair> purchaseItems = new ArrayList<>();

        final int NUM = 10;

        for (int i = 0; i < NUM ; i++) {
            String itemId = generateUUID();
            int price = generateRandomInteger(10000, 100000);

            purchaseItems.add(new ItemEventValuePair(itemId, price));
        }

        EventBatch eventBatch = new PurchaseEventBatch(userId, purchaseItems);
        checkSuccessfulEventBatchAdd(eventBatch);
    }

    @Test
    public void testAddPurchaseEventBatchManualTime() {
        String userId = generateUUID();
        long timestamp = Long.parseLong(getUnixTimestamp());

        ArrayList<ItemEventValuePair> purchaseItems = new ArrayList<>();

        final int NUM = 10;

        for (int i = 0; i < NUM ; i++) {
            String itemId = generateUUID();
            int price = generateRandomInteger(10000, 100000);

            purchaseItems.add(new ItemEventValuePair(itemId, price));
        }

        EventBatch eventBatch = new PurchaseEventBatch(userId, purchaseItems, timestamp);
        checkSuccessfulEventBatchAdd(eventBatch);
    }

    @Test
    public void testAddRateEventBatch() {
        String userId = generateUUID();

        ArrayList<ItemEventValuePair> rateItems = new ArrayList<>();

        final int NUM = 10;

        for (int i = 0; i < NUM ; i++) {
            String itemId = generateUUID();
            double rate = generateRandomDouble(0, 5);

            rateItems.add(new ItemEventValuePair(itemId, rate));
        }

        EventBatch eventBatch = new RateEventBatch(userId, rateItems);
        checkSuccessfulEventBatchAdd(eventBatch);
    }

    @Test
    public void testAddRateEventBatchManualTime() {
        String userId = generateUUID();
        long timestamp = Long.parseLong(getUnixTimestamp());

        ArrayList<ItemEventValuePair> rateItems = new ArrayList<>();

        final int NUM = 10;

        for (int i = 0; i < NUM ; i++) {
            String itemId = generateUUID();
            double rate = generateRandomDouble(0, 5);

            rateItems.add(new ItemEventValuePair(itemId, rate));
        }

        EventBatch eventBatch = new RateEventBatch(userId, rateItems, timestamp);
        checkSuccessfulEventBatchAdd(eventBatch);
    }

}

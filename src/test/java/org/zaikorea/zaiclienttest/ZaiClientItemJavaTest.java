package org.zaikorea.zaiclienttest;

import static org.junit.Assert.*;

import java.io.IOException;
import java.time.Instant;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

import org.junit.Before;
import org.junit.Test;
import org.zaikorea.zaiclient.ZaiClient;
import org.zaikorea.zaiclient.exceptions.ZaiClientException;
import org.zaikorea.zaiclient.request.items.Item;
import org.zaikorea.zaiclient.request.items.ItemRequest;
import org.zaikorea.zaiclient.request.items.AddItem;
import org.zaikorea.zaiclient.request.items.UpdateItem;
import org.zaikorea.zaiclient.response.ItemResponse;
import org.zaikorea.zaiclient.request.items.DeleteItem;

public class ZaiClientItemJavaTest {
    private static final String clientId = "test";
    private static final String clientSecret = "KVPzvdHTPWnt0xaEGc2ix-eqPXFCdEV5zcqolBr_h1k"; // this secret key is for testing purposes only
    private static final String AddItem = null;

    private ZaiClient client;

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

    private void checkSuccessfulItemRequest(AddItem request) {
        try {
            ItemResponse response = client.sendRequest(request);
            int expectedCount = request.getPayload().size();
            assertEquals("Response count should be equal to request count", expectedCount, response.getCount());

        } catch (IOException e) {
            System.out.println(e.getCause());
            fail(e.getMessage());
        } catch (ZaiClientException e) {
            System.out.println(e.getCause());
            fail(e.getMessage());
        }
    }

    private void checkSuccessfulItemRequest(DeleteItem request) {
        try {
            ItemResponse response = client.sendRequest(request);
            int expectedCount = request.getPayload().size();


            System.out.println(response.getCount());
            response.getItems().stream().forEach(item -> {
               System.out.println(item.getItemId());
            });
            assertEquals("Response count should be equal to request count", expectedCount, response.getCount());
        } catch (Exception e){
            fail(e.getMessage());
        }
    }

    @Before
    public void setUp() {
        client = new ZaiClient.Builder(clientId, clientSecret)
                .connectTimeout(10)
                .readTimeout(30)
                .customEndpoint("dev")
                .build();
    }

    @Test
    public void testAddItem() {
        String itemId = generateUUID();
        String itemName = generateRandomString(10);

        Item item = new Item(itemId, itemName);

        AddItem addItemRequest = new AddItem(item);
        checkSuccessfulItemRequest(addItemRequest);

        DeleteItem deleteItemRequest = new DeleteItem(itemId);
        checkSuccessfulItemRequest(deleteItemRequest);
    }

    @Test
    public void testAddItemBatch() {
        List<Item> items = new LinkedList<>();
        IntStream.range(0, 10).forEach(i -> {
            String itemId = generateUUID();
            String itemName = generateRandomString(10);

            Item item = new Item(itemId, itemName);
            items.add(item);
        });

        AddItem addItemRequest = new AddItem(items);
        checkSuccessfulItemRequest(addItemRequest);

        List<String> itemIds = new LinkedList<>();
        items.forEach(item -> itemIds.add(item.getItemId()));
        DeleteItem deleteItemRequest = new DeleteItem(itemIds);

        checkSuccessfulItemRequest(deleteItemRequest);
    }


}

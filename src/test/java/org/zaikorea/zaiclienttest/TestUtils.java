package org.zaikorea.zaiclienttest;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class TestUtils {

    public static String generateUUID() {
        return UUID.randomUUID().toString();
    }

    public static int generateRandomInteger(int min, int max) {
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

}

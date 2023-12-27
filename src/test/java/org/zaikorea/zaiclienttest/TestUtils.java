package org.zaikorea.zaiclienttest;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

public class TestUtils {

    public static String generateUUID() {
        return UUID.randomUUID().toString();
    }

    public static int generateRandomInteger(int min, int max) {
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    public static Map<String, Object> ddbMapToJavaMap(Map<String, AttributeValue> itemMap) {
        Map<String, Object> javaMap = new HashMap<>();

        for (Map.Entry<String, AttributeValue> entry : itemMap.entrySet()) {
            String key = entry.getKey();
            AttributeValue attributeValue = entry.getValue();

            // AttributeValue의 데이터 타입에 따라 적절한 Java 객체로 변환
            Object javaValue = toJavaObject(attributeValue);

            javaMap.put(key, javaValue);
        }

        return javaMap;
    }

    public static Object toJavaObject(AttributeValue attributeValue) {
        if (attributeValue.s() != null) {
            return attributeValue.s();
        } else if (attributeValue.n() != null) {
            return attributeValue.n();
        } else if (attributeValue.bool() != null) {
            return attributeValue.bool();
        } else if (attributeValue.nul() != null) {
            return null;
        } else if (attributeValue.hasM()) {
            // 중첩된 Map의 경우 재귀적으로 변환
            return ddbMapToJavaMap(attributeValue.m());
        } else if (attributeValue.hasL()) {
            return attributeValue.l().stream()
                    .map(TestUtils::toJavaObject)
                    .collect(java.util.stream.Collectors.toList());
        } else {
            // 그 외의 경우는 null 또는 다른 형태의 데이터일 수 있음
            return null;
        }
    }
}

package org.zaikorea.ZaiClient.security;

import org.zaikorea.ZaiClient.configs.Config;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class ZaiHeaders {

    public static String getUnixTimestamp() {
        long utcnow = Instant.now().getEpochSecond();
        return Long.toString(utcnow);
    }

    public static Map<String, String> generateZaiHeaders(String zaiClientId, String zaiSecret, String path)
            throws NoSuchAlgorithmException, InvalidKeyException {
        String unixTimestamp = ZaiHeaders.getUnixTimestamp();
        String zaiToken = Hmac.generateZaiToken(zaiSecret, path, unixTimestamp);

        Map<String, String> zaiHeaders = new HashMap<>();
        zaiHeaders.put(Config.zaiClientIdHeader, zaiClientId);
        zaiHeaders.put(Config.zaiUnixTimestampHeader, unixTimestamp);
        zaiHeaders.put(Config.zaiAuthorizationHeader, zaiToken);

        return zaiHeaders;
    }
}

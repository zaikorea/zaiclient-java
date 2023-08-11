package org.zaikorea.zaiclient.security;

import org.zaikorea.zaiclient.configs.Config;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Hex;

public class Hmac {
    public static String sign(
        String secret, String message, String algorithm
    ) throws NoSuchAlgorithmException, InvalidKeyException {

        SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), algorithm);
        Mac mac = Mac.getInstance(algorithm);
        mac.init(secretKey);
        byte[] hash = mac.doFinal(message.getBytes());
        return new String(Hex.encodeHex(hash));
    }

    public static String generateZaiToken(
            String secret, String path, String timestamp
    ) throws NoSuchAlgorithmException, InvalidKeyException {

        String modifiedPath;

        if (path.length() > 1 && path.charAt(path.length() - 1) == '/')
            modifiedPath = path.substring(0, path.length() - 1);
        else
            modifiedPath = path;

        String message = modifiedPath + ":" + timestamp;
        return Hmac.sign(secret, message, Config.hmacAlgorithm);
    }
}

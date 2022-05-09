package org.zaikorea.ZaiClient.configs;

public class Config {
    public static final String eventsApiEndPoint = "https://collector-api.zaikorea.org";
    public static final String eventsApiPath = "/events";
    public static final String mlApiPathPrefix = "/clients/{client_id}/recommenders";

    public static final String hmacAlgorithm = "HmacSHA256";
    public static final String hmacScheme = "Zai";
    public static final String zaiClientIdHeader = "X-ZAI-CLIENT-ID";
    public static final String zaiUnixTimestampHeader = "X-ZAI-TIMESTAMP";
    public static final String zaiAuthorizationHeader = "X-ZAI-AUTHORIZATION";
    public static final String zaiCallTypeHeader = "X-ZAI-CALL-TYPE";
    public static final String zaiCallType = "sdk_call";
    public static final String timestampKey = "timestamp";
    public static final double epsilon = 1e-4;
}

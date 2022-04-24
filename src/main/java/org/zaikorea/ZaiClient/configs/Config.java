package org.zaikorea.ZaiClient.configs;

public class Config {
    public static final String eventsApiEndPoint = "https://api.zaikorea.org";
    public static final String eventsApiPath = "/echo";
    public static final String mlApiPathPrefix = "/clients/{client_id}/recommenders";

    public static final String hmacAlgorithm = "HmacSHA256";
    public static final String zaiClientIdHeader = "X-ZAI-CLIENT-ID";
    public static final String zaiUnixTimestampHeader = "X-ZAI-TIMESTAMP";
    public static final String zaiAuthorizationHeader = "X-ZAI-AUTHORIZATION";
}

package org.zaikorea.zaiclient.configs;

public class Config {
    public static final String eventsApiEndPoint = "https://collector-api%s.zaikorea.org"; // TODO: Delete this line
    public static final String collectorApiEndPoint = "https://collector-api%s.zaikorea.org";
    public static final String eventsApiPath = "/events";

    public static final String itemsApiPath = "/items";

    public static final String mlApiEndPoint = "https://ml-api%s.zaikorea.org";
    public static final String mlApiPathPrefix = "/clients/%s/recommenders";

    public static final String userRecommendationPath = "/user-recommendations";
    public static final String relatedItemsRecommendationPath = "/related-items";
    public static final String rerankingRecommendationPath = "/reranking";
    public static final String customRecommendationPath = "/custom-recommendations";

    public static final String hmacAlgorithm = "HmacSHA256";
    public static final String hmacScheme = "ZAi";
    public static final String zaiClientIdHeader = "X-ZAI-CLIENT-ID";
    public static final String zaiUnixTimestampHeader = "X-ZAI-TIMESTAMP";
    public static final String zaiAuthorizationHeader = "X-ZAI-AUTHORIZATION";
    public static final String zaiCallTypeHeader = "X-ZAI-CALL-TYPE";
    public static final String zaiCallType = "sdk_call";

    public static final int batchRequestCap = 50;
    public static final double epsilon = 1e-4;
    public static final int testEventTimeToLive = 60 * 60 * 24; // 1 day
}

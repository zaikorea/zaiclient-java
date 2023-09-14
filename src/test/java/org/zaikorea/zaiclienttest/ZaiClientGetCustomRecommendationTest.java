package org.zaikorea.zaiclienttest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.zaikorea.zaiclient.ZaiClient;
import org.zaikorea.zaiclient.exceptions.ZaiClientException;
import org.zaikorea.zaiclient.request.recommendations.GetRerankingRecommendation;
import org.zaikorea.zaiclient.request.recommendations.RecommendationQuery;
import org.zaikorea.zaiclient.request.recommendations.RecommendationRequest;
import org.zaikorea.zaiclient.response.RecommendationResponse;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;


package org.zaikorea.ZaiClientTest

import org.zaikorea.ZaiClient.ZaiClient
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import java.util.UUID
import java.util.HashMap
import org.zaikorea.ZaiClientTest.ZaiClientRecommenderKotlinTest
import software.amazon.awssdk.services.dynamodb.model.QueryRequest
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest
import org.zaikorea.ZaiClient.request.RecommendationRequest
import org.zaikorea.ZaiClient.response.RecommendationResponse
import java.io.IOException
import org.zaikorea.ZaiClient.exceptions.ZaiClientException
import org.junit.Before
import org.junit.After
import org.junit.Assert
import org.junit.Test
import org.zaikorea.ZaiClient.request.UserRecommendationRequest
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import java.util.Collections
import java.util.concurrent.ThreadLocalRandom

class ZaiClientRecommenderKotlinTest {
    private var testClient: ZaiClient? = null
    private var incorrectIdClient: ZaiClient? = null
    private var incorrectSecretClient: ZaiClient? = null
    private var ddbClient: DynamoDbClient? = null
    private fun generateUUID(): String {
        return UUID.randomUUID().toString()
    }

    private fun generateRandomInteger(min: Int, max: Int): Int {
        return ThreadLocalRandom.current().nextInt(min, max + 1)
    }

    private fun getRecLog(partitionValue: String?): Map<String, String>? {
        val partitionAlias = "#pk"
        val attrNameAlias = HashMap<String, String>()
        attrNameAlias[partitionAlias] = recLogTablePartitionKey
        val attrValues = HashMap<String, AttributeValue>()
        attrValues[":" + recLogTablePartitionKey] = AttributeValue.builder()
            .s(partitionValue)
            .build()
        val request = QueryRequest.builder()
            .tableName(recLogTableName)
            .keyConditionExpression(partitionAlias + " = :" + recLogTablePartitionKey)
            .expressionAttributeNames(attrNameAlias)
            .expressionAttributeValues(attrValues)
            .build()
        return try {
            val returnedItems = ddbClient!!.query(request).items()
            if (returnedItems.size > 1) return null
            if (returnedItems.size == 0) return HashMap()
            val returnedItem = returnedItems[0]
            val item: MutableMap<String, String> = HashMap()
            if (returnedItem != null) {
                for (key in returnedItem.keys) {
                    val `val` = returnedItem[key].toString()
                    item[key] = `val`.substring(17, `val`.length - 1)
                }
                return item
            }
            null
        } catch (e: DynamoDbException) {
            e.printStackTrace()
            System.err.println(e.message)
            null
        }
    }

    private fun deleteRecLog(partitionValue: String): Boolean {
        val sortValue = getRecLog(partitionValue)!![recLogTableSortKey]
        val keyToGet = HashMap<String, AttributeValue>()
        keyToGet[recLogTablePartitionKey] = AttributeValue.builder().s(partitionValue).build()
        keyToGet[recLogTableSortKey] = AttributeValue.builder().n(sortValue).build()
        val deleteReq = DeleteItemRequest.builder()
            .key(keyToGet)
            .tableName(recLogTableName)
            .build()
        try {
            ddbClient!!.deleteItem(deleteReq)
        } catch (e: DynamoDbException) {
            System.err.println(e.message)
            return false
        }
        return true
    }

    private fun checkSuccessfulGetUserRecommendation(recommendation: RecommendationRequest, userId: String?) {
        var userId = userId
        try {
            val response = testClient!!.getRecommendations(recommendation)
            val limit = recommendation.limit
            var logItem = getRecLog(userId)
            if (userId == null) {
                userId = "null"
                logItem = getRecLog("null")
            }

            // assertNotNull(logItem);
            // assertNotEquals(logItem.size(), 0);
            // assertEquals(logItem.get(recLogRecommendations).split(",").length, response.getItems().size());
            Assert.assertEquals(response.items.size.toLong(), limit.toLong())
            Assert.assertEquals(response.count.toLong(), limit.toLong())
            // assertTrue(deleteRecLog(userId));
        } catch (e: IOException) {
            Assert.fail()
        } catch (e: ZaiClientException) {
            Assert.fail()
        }
    }

    @Before
    fun setup() {
        testClient = ZaiClient(clientId, clientSecret)
        incorrectIdClient = ZaiClient("." + clientId, clientSecret)
        incorrectSecretClient = ZaiClient(clientId, "." + clientSecret)
        ddbClient = DynamoDbClient.builder()
            .region(region)
            .build()
    }

    @After
    fun cleanup() {
        ddbClient!!.close()
    }

    @Test
    fun testGetUserRecommendation() {
        val userId = generateUUID()
        val limit = generateRandomInteger(1, 10)
        val offset = generateRandomInteger(20, 40)
        val recommendation: RecommendationRequest = UserRecommendationRequest(userId, limit, offset)
        checkSuccessfulGetUserRecommendation(recommendation, userId)
    }

    @Test
    fun testGetNullUserRecommendation() {
        val userId: String? = null
        val limit = generateRandomInteger(1, 10)
        val offset = generateRandomInteger(20, 40)
        val recommendation: RecommendationRequest = UserRecommendationRequest(userId, limit, offset)
        checkSuccessfulGetUserRecommendation(recommendation, userId)
    }

    @Test
    fun testGetUserRecommendationWithRecommendationType() {
        val userId = generateUUID()
        val limit = generateRandomInteger(1, 10)
        val offset = generateRandomInteger(20, 40)
        val recommendation: RecommendationRequest = UserRecommendationRequest(userId, limit, offset, "home_page")
        checkSuccessfulGetUserRecommendation(recommendation, userId)
    }

    @Test
    fun testGetUserRecommendationWrongClientId() {
        val userId = generateUUID()
        val limit = generateRandomInteger(1, 10)
        val offset = generateRandomInteger(20, 40)
        val recommendation: RecommendationRequest = UserRecommendationRequest(userId, limit, offset)
        try {
            incorrectIdClient!!.getRecommendations(recommendation)
        } catch (e: IOException) {
            Assert.fail()
        } catch (e: ZaiClientException) {
            Assert.assertEquals(e.httpStatusCode.toLong(), 404)
        }
    }

    @Test
    fun testGetUserRecommendationWrongSecret() {
        val userId = generateUUID()
        val limit = generateRandomInteger(1, 10)
        val offset = generateRandomInteger(20, 40)
        val recommendation: RecommendationRequest = UserRecommendationRequest(userId, limit, offset)
        try {
            incorrectSecretClient!!.getRecommendations(recommendation)
        } catch (e: IOException) {
            Assert.fail()
        } catch (e: ZaiClientException) {
            Assert.assertEquals(e.httpStatusCode.toLong(), 401)
        }
    }

    @Test
    fun testGetTooLongUserRecommendation() {
        val userId = java.lang.String.join("a", Collections.nCopies(101, "a"))
        val limit = generateRandomInteger(1, 10)
        val offset = generateRandomInteger(20, 40)
        val recommendation: RecommendationRequest = UserRecommendationRequest(userId, limit, offset)
        try {
            testClient!!.getRecommendations(recommendation)
        } catch (e: IOException) {
            Assert.fail()
        } catch (e: ZaiClientException) {
            Assert.assertEquals(e.httpStatusCode.toLong(), 422)
        }
    }

    @Test
    fun testGetTooBigLimitRecommendation() {
        val userId = generateUUID()
        val limit = 1000001
        val offset = generateRandomInteger(20, 40)
        val recommendation: RecommendationRequest = UserRecommendationRequest(userId, limit, offset)
        try {
            testClient!!.getRecommendations(recommendation)
        } catch (e: IOException) {
            Assert.fail()
        } catch (e: ZaiClientException) {
            Assert.assertEquals(e.httpStatusCode.toLong(), 422)
        }
    }

    @Test
    fun testGetTooBigOffsetRecommendation() {
        val userId = generateUUID()
        val limit = generateRandomInteger(20, 40)
        val offset = 1000001
        val recommendation: RecommendationRequest = UserRecommendationRequest(userId, limit, offset)
        try {
            testClient!!.getRecommendations(recommendation)
        } catch (e: IOException) {
            Assert.fail()
        } catch (e: ZaiClientException) {
            Assert.assertEquals(e.httpStatusCode.toLong(), 422)
        }
    }

    companion object {
        private const val clientId = "test"
        private const val clientSecret =
            "KVPzvdHTPWnt0xaEGc2ix-eqPXFCdEV5zcqolBr_h1k" // this secret key is for testing purposes only
        private const val recLogTableName = "rec_log_test"
        private const val recLogTablePartitionKey = "user_id"
        private const val recLogTableSortKey = "timestamp"
        private const val recLogRecommendations = "recommendations"
        private val region = Region.AP_NORTHEAST_2
    }
}
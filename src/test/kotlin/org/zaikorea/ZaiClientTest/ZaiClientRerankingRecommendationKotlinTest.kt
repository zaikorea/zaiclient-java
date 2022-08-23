package org.zaikorea.ZaiClientTest

import org.zaikorea.ZaiClient.ZaiClient
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import java.util.UUID
import java.util.HashMap
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
import org.zaikorea.ZaiClient.request.RerankingRecommendationRequest
import java.util.Collections
import java.lang.IllegalArgumentException
import org.zaikorea.ZaiClient.request.UserRecommendationRequest
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import java.lang.Error
import java.util.ArrayList
import java.util.concurrent.ThreadLocalRandom

class ZaiClientRerankingRecommendationKotlinTest {
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

    private fun checkSuccessfulGetRerankingRecommendation(recommendation: RecommendationRequest, userId: String?) {
        var userId = userId
        try {
            val response = testClient!!.getRecommendations(recommendation)
            val limit = recommendation.limit
            if (userId == null) {
                userId = "null"
                Assert.assertEquals(response.items.size.toLong(), limit.toLong())
                Assert.assertEquals(response.count.toLong(), limit.toLong())

                return ;
            }

            var logItem = getRecLog(userId)

            Assert.assertNotNull(logItem);
            if (logItem != null) {
                Assert.assertNotEquals(logItem.size.toLong(), 0)
                Assert.assertEquals(
                    logItem.get(recLogRecommendations)!!.split(",").size,
                    response.getItems().size
                )
            };
            Assert.assertEquals(response.items.size.toLong(), limit.toLong())
            Assert.assertEquals(response.count.toLong(), limit.toLong())
            Assert.assertTrue(deleteRecLog(userId));
        } catch (e: IOException) {
            Assert.fail()
        } catch (e: ZaiClientException) {
            Assert.fail()
        }
    }

    @Before
    fun setup() {
        testClient = ZaiClient.Builder(clientId, clientSecret)
            .connectTimeout(30)
            .readTimeout(10)
            .build()
        incorrectIdClient = ZaiClient.Builder("." + clientId, clientSecret)
            .connectTimeout(0)
            .readTimeout(0)
            .build()
        incorrectSecretClient = ZaiClient.Builder(clientId, "." + clientSecret)
            .connectTimeout(-1)
            .readTimeout(-1)
            .build()
        ddbClient = DynamoDbClient.builder()
            .region(region)
            .build()
    }

    @After
    fun cleanup() {
        ddbClient!!.close()
    }

    @Test
    fun testGetRerankingRecommendation_1() {
        val userId = generateUUID()
        val itemIds: MutableList<String> = ArrayList()
        for (i in 0..49) {
            itemIds.add(generateUUID())
        }
        val limit = generateRandomInteger(1, 10)
        val offset = generateRandomInteger(20, 40)
        val recommendationType = "category_page"
        val recommendation: RecommendationRequest = RerankingRecommendationRequest(
            userId, itemIds, limit, offset,
            recommendationType
        )
        checkSuccessfulGetRerankingRecommendation(recommendation, userId)
    }

    @Test
    fun testGetRerankingRecommendation_2() {
        val userId = generateUUID()
        val itemIds: MutableList<String> = ArrayList()
        for (i in 0..49) {
            itemIds.add(generateUUID())
        }
        val limit = generateRandomInteger(1, 10)
        val offset = generateRandomInteger(20, 40)
        val recommendation: RecommendationRequest = RerankingRecommendationRequest(userId, itemIds, limit, offset)
        checkSuccessfulGetRerankingRecommendation(recommendation, userId)
    }

    @Test
    fun testGetRerankingRecommendation_3() {
        val userId = generateUUID()
        val itemIds: MutableList<String> = ArrayList()
        for (i in 0..49) {
            itemIds.add(generateUUID())
        }
        val limit = generateRandomInteger(1, 10)
        val recommendation: RecommendationRequest = RerankingRecommendationRequest(userId, itemIds, limit)
        checkSuccessfulGetRerankingRecommendation(recommendation, userId)
    }

    @Test
    fun testGetRerankingRecommendation_4() {
        val userId = generateUUID()
        val itemIds: MutableList<String> = ArrayList()
        for (i in 0..49) {
            itemIds.add(generateUUID())
        }
        val limit = generateRandomInteger(1, 10)
        val recommendationType = "category_page"
        val recommendation: RecommendationRequest = RerankingRecommendationRequest(
            userId, itemIds, limit,
            recommendationType
        )
        checkSuccessfulGetRerankingRecommendation(recommendation, userId)
    }

    @Test
    fun testGetRerankingRecommendation_5() {
        val userId = generateUUID()
        val itemIds: MutableList<String> = ArrayList()
        for (i in 0..49) {
            itemIds.add(generateUUID())
        }
        val recommendationType = "category_page"
        val recommendation: RecommendationRequest = RerankingRecommendationRequest(userId, itemIds, recommendationType)
        checkSuccessfulGetRerankingRecommendation(recommendation, userId)
    }

    @Test
    fun testGetRerankingRecommendation_6() {
        val userId = generateUUID()
        val itemIds: MutableList<String> = ArrayList()
        for (i in 0..49) {
            itemIds.add(generateUUID())
        }
        val recommendation: RecommendationRequest = RerankingRecommendationRequest(userId, itemIds)
        checkSuccessfulGetRerankingRecommendation(recommendation, userId)
    }

    @Test
    fun testGetNullRerankingRecommendation_1() {
        val userId: String? = null
        val itemIds: MutableList<String> = ArrayList()
        for (i in 0..49) {
            itemIds.add(generateUUID())
        }
        val limit = generateRandomInteger(1, 10)
        val offset = generateRandomInteger(20, 40)
        val recommendationType = "category_page"
        val recommendation: RecommendationRequest = RerankingRecommendationRequest(
            userId, itemIds, limit, offset,
            recommendationType
        )
        checkSuccessfulGetRerankingRecommendation(recommendation, userId)
    }

    @Test
    fun testGetNullRerankingRecommendation_2() {
        val userId: String? = null
        val itemIds: MutableList<String> = ArrayList()
        for (i in 0..49) {
            itemIds.add(generateUUID())
        }
        val limit = generateRandomInteger(1, 10)
        val offset = generateRandomInteger(20, 40)
        val recommendation: RecommendationRequest = RerankingRecommendationRequest(userId, itemIds, limit, offset)
        checkSuccessfulGetRerankingRecommendation(recommendation, userId)
    }

    @Test
    fun testGetNullRerankingRecommendation_3() {
        val userId: String? = null
        val itemIds: MutableList<String> = ArrayList()
        for (i in 0..49) {
            itemIds.add(generateUUID())
        }
        val limit = generateRandomInteger(1, 10)
        val recommendationType = "category_page"
        val recommendation: RecommendationRequest = RerankingRecommendationRequest(
            userId, itemIds, limit,
            recommendationType
        )
        checkSuccessfulGetRerankingRecommendation(recommendation, userId)
    }

    @Test
    fun testGetNullRerankingRecommendation_4() {
        val userId: String? = null
        val itemIds: MutableList<String> = ArrayList()
        for (i in 0..49) {
            itemIds.add(generateUUID())
        }
        val limit = generateRandomInteger(1, 10)
        val recommendation: RecommendationRequest = RerankingRecommendationRequest(userId, itemIds, limit)
        checkSuccessfulGetRerankingRecommendation(recommendation, userId)
    }

    @Test
    fun testGetNullRerankingRecommendation_5() {
        val userId: String? = null
        val itemIds: MutableList<String> = ArrayList()
        for (i in 0..49) {
            itemIds.add(generateUUID())
        }
        val recommendationType = "category_page"
        val recommendation: RecommendationRequest = RerankingRecommendationRequest(userId, itemIds, recommendationType)
        checkSuccessfulGetRerankingRecommendation(recommendation, userId)
    }

    @Test
    fun testGetNullRerankingRecommendation_6() {
        val userId: String? = null
        val itemIds: MutableList<String> = ArrayList()
        for (i in 0..49) {
            itemIds.add(generateUUID())
        }
        val recommendation: RecommendationRequest = RerankingRecommendationRequest(userId, itemIds)
        checkSuccessfulGetRerankingRecommendation(recommendation, userId)
    }

    @Test
    fun testGetRerankingRecommendationWrongClientId() {
        val userId = generateUUID()
        val itemIds: MutableList<String> = ArrayList()
        for (i in 0..49) {
            itemIds.add(generateUUID())
        }
        val limit = generateRandomInteger(1, 10)
        val offset = generateRandomInteger(20, 40)
        val recommendation: RecommendationRequest = RerankingRecommendationRequest(userId, itemIds, limit, offset)
        try {
            incorrectIdClient!!.getRecommendations(recommendation)
        } catch (e: IOException) {
            Assert.fail()
        } catch (e: ZaiClientException) {
            Assert.assertEquals(e.httpStatusCode.toLong(), 404)
        }
    }

    @Test
    fun testGetRerankingRecommendationWrongSecret() {
        val userId = generateUUID()
        val itemIds: MutableList<String> = ArrayList()
        for (i in 0..49) {
            itemIds.add(generateUUID())
        }
        val limit = generateRandomInteger(1, 10)
        val offset = generateRandomInteger(20, 40)
        val recommendation: RecommendationRequest = RerankingRecommendationRequest(userId, itemIds, limit, offset)
        try {
            incorrectSecretClient!!.getRecommendations(recommendation)
        } catch (e: IOException) {
            Assert.fail()
        } catch (e: ZaiClientException) {
            Assert.assertEquals(e.httpStatusCode.toLong(), 401)
        }
    }

    @Test
    fun testGetTooLongRerankingRecommendation() {
        val userId = java.lang.String.join("a", Collections.nCopies(101, "a"))
        val itemIds: MutableList<String> = ArrayList()
        for (i in 0..49) {
            itemIds.add(generateUUID())
        }
        val limit = generateRandomInteger(1, 10)
        val offset = generateRandomInteger(20, 40)
        try {
            RerankingRecommendationRequest(userId, itemIds, limit, offset)
            Assert.fail()
        } catch (e: IllegalArgumentException) {
            Assert.assertEquals(e.message, userIdExceptionMessage)
        } catch (e: Error) {
            Assert.fail()
        }
    }

    @Test
    fun testGetTooBigLimitRecommendation() {
        val userId = generateUUID()
        val itemIds: MutableList<String> = ArrayList()
        for (i in 0..49) {
            itemIds.add(generateUUID())
        }
        val limit = 1000001
        val offset = generateRandomInteger(20, 40)
        try {
            RerankingRecommendationRequest(userId, itemIds, limit, offset)
            Assert.fail()
        } catch (e: IllegalArgumentException) {
            Assert.assertEquals(e.message, limitExceptionMessage)
        } catch (e: Error) {
            Assert.fail()
        }
    }

    @Test
    fun testGetTooBigOffsetRecommendation() {
        val userId = generateUUID()
        val itemIds: MutableList<String> = ArrayList()
        for (i in 0..49) {
            itemIds.add(generateUUID())
        }
        val limit = generateRandomInteger(20, 40)
        val offset = 1000001
        try {
            RerankingRecommendationRequest(userId, itemIds, limit, offset)
            Assert.fail()
        } catch (e: IllegalArgumentException) {
            Assert.assertEquals(e.message, offsetExceptionMessage)
        } catch (e: Error) {
            Assert.fail()
        }
    }

    @Test
    fun testGetTooLongTypeRecommendation() {
        val userId = generateUUID()
        val itemIds: MutableList<String> = ArrayList()
        for (i in 0..49) {
            itemIds.add(generateUUID())
        }
        val recommendationType = java.lang.String.join("a", Collections.nCopies(101, "a"))
        val limit = generateRandomInteger(1, 10)
        val offset = generateRandomInteger(20, 40)
        try {
            RerankingRecommendationRequest(
                userId, itemIds, limit, offset,
                recommendationType
            )
            Assert.fail()
        } catch (e: IllegalArgumentException) {
            Assert.assertEquals(e.message, recommendationTypeExceptionMessage)
        } catch (e: Error) {
            Assert.fail()
        }
    }

    @Test
    fun testGetEmptyRerankingRecommendation() {
        val userId = ""
        val itemIds: MutableList<String> = ArrayList()
        for (i in 0..49) {
            itemIds.add(generateUUID())
        }
        val limit = generateRandomInteger(1, 10)
        val offset = generateRandomInteger(20, 40)
        try {
            RerankingRecommendationRequest(userId, itemIds, limit, offset)
            Assert.fail()
        } catch (e: IllegalArgumentException) {
            Assert.assertEquals(e.message, userIdExceptionMessage)
        } catch (e: Error) {
            Assert.fail()
        }
    }

    @Test
    fun testGetZeroLimitRecommendation() {
        val userId = generateUUID()
        val itemIds: MutableList<String> = ArrayList()
        for (i in 0..49) {
            itemIds.add(generateUUID())
        }
        val limit = 0
        val offset = generateRandomInteger(20, 40)
        try {
            RerankingRecommendationRequest(userId, itemIds, limit, offset)
            Assert.fail()
        } catch (e: IllegalArgumentException) {
            Assert.assertEquals(e.message, limitExceptionMessage)
        } catch (e: Error) {
            Assert.fail()
        }
    }

    @Test
    fun testGetTooSmallLimitRecommendation() {
        val userId = generateUUID()
        val itemIds: MutableList<String> = ArrayList()
        for (i in 0..49) {
            itemIds.add(generateUUID())
        }
        val limit = -1
        val offset = generateRandomInteger(20, 40)
        try {
            RerankingRecommendationRequest(userId, itemIds, limit, offset)
            Assert.fail()
        } catch (e: IllegalArgumentException) {
            Assert.assertEquals(e.message, limitExceptionMessage)
        } catch (e: Error) {
            Assert.fail()
        }
    }

    @Test
    fun testGetZeroOffsetRecommendation() {
        val userId = generateUUID()
        val itemIds: MutableList<String> = ArrayList()
        for (i in 0..49) {
            itemIds.add(generateUUID())
        }
        val limit = generateRandomInteger(20, 40)
        val offset = 0
        val recommendation: RecommendationRequest = RerankingRecommendationRequest(userId, itemIds, limit, offset)
        checkSuccessfulGetRerankingRecommendation(recommendation, userId)
    }

    @Test
    fun testGetTooSmallOffsetRecommendation() {
        val userId = generateUUID()
        val itemIds: MutableList<String> = ArrayList()
        for (i in 0..49) {
            itemIds.add(generateUUID())
        }
        val limit = generateRandomInteger(20, 40)
        val offset = -1
        try {
            RerankingRecommendationRequest(userId, itemIds, limit, offset)
            Assert.fail()
        } catch (e: IllegalArgumentException) {
            Assert.assertEquals(e.message, offsetExceptionMessage)
        } catch (e: Error) {
            Assert.fail()
        }
    }

    @Test
    fun testGetEmptyTypeRecommendation() {
        val userId = generateUUID()
        val itemIds: MutableList<String> = ArrayList()
        for (i in 0..49) {
            itemIds.add(generateUUID())
        }
        val recommendationType = ""
        val limit = generateRandomInteger(1, 10)
        val offset = generateRandomInteger(20, 40)
        try {
            RerankingRecommendationRequest(
                userId, itemIds, limit, offset,
                recommendationType
            )
            Assert.fail()
        } catch (e: IllegalArgumentException) {
            Assert.assertEquals(e.message, recommendationTypeExceptionMessage)
        } catch (e: Error) {
            Assert.fail()
        }
    }

    @Test
    fun testGetTooBigItemIdsRecommendation() {
        val userId = generateUUID()
        val itemIds: MutableList<String> = ArrayList()
        for (i in 0..1000000) {
            itemIds.add(generateUUID())
        }
        val recommendationType = ""
        val limit = generateRandomInteger(1, 10)
        val offset = generateRandomInteger(20, 40)
        try {
            RerankingRecommendationRequest(
                userId, itemIds, limit, offset,
                recommendationType
            )
            Assert.fail()
        } catch (e: IllegalArgumentException) {
            Assert.assertEquals(e.message, itemIdsExceptionMessage)
        } catch (e: Error) {
            Assert.fail()
        }
    }

    @Test
    fun testGetTooLongItemIdsRecommendation() {
        val userId = generateUUID()
        val itemIds: MutableList<String> = ArrayList()
        for (i in 0..49) {
            itemIds.add(generateUUID())
        }
        itemIds.add(java.lang.String.join("a", Collections.nCopies(101, "a")))
        val recommendationType = ""
        val limit = generateRandomInteger(1, 10)
        val offset = generateRandomInteger(20, 40)
        try {
            RerankingRecommendationRequest(
                userId, itemIds, limit, offset,
                recommendationType
            )
            Assert.fail()
        } catch (e: IllegalArgumentException) {
            Assert.assertEquals(e.message, itemIdInListExceptionMessage)
        } catch (e: Error) {
            Assert.fail()
        }
    }

    companion object {
        private const val clientId = "test"
        private const val clientSecret = "KVPzvdHTPWnt0xaEGc2ix-eqPXFCdEV5zcqolBr_h1k" // this secret key is for
                                                                                       // testing purposes only
        private const val recLogTableName = "rec_log_test"
        private const val recLogTablePartitionKey = "user_id"
        private const val recLogTableSortKey = "timestamp"
        private const val recLogRecommendations = "recommendations"
        private const val userIdExceptionMessage = "Length of user id must be between 1 and 100."
        private const val itemIdsExceptionMessage = "Length of item_ids must be between 1 and 1000,000."
        private const val itemIdInListExceptionMessage = "Length of item id in item id list must be between 1 and 100."
        private const val recommendationTypeExceptionMessage =
            "Length of recommendation type must be between 1 and 100."
        private const val limitExceptionMessage = "Limit must be between 1 and 1000,000."
        private const val offsetExceptionMessage = "Offset must be between 0 and 1000,000."
        private val region = Region.AP_NORTHEAST_2
    }
}
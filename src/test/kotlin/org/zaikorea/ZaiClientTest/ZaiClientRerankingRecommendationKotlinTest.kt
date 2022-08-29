package org.zaikorea.ZaiClientTest

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.zaikorea.ZaiClient.ZaiClient
import org.zaikorea.ZaiClient.exceptions.ZaiClientException
import org.zaikorea.ZaiClient.request.RecommendationRequest
import org.zaikorea.ZaiClient.request.RerankingRecommendationRequest
import org.zaikorea.ZaiClient.request.UserRecommendationRequest
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException
import software.amazon.awssdk.services.dynamodb.model.QueryRequest
import java.io.IOException
import java.util.*
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

    private fun getRecLog(partitionValue: String): Map<String, String>? {
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
        val limit = recommendation.limit
        val offset = recommendation.offset
        val recommendationType = recommendation.recommendationType
        val options = recommendation.options
        val mapper = ObjectMapper().registerModule(KotlinModule())
        var optionsObj: Map<String, Int>? = null
        if (options != null) {
            try {
                optionsObj = mapper.readValue(options);
            } catch (e: JsonProcessingException) {
                throw RuntimeException(e)
            }
        }
        val builder = StringBuilder()
        optionsObj?.forEach { (k: String, v: Int) ->
            builder.append(
                "$k:$v"
            ).append("|")
        }
            ?: builder.append("|")
        val expectedOptions = builder.toString()
        try {
            val response = testClient!!.getRecommendations(recommendation)

            // Response Testing
            val responseItems = response.items
            for (i in 0 until recommendation.limit) {
                val expectedItem = (userId ?: "None") + "|" +
                        recommendationType + "|" +
                        expectedOptions + String.format("ITEM_ID_%d", i + offset)
                Assert.assertEquals(expectedItem, responseItems[i])
            }
            Assert.assertEquals(response.items.size.toLong(), limit.toLong())
            Assert.assertEquals(response.count.toLong(), limit.toLong())

            // Log testing unavailable when userId is null
            if (userId == null) return

            // Check log
            val logItem = getRecLog(userId)
            Assert.assertNotNull(logItem)
            Assert.assertNotEquals(logItem!!.size.toLong(), 0)
            Assert.assertEquals(
                logItem[recLogRecommendations]!!.split(",".toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray().size.toLong(),
                response.items.size
                    .toLong()
            )
            Assert.assertTrue(deleteRecLog(userId))
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
            itemIds.add(String.format("ITEM_ID_%d", i))
        }
        val limit = generateRandomInteger(1, 10)
        val offset = generateRandomInteger(20, 40)
        val recommendationType = "category_page"
        val recommendation: RecommendationRequest = RerankingRecommendationRequest.Builder(userId, itemIds)
            .limit(limit)
            .offset(offset)
            .recommendationType(recommendationType)
            .build()
        checkSuccessfulGetRerankingRecommendation(recommendation, userId)
    }

    @Test
    fun testGetRerankingRecommendation_2() {
        val userId = generateUUID()
        val itemIds: MutableList<String> = ArrayList()
        for (i in 0..49) {
            itemIds.add(String.format("ITEM_ID_%d", i))
        }
        val limit = generateRandomInteger(1, 10)
        val offset = generateRandomInteger(20, 40)
        val recommendation: RecommendationRequest = RerankingRecommendationRequest.Builder(userId, itemIds)
            .limit(limit)
            .offset(offset)
            .build()
        checkSuccessfulGetRerankingRecommendation(recommendation, userId)
    }

    @Test
    fun testGetRerankingRecommendation_3() {
        val userId = generateUUID()
        val itemIds: MutableList<String> = ArrayList()
        for (i in 0..49) {
            itemIds.add(String.format("ITEM_ID_%d", i))
        }
        val limit = generateRandomInteger(1, 10)
        val recommendation: RecommendationRequest = RerankingRecommendationRequest.Builder(userId, itemIds)
            .limit(limit)
            .build()
        checkSuccessfulGetRerankingRecommendation(recommendation, userId)
    }

    @Test
    fun testGetRerankingRecommendation_4() {
        val userId = generateUUID()
        val itemIds: MutableList<String> = ArrayList()
        for (i in 0..49) {
            itemIds.add(String.format("ITEM_ID_%d", i))
        }
        val limit = generateRandomInteger(1, 10)
        val recommendationType = "category_page"
        val recommendation: RecommendationRequest = RerankingRecommendationRequest.Builder(userId, itemIds)
            .limit(limit)
            .recommendationType(recommendationType)
            .build()
        checkSuccessfulGetRerankingRecommendation(recommendation, userId)
    }

    @Test
    fun testGetRerankingRecommendation_5() {
        val userId = generateUUID()
        val itemIds: MutableList<String> = ArrayList()
        for (i in 0..49) {
            itemIds.add(String.format("ITEM_ID_%d", i))
        }
        val recommendationType = "category_page"
        val recommendation: RecommendationRequest = RerankingRecommendationRequest.Builder(userId, itemIds)
            .recommendationType(recommendationType)
            .build()
        checkSuccessfulGetRerankingRecommendation(recommendation, userId)
    }

    @Test
    fun testGetRerankingRecommendation_6() {
        val userId = generateUUID()
        val itemIds: MutableList<String> = ArrayList()
        for (i in 0..49) {
            itemIds.add(String.format("ITEM_ID_%d", i))
        }
        val recommendation: RecommendationRequest = RerankingRecommendationRequest.Builder(userId, itemIds)
            .build()
        checkSuccessfulGetRerankingRecommendation(recommendation, userId)
    }

    @Test
    fun testGetRerankingRecommendation_7() {
        val userId = generateUUID()
        val itemIds: MutableList<String> = ArrayList()
        for (i in 0..49) {
            itemIds.add(String.format("ITEM_ID_%d", i))
        }
        val limit = generateRandomInteger(1, 10)
        val offset = generateRandomInteger(20, 40)
        val recommendationType = "home_page"
        val map: MutableMap<String?, Int?> = HashMap()
        map["call_type"] = 1
        map["response_type"] = 2
        val recommendation: RecommendationRequest = UserRecommendationRequest.Builder(userId, limit)
            .offset(offset)
            .recommendationType(recommendationType)
            .options(map)
            .build()
        checkSuccessfulGetRerankingRecommendation(recommendation, userId)
    }

    @Test
    fun testGetNullRerankingRecommendation_1() {
        val userId: String? = null
        val itemIds: MutableList<String> = ArrayList()
        for (i in 0..49) {
            itemIds.add(String.format("ITEM_ID_%d", i))
        }
        val limit = generateRandomInteger(1, 10)
        val offset = generateRandomInteger(20, 40)
        val recommendationType = "category_page"
        val recommendation: RecommendationRequest = RerankingRecommendationRequest.Builder(userId, itemIds)
            .limit(limit)
            .offset(offset)
            .recommendationType(recommendationType)
            .build()
        checkSuccessfulGetRerankingRecommendation(recommendation, userId)
    }

    @Test
    fun testGetNullRerankingRecommendation_2() {
        val userId: String? = null
        val itemIds: MutableList<String> = ArrayList()
        for (i in 0..49) {
            itemIds.add(String.format("ITEM_ID_%d", i))
        }
        val limit = generateRandomInteger(1, 10)
        val offset = generateRandomInteger(20, 40)
        val recommendation: RecommendationRequest = RerankingRecommendationRequest.Builder(userId, itemIds)
            .limit(limit)
            .offset(offset)
            .build()
        checkSuccessfulGetRerankingRecommendation(recommendation, userId)
    }

    @Test
    fun testGetNullRerankingRecommendation_3() {
        val userId: String? = null
        val itemIds: MutableList<String> = ArrayList()
        for (i in 0..49) {
            itemIds.add(String.format("ITEM_ID_%d", i))
        }
        val limit = generateRandomInteger(1, 10)
        val recommendationType = "category_page"
        val recommendation: RecommendationRequest = RerankingRecommendationRequest.Builder(userId, itemIds)
            .limit(limit)
            .recommendationType(recommendationType)
            .build()
        checkSuccessfulGetRerankingRecommendation(recommendation, userId)
    }

    @Test
    fun testGetNullRerankingRecommendation_4() {
        val userId: String? = null
        val itemIds: MutableList<String> = ArrayList()
        for (i in 0..49) {
            itemIds.add(String.format("ITEM_ID_%d", i))
        }
        val limit = generateRandomInteger(1, 10)
        val recommendation: RecommendationRequest = RerankingRecommendationRequest.Builder(userId, itemIds)
            .limit(limit)
            .build()
        checkSuccessfulGetRerankingRecommendation(recommendation, userId)
    }

    @Test
    fun testGetNullRerankingRecommendation_5() {
        val userId: String? = null
        val itemIds: MutableList<String> = ArrayList()
        for (i in 0..49) {
            itemIds.add(String.format("ITEM_ID_%d", i))
        }
        val recommendationType = "category_page"
        val recommendation: RecommendationRequest = RerankingRecommendationRequest.Builder(userId, itemIds)
            .recommendationType(recommendationType)
            .build()
        checkSuccessfulGetRerankingRecommendation(recommendation, userId)
    }

    @Test
    fun testGetNullRerankingRecommendation_6() {
        val userId: String? = null
        val itemIds: MutableList<String> = ArrayList()
        for (i in 0..49) {
            itemIds.add(String.format("ITEM_ID_%d", i))
        }
        val recommendation: RecommendationRequest = RerankingRecommendationRequest.Builder(userId, itemIds)
            .build()
        checkSuccessfulGetRerankingRecommendation(recommendation, userId)
    }

    @Test
    fun testGetNullRerankingRecommendation_7() {
        val userId: String? = null
        val itemIds: MutableList<String> = ArrayList()
        for (i in 0..49) {
            itemIds.add(String.format("ITEM_ID_%d", i))
        }
        val limit = generateRandomInteger(1, 10)
        val offset = generateRandomInteger(20, 40)
        val recommendationType = "home_page"
        val map: MutableMap<String?, Int?> = HashMap()
        map["call_type"] = 1
        map["response_type"] = 2
        val recommendation: RecommendationRequest = UserRecommendationRequest.Builder(userId, limit)
            .offset(offset)
            .recommendationType(recommendationType)
            .options(map)
            .build()
        checkSuccessfulGetRerankingRecommendation(recommendation, userId)
    }

    @Test
    fun testGetRerankingRecommendationWrongClientId() {
        val userId = generateUUID()
        val itemIds: MutableList<String> = ArrayList()
        for (i in 0..49) {
            itemIds.add(String.format("ITEM_ID_%d", i))
        }
        val limit = generateRandomInteger(1, 10)
        val offset = generateRandomInteger(20, 40)
        val recommendation: RecommendationRequest = RerankingRecommendationRequest.Builder(userId, itemIds)
            .limit(limit)
            .offset(offset)
            .build()
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
            itemIds.add(String.format("ITEM_ID_%d", i))
        }
        val limit = generateRandomInteger(1, 10)
        val offset = generateRandomInteger(20, 40)
        val recommendation: RecommendationRequest = RerankingRecommendationRequest.Builder(userId, itemIds)
            .limit(limit)
            .offset(offset)
            .build()
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
            itemIds.add(String.format("ITEM_ID_%d", i))
        }
        val limit = generateRandomInteger(1, 10)
        val offset = generateRandomInteger(20, 40)
        try {
            RerankingRecommendationRequest.Builder(userId, itemIds)
                .limit(limit)
                .offset(offset)
                .build()
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
            itemIds.add(String.format("ITEM_ID_%d", i))
        }
        val limit = 1000001
        val offset = generateRandomInteger(20, 40)
        try {
            RerankingRecommendationRequest.Builder(userId, itemIds)
                .limit(limit)
                .offset(offset)
                .build()
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
            itemIds.add(String.format("ITEM_ID_%d", i))
        }
        val limit = generateRandomInteger(20, 40)
        val offset = 1000001
        try {
            RerankingRecommendationRequest.Builder(userId, itemIds)
                .limit(limit)
                .offset(offset)
                .build()
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
            itemIds.add(String.format("ITEM_ID_%d", i))
        }
        val recommendationType = java.lang.String.join("a", Collections.nCopies(101, "a"))
        val limit = generateRandomInteger(1, 10)
        val offset = generateRandomInteger(20, 40)
        try {
            RerankingRecommendationRequest.Builder(userId, itemIds)
                .limit(limit)
                .offset(offset)
                .recommendationType(recommendationType)
                .build()
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
            itemIds.add(String.format("ITEM_ID_%d", i))
        }
        val limit = generateRandomInteger(1, 10)
        val offset = generateRandomInteger(20, 40)
        try {
            RerankingRecommendationRequest.Builder(userId, itemIds)
                .limit(limit)
                .offset(offset)
                .build()
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
            itemIds.add(String.format("ITEM_ID_%d", i))
        }
        val limit = 0
        val offset = generateRandomInteger(20, 40)
        try {
            RerankingRecommendationRequest.Builder(userId, itemIds)
                .limit(limit)
                .offset(offset)
                .build()
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
            itemIds.add(String.format("ITEM_ID_%d", i))
        }
        val limit = -1
        val offset = generateRandomInteger(20, 40)
        try {
            RerankingRecommendationRequest.Builder(userId, itemIds)
                .limit(limit)
                .offset(offset)
                .build()
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
            itemIds.add(String.format("ITEM_ID_%d", i))
        }
        val limit = generateRandomInteger(20, 40)
        val offset = 0
        val recommendation: RecommendationRequest = RerankingRecommendationRequest.Builder(userId, itemIds)
            .limit(limit)
            .offset(offset)
            .build()
        checkSuccessfulGetRerankingRecommendation(recommendation, userId)
    }

    @Test
    fun testGetTooSmallOffsetRecommendation() {
        val userId = generateUUID()
        val itemIds: MutableList<String> = ArrayList()
        for (i in 0..49) {
            itemIds.add(String.format("ITEM_ID_%d", i))
        }
        val limit = generateRandomInteger(20, 40)
        val offset = -1
        try {
            RerankingRecommendationRequest.Builder(userId, itemIds)
                .limit(limit)
                .offset(offset)
                .build()
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
            itemIds.add(String.format("ITEM_ID_%d", i))
        }
        val recommendationType = ""
        val limit = generateRandomInteger(1, 10)
        val offset = generateRandomInteger(20, 40)
        try {
            RerankingRecommendationRequest.Builder(userId, itemIds)
                .limit(limit)
                .offset(offset)
                .recommendationType(recommendationType)
                .build()
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
        for (i in 0..1000001 - 1) {
            itemIds.add(String.format("ITEM_ID_%d", i))
        }
        val recommendationType = ""
        val limit = generateRandomInteger(1, 10)
        val offset = generateRandomInteger(20, 40)
        try {
            RerankingRecommendationRequest.Builder(userId, itemIds)
                .limit(limit)
                .offset(offset)
                .recommendationType(recommendationType)
                .build()
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
            itemIds.add(String.format("ITEM_ID_%d", i))
        }
        itemIds.add(java.lang.String.join("a", Collections.nCopies(101, "a")))
        val recommendationType = ""
        val limit = generateRandomInteger(1, 10)
        val offset = generateRandomInteger(20, 40)
        try {
            RerankingRecommendationRequest.Builder(userId, itemIds)
                .limit(limit)
                .offset(offset)
                .recommendationType(recommendationType)
                .build()
            Assert.fail()
        } catch (e: IllegalArgumentException) {
            Assert.assertEquals(e.message, itemIdInListExceptionMessage)
        } catch (e: Error) {
            Assert.fail()
        }
    }

    @Test
    fun testGetTooLongOptionsRerankingRecommendation() {
        val userId = generateUUID()
        val itemIds: MutableList<String> = ArrayList()
        for (i in 0..49) {
            itemIds.add(String.format("ITEM_ID_%d", i))
        }
        val limit = generateRandomInteger(1, 10)
        val offset = generateRandomInteger(20, 40)
        val recommendationType = "home_page"
        val map: MutableMap<String?, Int?> = HashMap()
        map["call_type"] = 1
        map["response_type"] = 2
        map[java.lang.String.join("a", Collections.nCopies(1000, "a"))] = 3
        try {
            RerankingRecommendationRequest.Builder(userId, itemIds)
                .limit(limit)
                .offset(offset)
                .recommendationType(recommendationType)
                .options(map)
                .build()
            Assert.fail()
        } catch (e: IllegalArgumentException) {
            Assert.assertEquals(e.message, optionsExceptionMessage)
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
        private const val itemIdsExceptionMessage = "Length of item_ids must be between 1 and 1,000,000."
        private const val itemIdInListExceptionMessage = "Length of item id in item id list must be between 1 and 100."
        private const val recommendationTypeExceptionMessage =
            "Length of recommendation type must be between 1 and 100."
        private const val limitExceptionMessage = "Limit must be between 1 and 1,000,000."
        private const val offsetExceptionMessage = "Offset must be between 0 and 1,000,000."
        private const val optionsExceptionMessage = "Length of options must be less than or equal to 1000 when converted to string."
        private val region = Region.AP_NORTHEAST_2
    }
}
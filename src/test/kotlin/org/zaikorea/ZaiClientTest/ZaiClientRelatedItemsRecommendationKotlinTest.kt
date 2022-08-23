package org.zaikorea.ZaiClientTest

import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.zaikorea.ZaiClient.ZaiClient
import org.zaikorea.ZaiClient.exceptions.ZaiClientException
import org.zaikorea.ZaiClient.request.RecommendationRequest
import org.zaikorea.ZaiClient.request.RelatedItemsRecommendationRequest
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException
import software.amazon.awssdk.services.dynamodb.model.QueryRequest
import java.io.IOException
import java.util.*
import java.util.concurrent.ThreadLocalRandom

class ZaiClientRelatedItemsRecommendationKotlinTest {
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

    private fun checkSuccessfulGetRelatedRecommendation(recommendation: RecommendationRequest, itemId: String) {
        var itemId: String? = itemId
        try {
            val response = testClient!!.getRecommendations(recommendation)
            val limit = recommendation.limit
            var logItem = getRecLog(itemId)
            if (itemId == null) {
                itemId = "null"
                logItem = getRecLog("null")
            }

            // assertNotNull(logItem);
            // assertNotEquals(logItem.size(), 0);
            // assertEquals(logItem.get(recLogRecommendations).split(",").length,
            // response.getItems().size());
            Assert.assertEquals(response.items.size.toLong(), limit.toLong())
            Assert.assertEquals(response.count.toLong(), limit.toLong())
            // assertTrue(deleteRecLog(itemId));
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
    fun testGetRelatedRecommendation_1() {
        val itemId = generateUUID()
        val limit = generateRandomInteger(1, 10)
        val offset = generateRandomInteger(20, 40)
        val recommendationType = "product_detail_page"
        val recommendation: RecommendationRequest = RelatedItemsRecommendationRequest.Builder(itemId, limit)
            .offset(offset)
            .build()
        checkSuccessfulGetRelatedRecommendation(recommendation, itemId)
    }

    @Test
    fun testGetRelatedRecommendation_2() {
        val itemId = generateUUID()
        val limit = generateRandomInteger(1, 10)
        val offset = generateRandomInteger(20, 40)
        val recommendation: RecommendationRequest = RelatedItemsRecommendationRequest.Builder(itemId, limit)
            .offset(offset)
            .build()
        checkSuccessfulGetRelatedRecommendation(recommendation, itemId)
    }

    @Test
    fun testGetRelatedRecommendation_3() {
        val itemId = generateUUID()
        val limit = generateRandomInteger(1, 10)
        val recommendation: RecommendationRequest = RelatedItemsRecommendationRequest.Builder(itemId, limit)
            .build()
        checkSuccessfulGetRelatedRecommendation(recommendation, itemId)
    }

    @Test
    fun testGetRelatedRecommendation_4() {
        val itemId = generateUUID()
        val limit = generateRandomInteger(1, 10)
        val recommendationType = "product_detail_page"
        val recommendation: RecommendationRequest = RelatedItemsRecommendationRequest.Builder(itemId, limit)
            .recommendationType(recommendationType)
            .build()
        checkSuccessfulGetRelatedRecommendation(recommendation, itemId)
    }

    @Test
    fun testGetNullRelatedRecommendation() {
        val itemId: String? = null
        val limit = generateRandomInteger(1, 10)
        val offset = generateRandomInteger(20, 40)
        val recommendationType = "product_detail_page"
        try {
            RelatedItemsRecommendationRequest.Builder(itemId, limit)
                .offset(offset)
                .recommendationType(recommendationType)
                .build()
            Assert.fail()
        } catch (e: IllegalArgumentException) {
            Assert.assertEquals(e.message, itemIdExceptionMessage)
        } catch (e: Error) {
            Assert.fail()
        }
    }

    @Test
    fun testGetRelatedRecommendationWrongClientId() {
        val itemId = generateUUID()
        val limit = generateRandomInteger(1, 10)
        val offset = generateRandomInteger(20, 40)
        val recommendation: RecommendationRequest = RelatedItemsRecommendationRequest.Builder(itemId, limit)
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
    fun testGetRelatedRecommendationWrongSecret() {
        val itemId = generateUUID()
        val limit = generateRandomInteger(1, 10)
        val offset = generateRandomInteger(20, 40)
        val recommendation: RecommendationRequest = RelatedItemsRecommendationRequest.Builder(itemId, limit)
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
    fun testGetTooLongRelatedItemRecommendation() {
        val itemId = java.lang.String.join("a", Collections.nCopies(101, "a"))
        val limit = generateRandomInteger(1, 10)
        val offset = generateRandomInteger(20, 40)
        try {
            RelatedItemsRecommendationRequest.Builder(itemId, limit)
                .offset(offset)
                .build()
            Assert.fail()
        } catch (e: IllegalArgumentException) {
            Assert.assertEquals(e.message, itemIdExceptionMessage)
        } catch (e: Error) {
            Assert.fail()
        }
    }

    @Test
    fun testGetTooBigLimitRecommendation() {
        val itemId = generateUUID()
        val limit = 1000001
        val offset = generateRandomInteger(20, 40)
        try {
            RelatedItemsRecommendationRequest.Builder(itemId, limit)
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
        val itemId = generateUUID()
        val limit = generateRandomInteger(20, 40)
        val offset = 1000001
        try {
            RelatedItemsRecommendationRequest.Builder(itemId, limit)
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
        val itemId = generateUUID()
        val recommendationType = java.lang.String.join("a", Collections.nCopies(101, "a"))
        val limit = generateRandomInteger(1, 10)
        val offset = generateRandomInteger(20, 40)
        try {
            RelatedItemsRecommendationRequest.Builder(itemId, limit)
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
    fun testGetEmptyRelatedRecommendation() {
        val itemId = ""
        val limit = generateRandomInteger(1, 10)
        val offset = generateRandomInteger(20, 40)
        try {
            RelatedItemsRecommendationRequest.Builder(itemId, limit)
                .offset(offset)
                .build()
            Assert.fail()
        } catch (e: IllegalArgumentException) {
            Assert.assertEquals(e.message, itemIdExceptionMessage)
        } catch (e: Error) {
            Assert.fail()
        }
    }

    @Test
    fun testGetZeroLimitRecommendation() {
        val itemId = generateUUID()
        val limit = 0
        val offset = generateRandomInteger(20, 40)
        try {
            RelatedItemsRecommendationRequest.Builder(itemId, limit)
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
        val itemId = generateUUID()
        val limit = -1
        val offset = generateRandomInteger(20, 40)
        try {
            RelatedItemsRecommendationRequest.Builder(itemId, limit)
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
        val itemId = generateUUID()
        val limit = generateRandomInteger(20, 40)
        val offset = 0
        val recommendation: RecommendationRequest = RelatedItemsRecommendationRequest.Builder(itemId, limit)
            .offset(offset)
            .build()
        checkSuccessfulGetRelatedRecommendation(recommendation, itemId)
    }

    @Test
    fun testGetTooSmallOffsetRecommendation() {
        val itemId = generateUUID()
        val limit = generateRandomInteger(20, 40)
        val offset = -1
        try {
            RelatedItemsRecommendationRequest.Builder(itemId, limit)
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
        val itemId = generateUUID()
        val recommendationType = ""
        val limit = generateRandomInteger(1, 10)
        val offset = generateRandomInteger(20, 40)
        try {
            RelatedItemsRecommendationRequest.Builder(itemId, limit)
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

    fun testGetTooLongOptionsRelatedItemsRecommendation() {
        val itemId = generateUUID()
        val limit = generateRandomInteger(1, 10)
        val offset = generateRandomInteger(20, 40)
        val recommendationType = "home_page"
        val map: MutableMap<String?, Int?> = HashMap()
        map["call_type"] = 1
        map["response_type"] = 2
        map[java.lang.String.join("a", Collections.nCopies(1000, "a"))] = 3
        try {
            RelatedItemsRecommendationRequest.Builder(itemId, limit)
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
        private const val itemIdExceptionMessage = "Length of item id must be between 1 and 100."
        private const val recommendationTypeExceptionMessage =
            "Length of recommendation type must be between 1 and 100."
        private const val limitExceptionMessage = "Limit must be between 1 and 1000,000."
        private const val offsetExceptionMessage = "Offset must be between 0 and 1000,000."
        private const val optionsExceptionMessage = "Length of options must be less than 1000 when converted to string."
        private val region = Region.AP_NORTHEAST_2
    }
}
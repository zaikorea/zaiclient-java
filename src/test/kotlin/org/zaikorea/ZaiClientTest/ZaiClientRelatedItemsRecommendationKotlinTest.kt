package org.zaikorea.ZaiClientTest

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
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
import java.security.InvalidParameterException
import java.util.*
import java.util.concurrent.ThreadLocalRandom
import kotlin.reflect.full.memberProperties

class ZaiClientRelatedItemsRecommendationKotlinTest {

    data class Metadata(
        @SerializedName("user_id")
        var userId: String? = null,

        @SerializedName("item_id")
        var itemId: String? = null,

        @SerializedName("item_ids")
        var itemIds: List<String>? = null,

        @SerializedName("limit")
        var limit: Int? = null,

        @SerializedName("offset")
        var offset: Int? = 0,

        @SerializedName("options")
        var options: MutableMap<String?, Int?> = HashMap<String?, Int?>(),

        @SerializedName("call_type")
        var callType: String? = "related-items",

        @SerializedName("recommendation_type")
        var recommendationType: String? = "product_detail_page"
    ) {

        override fun equals(other: Any?): Boolean {
            var otherMetadata: Metadata

            if (other is Metadata)
                otherMetadata = other
            else
                throw InvalidParameterException("Other must be metadata")

            for (props in Metadata::class.memberProperties) {
                if (props.get(otherMetadata) != null && props.get(otherMetadata) == props.get(this))
                    continue
                else {
                    if (props.get(otherMetadata) == null && props.get(this) == null)
                        continue
                    else
                        return false
                }
            }

            return true;
        }

    }

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

    private fun checkSuccessfulGetRelatedRecommendation(recommendation: RecommendationRequest, itemId: String, expectedMetadata: Metadata) {
        val limit = recommendation.limit
        val offset = recommendation.offset

        try {
            val response = testClient!!.getRecommendations(recommendation)

            // Response Testing
            val responseItems = response.items
            for (i in 0 until recommendation.limit) {
                val expectedItem = String.format("ITEM_ID_%d", i + offset)
                Assert.assertEquals(expectedItem, responseItems[i])
            }

            val metadata = Gson().fromJson(response.metadata, Metadata::class.java)
            Assert.assertEquals(expectedMetadata, metadata)
            Assert.assertEquals(response.items.size.toLong(), limit.toLong())
            Assert.assertEquals(response.count.toLong(), limit.toLong())

            // No log test for related recommendation, DynamoDB partition key is userID
            // multiple null userIds prevent log testing for related recommendation
        } catch (e: IOException) {
            Assert.fail()
        } catch (e: ZaiClientException) {
            Assert.fail()
        }
    }

    @Before
    fun setup() {
        testClient = ZaiClient.Builder(clientId, clientSecret)
            .connectTimeout(20)
            .readTimeout(40)
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
            .recommendationType(recommendationType)
            .build()

        var metadata = Metadata(
            itemId=itemId,
            limit=limit,
            offset=offset,
            recommendationType=recommendationType
        )

        checkSuccessfulGetRelatedRecommendation(recommendation, itemId, metadata)
    }

    @Test
    fun testGetRelatedRecommendation_2() {
        val itemId = generateUUID()
        val limit = generateRandomInteger(1, 10)
        val offset = generateRandomInteger(20, 40)
        val recommendation: RecommendationRequest = RelatedItemsRecommendationRequest.Builder(itemId, limit)
            .offset(offset)
            .build()

        var metadata: Metadata = Metadata(
            itemId=itemId,
            limit=limit,
            offset=offset
        )

        checkSuccessfulGetRelatedRecommendation(recommendation, itemId, metadata)
    }

    @Test
    fun testGetRelatedRecommendation_3() {
        val itemId = generateUUID()
        val limit = generateRandomInteger(1, 10)
        val recommendation: RecommendationRequest = RelatedItemsRecommendationRequest.Builder(itemId, limit)
            .build()

        var metadata: Metadata = Metadata(
            itemId=itemId,
            limit=limit
        )

        checkSuccessfulGetRelatedRecommendation(recommendation, itemId, metadata)
    }

    @Test
    fun testGetRelatedRecommendation_4() {
        val itemId = generateUUID()
        val limit = generateRandomInteger(1, 10)
        val recommendationType = "product_detail_page"
        val recommendation: RecommendationRequest = RelatedItemsRecommendationRequest.Builder(itemId, limit)
            .recommendationType(recommendationType)
            .build()

        var metadata = Metadata(
            itemId=itemId,
            limit=limit,
            recommendationType=recommendationType
        )

        checkSuccessfulGetRelatedRecommendation(recommendation, itemId, metadata)
    }

    @Test
    fun testGetRelatedRecommendation_5() {
        val itemId = generateUUID()
        val limit = generateRandomInteger(1, 10)
        val offset = generateRandomInteger(20, 40)
        val recommendationType = "home_page"
        val map: MutableMap<String?, Int?> = HashMap()
        map["call_type"] = 1
        map["response_type"] = 2
        val recommendation: RecommendationRequest = RelatedItemsRecommendationRequest.Builder(itemId, limit)
            .offset(offset)
            .recommendationType(recommendationType)
            .options(map)
            .build()

        var metadata = Metadata(
            itemId=itemId,
            limit=limit,
            offset=offset,
            options=map,
            recommendationType=recommendationType
        )

        checkSuccessfulGetRelatedRecommendation(recommendation, itemId, metadata)
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
            Assert.assertEquals(401, e.httpStatusCode.toLong())
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
            Assert.assertEquals(401, e.httpStatusCode.toLong())
        }
    }

    @Test
    fun testGetTooLongRelatedItemRecommendation() {
        val itemId = java.lang.String.join("a", Collections.nCopies(501, "a"))
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
        val recommendationType = java.lang.String.join("a", Collections.nCopies(501, "a"))
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

        var metadata = Metadata(
            itemId=itemId,
            limit=limit,
            offset=offset
        )

        checkSuccessfulGetRelatedRecommendation(recommendation, itemId, metadata)
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
        private const val userIdExceptionMessage = "Length of user id must be between 1 and 500."
        private const val itemIdExceptionMessage = "Length of item id must be between 1 and 500."
        private const val recommendationTypeExceptionMessage =
            "Length of recommendation type must be between 1 and 500."
        private const val limitExceptionMessage = "Limit must be between 0 and 10,000."
        private const val offsetExceptionMessage = "Offset must be between 0 and 10,000."
        private const val optionsExceptionMessage = "Length of options must be less than or equal to 1000 when converted to string."
        private val region = Region.AP_NORTHEAST_2
    }
}
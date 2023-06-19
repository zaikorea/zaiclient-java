package org.zaikorea.ZaiClientTest

import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.zaikorea.ZaiClient.ZaiClient
import org.zaikorea.ZaiClient.exceptions.ZaiClientException
import org.zaikorea.ZaiClient.request.*
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException
import software.amazon.awssdk.services.dynamodb.model.QueryRequest
import java.io.IOException
import java.security.InvalidParameterException
import java.time.Instant
import java.util.*
import java.util.concurrent.ThreadLocalRandom

class ZaiClientKotlinTest {
    private var testClient: ZaiClient? = null
    private var incorrectIdClient: ZaiClient? = null
    private var incorrectSecretClient: ZaiClient? = null
    private var ddbClient: DynamoDbClient? = null
    private val unixTimestamp: String
        private get() {
            val utcnow = Instant.now().epochSecond
            return utcnow.toString()
        }
    private val incorrectCustomEndpointMsg = "Only alphanumeric characters are allowed for custom endpoint."
    private val longLengthCustomEndpointMsg = "Custom endpoint should be less than or equal to 10."

    private fun generateUUID(): String {
        return UUID.randomUUID().toString()
    }

    private fun generateRandomInteger(min: Int, max: Int): Int {
        return ThreadLocalRandom.current().nextInt(min, max + 1)
    }

    private fun generateRandomDouble(min: Int, max: Int): Double {
        return ThreadLocalRandom.current().nextDouble(min.toDouble(), max.toDouble())
    }

    private fun generateRandomString(n: Int): String {
        val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
        return (1..n)
            .map { allowedChars.random() }
            .joinToString("")
    }

    private fun generatePageType(): String? {
        val products = arrayOf("homepage", "category", "today's pick")
        val randomIndex = Random().nextInt(products.size)
        return products[randomIndex]
    }

    private fun generateSearchQuery(): String? {
        val products = arrayOf("waterproof camera", "headphones with NAC", "book for coding")
        val randomIndex = Random().nextInt(products.size)
        return products[randomIndex]
    }

    private fun getEventLog(partitionValue: String): Map<String, String>? {
        val partitionAlias = "#pk"
        val attrNameAlias = HashMap<String, String>()
        attrNameAlias[partitionAlias] = eventTablePartitionKey
        val attrValues = HashMap<String, AttributeValue>()
        attrValues[":$eventTablePartitionKey"] = AttributeValue.builder()
            .s(partitionValue)
            .build()
        val request = QueryRequest.builder()
            .tableName(eventTableName)
            .keyConditionExpression("$partitionAlias = :$eventTablePartitionKey")
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
                    item[key] = `val`.substring(`val`.indexOf('=') + 1, `val`.lastIndex)
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

    private fun deleteEventLog(partitionValue: String): Boolean {
        val sortValue = getEventLog(partitionValue)!![eventTableSortKey]
        val keyToGet = HashMap<String, AttributeValue>()
        keyToGet[eventTablePartitionKey] = AttributeValue.builder().s(partitionValue).build()
        keyToGet[eventTableSortKey] = AttributeValue.builder().n(sortValue).build()
        val deleteReq = DeleteItemRequest.builder()
            .key(keyToGet)
            .tableName(eventTableName)
            .build()
        try {
            ddbClient!!.deleteItem(deleteReq)
        } catch (e: DynamoDbException) {
            System.err.println(e.message)
            return false
        }
        return true
    }

    private fun checkSuccessfulEventAdd(event: Event) {
        try {
            testClient!!.addEventLog(event)
            val userId = event.userId
            val timestamp = event.timestamp
            val itemId = event.itemId
            val eventType = event.eventType
            val eventValue = event.eventValue
            val isZaiRecommendation = event.isZaiRecommendation
            val from = event.from
            val logItem = getEventLog(userId)
            Assert.assertNotNull(logItem)
            Assert.assertNotEquals(logItem!!.size.toLong(), 0)
            Assert.assertEquals(logItem[eventTablePartitionKey], userId)
            Assert.assertEquals(logItem[eventTableItemIdKey], itemId)
            Assert.assertEquals(logItem[eventTableSortKey]!!.toDouble(), timestamp, 0.0001)
            Assert.assertEquals(logItem[eventTableEventTypeKey], eventType)
            Assert.assertEquals(logItem[eventTableEventValueKey], eventValue)
            Assert.assertEquals(logItem[eventTableIsZaiRecommendationKey].toBoolean(), isZaiRecommendation)
            Assert.assertEquals(logItem[eventTableFromKey], from)
            Assert.assertTrue(deleteEventLog(userId))
        } catch (e: IOException) {
            Assert.fail()
        } catch (e: ZaiClientException) {
            Assert.fail()
        }
    }

    private fun checkSuccessfulEventAdd(event: Event, isTest: Boolean) {
        try {
            val response = testClient!!.addEventLog(event, isTest)
            val userId = event.userId
            val timestamp = event.timestamp
            val itemId = event.itemId
            val eventType = event.eventType
            val eventValue = event.eventValue
            val serverTimestamp = response.timestamp
            val isZaiRecommendation = event.isZaiRecommendation
            val from = event.from
            val timeToLive: Int? = event.timeToLive
            val logItem = getEventLog(userId)

            Assert.assertNotNull(logItem)
            Assert.assertNotEquals(logItem!!.size.toLong(), 0)
            Assert.assertEquals(logItem[eventTablePartitionKey], userId)
            Assert.assertEquals(logItem[eventTableItemIdKey], itemId)
            Assert.assertEquals(logItem[eventTableSortKey]!!.toDouble(), timestamp, 0.0001)
            Assert.assertEquals(logItem[eventTableEventTypeKey], eventType)
            Assert.assertEquals(logItem[eventTableEventValueKey], eventValue)
            if (isTest) {
                Assert.assertEquals(logItem[eventTableExpirationTimeKey]!!.toInt(),
                        (serverTimestamp + timeToLive!!).toInt())
            }
            else {
                Assert.assertEquals(logItem[eventTableExpirationTimeKey]!!.toInt(),
                        (serverTimestamp + defaultDataExpirationSeconds).toInt())
            }
            Assert.assertEquals(logItem[eventTableIsZaiRecommendationKey].toBoolean(), isZaiRecommendation)
            Assert.assertEquals(logItem[eventTableFromKey], from)
            Assert.assertTrue(deleteEventLog(userId))
        } catch (e: IOException) {
            Assert.fail()
        } catch (e: ZaiClientException) {
            Assert.fail()
        }
    }

    @Before
    fun setup() {
        testClient = ZaiClient.Builder(clientId, clientSecret)
            .connectTimeout(10)
            .readTimeout(30)
            .customEndpoint("dev")
            .build()
        incorrectIdClient = ZaiClient.Builder(".$clientId", clientSecret)
            .connectTimeout(0)
            .readTimeout(0)
            .build()
        incorrectSecretClient = ZaiClient.Builder(clientId, ".$clientSecret")
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
    fun testIncorrectCustomEndpointClient_1() {
        val incorrectCustomEndpointClient: ZaiClient
        try {
            incorrectCustomEndpointClient = ZaiClient.Builder(clientId, clientSecret)
                .customEndpoint("-@dev")
                .build()
            Assert.fail()
        } catch(e: InvalidParameterException) {
            Assert.assertEquals(e.message, incorrectCustomEndpointMsg)
        }
    }

    @Test
    fun testIncorrectCustomEndpointClient_2() {
        val incorrectCustomEndpointClient: ZaiClient
        try {
            incorrectCustomEndpointClient = ZaiClient.Builder(clientId, clientSecret)
                .customEndpoint("abcdefghijklmnop")
                .build()
            Assert.fail()
        } catch(e: InvalidParameterException) {
            Assert.assertEquals(e.message, longLengthCustomEndpointMsg)
        }
    }

    /**********************************
     *     ProductDetailViewEvent     *
     **********************************/
    @Test
    fun testAddProductDetailViewEventManualTime() {
        val userId = generateUUID()
        val itemId = generateUUID()
        val timestamp = unixTimestamp.toLong()
        val event: Event = ProductDetailViewEvent(userId, itemId).setTimestamp(timestamp.toDouble())
        checkSuccessfulEventAdd(event)
    }

    @Test
    fun testAddProductDetailViewEventWithIsZaiRec() {
        val userId = generateUUID()
        val itemId = generateUUID()
        val event: Event = ProductDetailViewEvent(userId, itemId).setIsZaiRec(true)
        checkSuccessfulEventAdd(event)
    }

    @Test
    fun testAddProductDetailViewEventWithFrom() {
        val userId = generateUUID()
        val itemId = generateUUID()
        val event: Event = ProductDetailViewEvent(userId, itemId).setFrom("home")
        checkSuccessfulEventAdd(event)
    }

    @Test
    fun testAddTrueTestProductDetailViewEvent() {
        val userId = generateUUID()
        val itemId = generateUUID()
        val event: Event = ProductDetailViewEvent(userId, itemId)
        checkSuccessfulEventAdd(event, true)
    }

    @Test
    fun testAddFalseTestProductDetailViewEvent() {
        val userId = generateUUID()
        val itemId = generateUUID()
        val event: Event = ProductDetailViewEvent(userId, itemId)
        checkSuccessfulEventAdd(event, false)
    }

    @Test
    fun testAddProductDetailViewEventWrongClientId() {
        val userId = generateUUID()
        val itemId = generateUUID()
        val timestamp = unixTimestamp.toLong()
        val event: Event = ProductDetailViewEvent(userId, itemId).setTimestamp(timestamp.toDouble())
        try {
            incorrectIdClient!!.addEventLog(event)
        } catch (e: IOException) {
            Assert.fail()
        } catch (e: ZaiClientException) {
            Assert.assertEquals(401, e.httpStatusCode.toLong())
        }
    }

    @Test
    fun testAddProductDetailViewEventWrongSecret() {
        val userId = generateUUID()
        val itemId = generateUUID()
        val timestamp = unixTimestamp.toLong()
        val event: Event = ProductDetailViewEvent(userId, itemId).setTimestamp(timestamp.toDouble())
        try {
            incorrectSecretClient!!.addEventLog(event)
        } catch (e: IOException) {
            Assert.fail()
        } catch (e: ZaiClientException) {
            Assert.assertEquals(401, e.httpStatusCode.toLong())
        }
    }

    /**********************************
     *          PageViewEvent         *
     **********************************/
    @Test
    fun testAddPageViewEventWithContainsZaiRec() {
        val userId = generateUUID()
        val pageType = generatePageType()
        val event: Event = PageViewEvent(userId, pageType).setContainsZaiRec(true)
        checkSuccessfulEventAdd(event)
    }

    @Test
    fun testAddTrueTestPageViewEvent() {
        val userId = generateUUID()
        val pageType = generatePageType()
        val event: Event = PageViewEvent(userId, pageType)
        checkSuccessfulEventAdd(event, true)
    }

    @Test
    fun testAddFalseTestPageViewEvent() {
        val userId = generateUUID()
        val pageType = generatePageType()
        val event: Event = PageViewEvent(userId, pageType)
        checkSuccessfulEventAdd(event, false)
    }

    @Test
    fun testAddPageViewEventManualTime() {
        val userId = generateUUID()
        val pageType = generatePageType()
        val timestamp = unixTimestamp.toLong()
        val event: Event = PageViewEvent(userId, pageType).setTimestamp(timestamp.toDouble())
        checkSuccessfulEventAdd(event)
    }

    @Test
    fun testAddPageViewEventWrongClientId() {
        val userId = generateUUID()
        val pageType = generatePageType()
        val timestamp = unixTimestamp.toLong()
        val event: Event = PageViewEvent(userId, pageType).setTimestamp(timestamp.toDouble())
        try {
            incorrectIdClient!!.addEventLog(event)
        } catch (e: IOException) {
            Assert.fail()
        } catch (e: ZaiClientException) {
            Assert.assertEquals(401, e.httpStatusCode.toLong())
        }
    }

    @Test
    fun testAddPageViewEventWrongSecret() {
        val userId = generateUUID()
        val pageType = generatePageType()
        val timestamp = unixTimestamp.toLong()
        val event: Event = PageViewEvent(userId, pageType).setTimestamp(timestamp.toDouble())
        try {
            incorrectSecretClient!!.addEventLog(event)
        } catch (e: IOException) {
            Assert.fail()
        } catch (e: ZaiClientException) {
            Assert.assertEquals(401, e.httpStatusCode.toLong())
        }
    }

    /**********************************
     *            LikeEvent           *
     **********************************/
    @Test
    fun testAddLikeEvent() {
        val userId = generateUUID()
        val itemId = generateUUID()
        val event: Event = LikeEvent(userId, itemId)
        checkSuccessfulEventAdd(event)
    }

    @Test
    fun testAddLikeEventWithIsZaiRec() {
        val userId = generateUUID()
        val itemId = generateUUID()
        val event: Event = LikeEvent(userId, itemId).setIsZaiRec(true)
        checkSuccessfulEventAdd(event)
    }

    @Test
    fun testAddLikeEventWithFro() {
        val userId = generateUUID()
        val itemId = generateUUID()
        val event: Event = LikeEvent(userId, itemId).setFrom("home")
        checkSuccessfulEventAdd(event)
    }

    @Test
    fun testAddTrueTestLikeEvent() {
        val userId = generateUUID()
        val itemId = generateUUID()
        val event: Event = LikeEvent(userId, itemId)
        checkSuccessfulEventAdd(event, true)
    }

    @Test
    fun testAddFalseTestLikeEvent() {
        val userId = generateUUID()
        val itemId = generateUUID()
        val event: Event = LikeEvent(userId, itemId)
        checkSuccessfulEventAdd(event, false)
    }

    @Test
    fun testAddLikeEventManualTime() {
        val userId = generateUUID()
        val itemId = generateUUID()
        val timestamp = unixTimestamp.toLong()
        val event: Event = LikeEvent(userId, itemId).setTimestamp(timestamp.toDouble())
        checkSuccessfulEventAdd(event)
    }

    @Test
    fun testAddLikeEventWrongClientId() {
        val userId = generateUUID()
        val itemId = generateUUID()
        val timestamp = unixTimestamp.toLong()
        val event: Event = LikeEvent(userId, itemId).setTimestamp(timestamp.toDouble())
        try {
            incorrectIdClient!!.addEventLog(event)
        } catch (e: IOException) {
            Assert.fail()
        } catch (e: ZaiClientException) {
            Assert.assertEquals(401, e.httpStatusCode.toLong())
        }
    }

    @Test
    fun testAddLikeEventWrongSecret() {
        val userId = generateUUID()
        val itemId = generateUUID()
        val timestamp = unixTimestamp.toLong()
        val event: Event = LikeEvent(userId, itemId).setTimestamp(timestamp.toDouble())
        try {
            incorrectSecretClient!!.addEventLog(event)
        } catch (e: IOException) {
            Assert.fail()
        } catch (e: ZaiClientException) {
            Assert.assertEquals(401, e.httpStatusCode.toLong())
        }
    }

    /**********************************
     *           CartaddEvent         *
     **********************************/
    @Test
    fun testAddCartaddEvent() {
        val userId = generateUUID()
        val itemId = generateUUID()
        val event: Event = CartaddEvent(userId, itemId)
        checkSuccessfulEventAdd(event)
    }

    @Test
    fun testAddCartaddEventWithIsZaiRec() {
        val userId = generateUUID()
        val itemId = generateUUID()
        val event: Event = CartaddEvent(userId, itemId).setIsZaiRec(true)
        checkSuccessfulEventAdd(event)
    }

    @Test
    fun testAddCartaddEventWithFrom() {
        val userId = generateUUID()
        val itemId = generateUUID()
        val event: Event = CartaddEvent(userId, itemId).setFrom("home")
        checkSuccessfulEventAdd(event)
    }

    @Test
    fun testAddTrueTestCartaddEvent() {
        val userId = generateUUID()
        val itemId = generateUUID()
        val event: Event = CartaddEvent(userId, itemId)
        checkSuccessfulEventAdd(event, true)
    }

    @Test
    fun testAddFalseTestCartaddEvent() {
        val userId = generateUUID()
        val itemId = generateUUID()
        val event: Event = CartaddEvent(userId, itemId)
        checkSuccessfulEventAdd(event, false)
    }

    @Test
    fun testAddCartaddEventManualTime() {
        val userId = generateUUID()
        val itemId = generateUUID()
        val timestamp = unixTimestamp.toLong()
        val event: Event = CartaddEvent(userId, itemId).setTimestamp(timestamp.toDouble())
        checkSuccessfulEventAdd(event)
    }

    @Test
    fun testAddCartaddEventWrongClientId() {
        val userId = generateUUID()
        val itemId = generateUUID()
        val timestamp = unixTimestamp.toLong()
        val event: Event = CartaddEvent(userId, itemId).setTimestamp(timestamp.toDouble())
        try {
            incorrectIdClient!!.addEventLog(event)
        } catch (e: IOException) {
            Assert.fail()
        } catch (e: ZaiClientException) {
            Assert.assertEquals(401, e.httpStatusCode.toLong())
        }
    }

    @Test
    fun testAddCartaddEventWrongSecret() {
        val userId = generateUUID()
        val itemId = generateUUID()
        val timestamp = unixTimestamp.toLong()
        val event: Event = CartaddEvent(userId, itemId).setTimestamp(timestamp.toDouble())
        try {
            incorrectSecretClient!!.addEventLog(event)
        } catch (e: IOException) {
            Assert.fail()
        } catch (e: ZaiClientException) {
            Assert.assertEquals(401, e.httpStatusCode.toLong())
        }
    }

    /**********************************
     *            RateEvent           *
     **********************************/
    @Test
    fun testAddRateEvent() {
        val userId = generateUUID()
        val itemId = generateUUID()
        val rating = generateRandomDouble(0, 5)
        val event: Event = RateEvent(userId, itemId, rating)
        checkSuccessfulEventAdd(event)
    }

    @Test
    fun testAddRateEventWithIsZaiRec() {
        val userId = generateUUID()
        val itemId = generateUUID()
        val rating = generateRandomDouble(0, 5)
        val event: Event = RateEvent(userId, itemId, rating).setIsZaiRec(true)
        checkSuccessfulEventAdd(event)
    }

    @Test
    fun testAddTrueTestRateEvent() {
        val userId = generateUUID()
        val itemId = generateUUID()
        val rating = generateRandomDouble(0, 5)
        val event: Event = RateEvent(userId, itemId, rating)
        checkSuccessfulEventAdd(event, true)
    }

    @Test
    fun testAddFalseTestRateEvent() {
        val userId = generateUUID()
        val itemId = generateUUID()
        val rating = generateRandomDouble(0, 5)
        val event: Event = RateEvent(userId, itemId, rating)
        checkSuccessfulEventAdd(event, false)
    }

    @Test
    fun testAddRateEventManualTime() {
        val userId = generateUUID()
        val itemId = generateUUID()
        val rating = generateRandomDouble(0, 5)
        val timestamp = unixTimestamp.toLong()
        val event: Event = RateEvent(userId, itemId, rating).setTimestamp(timestamp.toDouble())
        checkSuccessfulEventAdd(event)
    }

    @Test
    fun testAddRateEventWrongClientId() {
        val userId = generateUUID()
        val itemId = generateUUID()
        val rating = generateRandomDouble(0, 5)
        val timestamp = unixTimestamp.toLong()
        val event: Event = RateEvent(userId, itemId, rating).setTimestamp(timestamp.toDouble())
        try {
            incorrectIdClient!!.addEventLog(event)
        } catch (e: IOException) {
            Assert.fail()
        } catch (e: ZaiClientException) {
            Assert.assertEquals(401, e.httpStatusCode.toLong())
        }
    }

    @Test
    fun testAddRateEventWrongSecret() {
        val userId = generateUUID()
        val itemId = generateUUID()
        val rating = generateRandomDouble(0, 5)
        val timestamp = unixTimestamp.toLong()
        val event: Event = RateEvent(userId, itemId, rating).setTimestamp(timestamp.toDouble())
        try {
            incorrectSecretClient!!.addEventLog(event)
        } catch (e: IOException) {
            Assert.fail()
        } catch (e: ZaiClientException) {
            Assert.assertEquals(401, e.httpStatusCode.toLong())
        }
    }

    /**********************************
     *          PurchaseEvent         *
     **********************************/
    @Test
    fun testAddPurchaseEvent() {
        val userId = generateUUID()
        val itemId = generateUUID()
        val price = generateRandomInteger(10000, 100000)
        val event: Event = PurchaseEvent(userId, itemId, price)
        checkSuccessfulEventAdd(event)
    }

    @Test
    fun testAddPurchaseEventWithIsZaiRec() {
        val userId = generateUUID()
        val itemId = generateUUID()
        val price = generateRandomInteger(10000, 100000)
        val event: Event = PurchaseEvent(userId, itemId, price).setIsZaiRec(true)
        checkSuccessfulEventAdd(event)
    }

    @Test
    fun testAddTrueTestPurchaseEvent() {
        val userId = generateUUID()
        val itemId = generateUUID()
        val price = generateRandomInteger(10000, 100000)
        val event: Event = PurchaseEvent(userId, itemId, price)
        checkSuccessfulEventAdd(event, true)
    }

    @Test
    fun testAddFalseTestPurchaseEvent() {
        val userId = generateUUID()
        val itemId = generateUUID()
        val price = generateRandomInteger(10000, 100000)
        val event: Event = PurchaseEvent(userId, itemId, price)
        checkSuccessfulEventAdd(event, false)
    }

    @Test
    fun testAddPurchaseEventManualTime() {
        val userId = generateUUID()
        val itemId = generateUUID()
        val price = generateRandomInteger(10000, 100000)
        val timestamp = unixTimestamp.toLong()
        val event: Event = PurchaseEvent(userId, itemId, price).setTimestamp(timestamp.toDouble())
        checkSuccessfulEventAdd(event)
    }

    @Test
    fun testAddPurchaseEventWrongClientId() {
        val userId = generateUUID()
        val itemId = generateUUID()
        val price = generateRandomInteger(10000, 100000)
        val timestamp = unixTimestamp.toLong()
        val event: Event = PurchaseEvent(userId, itemId, price).setTimestamp(timestamp.toDouble())
        try {
            incorrectIdClient!!.addEventLog(event)
        } catch (e: IOException) {
            Assert.fail()
        } catch (e: ZaiClientException) {
            Assert.assertEquals(401, e.httpStatusCode.toLong())
        }
    }

    @Test
    fun testAddPurchaseEventWrongSecret() {
        val userId = generateUUID()
        val itemId = generateUUID()
        val price = generateRandomInteger(10000, 100000)
        val timestamp = unixTimestamp.toLong()
        val event: Event = PurchaseEvent(userId, itemId, price).setTimestamp(timestamp.toDouble())
        try {
            incorrectSecretClient!!.addEventLog(event)
        } catch (e: IOException) {
            Assert.fail()
        } catch (e: ZaiClientException) {
            Assert.assertEquals(401, e.httpStatusCode.toLong())
        }
    }

    /**********************************
     *           SearchEvent          *
     **********************************/
    @Test
    fun testAddSearchEventWithIsZaiRec() {
        val userId = generateUUID()
        val searchQuery = generateSearchQuery()
        val event: Event = SearchEvent(userId, searchQuery).setIsZaiRec(true)
        checkSuccessfulEventAdd(event, true)
    }

    @Test
    fun testAddTrueTestSearchEvent() {
        val userId = generateUUID()
        val searchQuery = generateSearchQuery()
        val event: Event = SearchEvent(userId, searchQuery)
        checkSuccessfulEventAdd(event, true)
    }

    @Test
    fun testAddFalseTestSearchEvent() {
        val userId = generateUUID()
        val searchQuery = generateSearchQuery()
        val event: Event = SearchEvent(userId, searchQuery)
        checkSuccessfulEventAdd(event, false)
    }

    @Test
    fun testAddSearchEventManualTime() {
        val userId = generateUUID()
        val searchQuery = generateSearchQuery()
        val timestamp = unixTimestamp.toLong()
        val event: Event = SearchEvent(userId, searchQuery).setTimestamp(timestamp.toDouble())
        checkSuccessfulEventAdd(event)
    }

    @Test
    fun testAddSearchEventWrongClientId() {
        val userId = generateUUID()
        val searchQuery = generatePageType()
        val timestamp = unixTimestamp.toLong()
        val event: Event = SearchEvent(userId, searchQuery).setTimestamp(timestamp.toDouble())
        try {
            incorrectIdClient!!.addEventLog(event)
        } catch (e: IOException) {
            Assert.fail()
        } catch (e: ZaiClientException) {
            Assert.assertEquals(401, e.httpStatusCode.toLong())
        }
    }

    @Test
    fun testAddSearchEventWrongSecret() {
        val userId = generateUUID()
        val searchQuery = generatePageType()
        val timestamp = unixTimestamp.toLong()
        val event: Event = SearchEvent(userId, searchQuery).setTimestamp(timestamp.toDouble())
        try {
            incorrectSecretClient!!.addEventLog(event)
        } catch (e: IOException) {
            Assert.fail()
        } catch (e: ZaiClientException) {
            Assert.assertEquals(401, e.httpStatusCode.toLong())
        }
    }

    /**********************************
     *           CustomEvent          *
     **********************************/
    @Test
    fun testAddCustomEvent() {
        val userId = generateUUID()
        val itemId = generateUUID()
        val eventType = "customEventType"
        val eventValue = "customEventValue"
        val event: Event = CustomEvent(userId, itemId, eventType, eventValue)
        checkSuccessfulEventAdd(event)
    }

    @Test
    fun testAddCustomEventWithIsZaiRec() {
        val userId = generateUUID()
        val itemId = generateUUID()
        val eventType = "customEventType"
        val eventValue = "customEventValue"
        val event: Event = CustomEvent(userId, itemId, eventType, eventValue).setIsZaiRec(true)
        checkSuccessfulEventAdd(event)
    }

    @Test
    fun testAddCustomEventWithFrom() {
        val userId = generateUUID()
        val itemId = generateUUID()
        val eventType = "customEventType"
        val eventValue = "customEventValue"
        val event: Event = CustomEvent(userId, itemId, eventType, eventValue).setFrom("home")
        checkSuccessfulEventAdd(event)
    }

    @Test
    fun testAddTrueTestCustomEvent() {
        val userId = generateUUID()
        val itemId = generateUUID()
        val eventType = "customEventType"
        val eventValue = "customEventValue"
        val event: Event = CustomEvent(userId, itemId, eventType, eventValue)
        checkSuccessfulEventAdd(event, true)
    }

    @Test
    fun testAddFalseTestCustomEvent() {
        val userId = generateUUID()
        val itemId = generateUUID()
        val eventType = "customEventType"
        val eventValue = "customEventValue"
        val event: Event = CustomEvent(userId, itemId, eventType, eventValue)
        checkSuccessfulEventAdd(event, false)
    }

    @Test
    fun testAddCustomEventManualTime() {
        val userId = generateUUID()
        val itemId = generateUUID()
        val eventType = "customEventType"
        val eventValue = "customEventValue"
        val timestamp = unixTimestamp.toLong()
        val event: Event = CustomEvent(userId, itemId, eventType, eventValue).setTimestamp(timestamp.toDouble())
        checkSuccessfulEventAdd(event)
    }

    @Test
    fun testLongUserId() {
        val userId: String = generateRandomString(501)
        val itemId = generateUUID()
        val eventType = "customEventType"
        val eventValue = "customEventValue"
        try {
            val event: Event = CustomEvent(userId, itemId, eventType, eventValue)
        } catch (e: InvalidParameterException) {
            return
        }
        Assert.fail()
    }

    @Test
    fun testZeroLengthUserId() {
        val userId = ""
        val itemId = generateUUID()
        val eventType = generateUUID()
        val eventValue = generateUUID()
        try {
            val event: Event = CustomEvent(userId, itemId, eventType, eventValue)
        } catch (e: InvalidParameterException) {
            return
        }
        Assert.fail()
    }

    @Test
    fun testLongItemId() {
        val userId = generateUUID()
        val itemId: String = generateRandomString(501)
        val eventType = generateUUID()
        val eventValue = generateUUID()
        try {
            val event: Event = CustomEvent(userId, itemId, eventType, eventValue)
        } catch (e: InvalidParameterException) {
            return
        }
        Assert.fail()
    }

    @Test
    fun testZeroLengthItemId() {
        val userId = generateUUID()
        val itemId = ""
        val eventType = "customEventType"
        val eventValue = "customEventValue"
        try {
            val event: Event = CustomEvent(userId, itemId, eventType, eventValue)
        } catch (e: InvalidParameterException) {
            return
        }
        Assert.fail()
    }

    @Test
    fun testLongEventType() {
        val userId = generateUUID()
        val itemId = generateUUID()
        val eventType: String = generateRandomString(501)
        val eventValue = generateUUID()
        try {
            val event: Event = CustomEvent(userId, itemId, eventType, eventValue)
        } catch (e: InvalidParameterException) {
            return
        }
        Assert.fail()
    }


    @Test
    fun testZeroLengthEventType() {
        val userId = generateUUID()
        val itemId = generateUUID()
        val eventType = ""
        val eventValue = generateUUID()
        try {
            val event: Event = CustomEvent(userId, itemId, eventType, eventValue)
        } catch (e: InvalidParameterException) {
            return
        }
        Assert.fail()
    }

    @Test
    fun testLongEventValue() {
        val userId = generateUUID()
        val itemId = generateUUID()
        val eventType = generateUUID()
        val eventValue: String = generateRandomString(505)
        val event: Event = CustomEvent(userId, itemId, eventType, eventValue)
        Assert.assertEquals(500, event.eventValue.length)
    }


    @Test
    fun testZeroLengthEventValue() {
        val userId = generateUUID()
        val itemId = generateUUID()
        val eventType = generateUUID()
        val eventValue = ""
        try {
            val event: Event = CustomEvent(userId, itemId, eventType, eventValue)
        } catch (e: InvalidParameterException) {
            return
        }
        Assert.fail()
    }

    @Test
    fun testNegativeTimeToLive() {
        val userId = generateUUID()
        val itemId = generateUUID()
        val eventType = generateUUID()
        val eventValue = ""
        try {
            val event: Event = CustomEvent(userId, itemId, eventType, eventValue)
            event.timeToLive = -defaultDataExpirationSeconds
        } catch (e: InvalidParameterException) {
            return
        }
        Assert.fail()
    }

    @Test
    fun testLongFromValue() {
        val userId = generateUUID()
        val itemId = generateUUID()
        val eventType = generateUUID()
        val eventValue = generateUUID()
        val from = generateRandomString(501)
        val event: Event = CustomEvent(userId, itemId, eventType, eventValue).setFrom(from)
        Assert.assertEquals(500, event.from.length.toLong())
    }

    @Test
    fun testZeroLengthFromValue() {
        val userId = generateUUID()
        val itemId = generateUUID()
        val eventType = generateUUID()
        val eventValue = generateUUID()
        val from = ""
        val event: Event = CustomEvent(userId, itemId, eventType, eventValue).setFrom(from)
        Assert.assertNull(event.from)
    }

    companion object {
        private const val clientId = "test"
        private const val clientSecret =
            "KVPzvdHTPWnt0xaEGc2ix-eqPXFCdEV5zcqolBr_h1k" // this secret key is for testing purposes only
        private const val eventTableName = "events_test"
        private const val eventTablePartitionKey = "user_id"
        private const val eventTableSortKey = "timestamp"
        private const val eventTableItemIdKey = "item_id"
        private const val eventTableEventTypeKey = "event_type"
        private const val eventTableEventValueKey = "event_value"
        private const val eventTableExpirationTimeKey = "expiration_time"
        private const val eventTableIsZaiRecommendationKey = "is_zai_recommendation"
        private const val eventTableFromKey = "from"
        private const val defaultDataExpirationSeconds = 60 * 60 * 24 * 365
        private val region = Region.AP_NORTHEAST_2
    }
}
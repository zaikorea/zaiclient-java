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
            return java.lang.Long.toString(utcnow)
        }

    private fun generateUUID(): String {
        return UUID.randomUUID().toString()
    }

    private fun generateRandomInteger(min: Int, max: Int): Int {
        return ThreadLocalRandom.current().nextInt(min, max + 1)
    }

    private fun generateRandomDouble(min: Int, max: Int): Double {
        return ThreadLocalRandom.current().nextDouble(min.toDouble(), max.toDouble())
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
        attrValues[":" + eventTablePartitionKey] = AttributeValue.builder()
            .s(partitionValue)
            .build()
        val request = QueryRequest.builder()
            .tableName(eventTableName)
            .keyConditionExpression(partitionAlias + " = :" + eventTablePartitionKey)
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
            val logItem = getEventLog(userId)
            Assert.assertNotNull(logItem)
            Assert.assertNotEquals(logItem!!.size.toLong(), 0)
            Assert.assertEquals(logItem[eventTablePartitionKey], userId)
            Assert.assertEquals(logItem[eventTableItemIdKey], itemId)
            Assert.assertEquals(logItem[eventTableSortKey]!!.toDouble(), timestamp, 0.0001)
            Assert.assertEquals(logItem[eventTableEventTypeKey], eventType)
            Assert.assertEquals(logItem[eventTableEventValueKey], eventValue)
            Assert.assertTrue(deleteEventLog(userId))
        } catch (e: IOException) {
            Assert.fail()
        } catch (e: ZaiClientException) {
            Assert.fail()
        }
    }

    private fun checkSuccessfulEventUpdate(oldEvent: Event, newEvent: Event) {
        Assert.assertEquals(oldEvent.userId, newEvent.userId)
        Assert.assertEquals(oldEvent.timestamp, newEvent.timestamp, 0.0001)
        try {
            testClient!!.addEventLog(oldEvent)
            var userId = oldEvent.userId
            var timestamp = oldEvent.timestamp
            var itemId = oldEvent.itemId
            var eventType = oldEvent.eventType
            var eventValue = oldEvent.eventValue
            var logItem = getEventLog(userId)
            Assert.assertNotNull(logItem)
            Assert.assertNotEquals(logItem!!.size.toLong(), 0)
            Assert.assertEquals(logItem[eventTablePartitionKey], userId)
            Assert.assertEquals(logItem[eventTableItemIdKey], itemId)
            Assert.assertEquals(logItem[eventTableSortKey]!!.toDouble(), timestamp, 0.0001)
            Assert.assertEquals(logItem[eventTableEventTypeKey], eventType)
            Assert.assertEquals(logItem[eventTableEventValueKey], eventValue)
            testClient!!.updateEventLog(newEvent)
            userId = newEvent.userId
            timestamp = newEvent.timestamp
            itemId = newEvent.itemId
            eventType = newEvent.eventType
            eventValue = newEvent.eventValue
            logItem = getEventLog(userId)
            Assert.assertNotNull(logItem)
            Assert.assertNotEquals(logItem!!.size.toLong(), 0)
            Assert.assertEquals(logItem[eventTablePartitionKey], userId)
            Assert.assertEquals(logItem[eventTableItemIdKey], itemId)
            Assert.assertEquals(logItem[eventTableSortKey]!!.toDouble(), timestamp, 0.0001)
            Assert.assertEquals(logItem[eventTableEventTypeKey], eventType)
            Assert.assertEquals(logItem[eventTableEventValueKey], eventValue)
            Assert.assertTrue(deleteEventLog(userId))
        } catch (e: IOException) {
            Assert.fail()
        } catch (e: ZaiClientException) {
            Assert.fail()
        }
    }

    private fun checkSuccessfulEventDelete(event: Event) {
        try {
            testClient!!.addEventLog(event)
            val userId = event.userId
            val timestamp = event.timestamp
            val itemId = event.itemId
            val eventType = event.eventType
            val eventValue = event.eventValue
            val logItem = getEventLog(userId)
            Assert.assertNotNull(logItem)
            Assert.assertNotEquals(logItem!!.size.toLong(), 0)
            Assert.assertEquals(logItem[eventTablePartitionKey], userId)
            Assert.assertEquals(logItem[eventTableItemIdKey], itemId)
            Assert.assertEquals(logItem[eventTableSortKey]!!.toDouble(), timestamp, 0.0001)
            Assert.assertEquals(logItem[eventTableEventTypeKey], eventType)
            Assert.assertEquals(logItem[eventTableEventValueKey], eventValue)
            testClient!!.deleteEventLog(event)
            val newLogItem = getEventLog(userId)
            Assert.assertNotNull(newLogItem)
            Assert.assertEquals(newLogItem!!.size.toLong(), 0)
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
    fun testAddViewEvent() {
        val userId = generateUUID()
        val itemId = generateUUID()
        val event: Event = ViewEvent(userId, itemId)
        checkSuccessfulEventAdd(event)
    }

    @Test
    fun testAddViewEventManualTime() {
        val userId = generateUUID()
        val itemId = generateUUID()
        val timestamp = unixTimestamp.toLong()
        val event: Event = ViewEvent(userId, itemId, timestamp.toDouble())
        checkSuccessfulEventAdd(event)
    }

    @Test
    fun testAddViewEventWrongClientId() {
        val userId = generateUUID()
        val itemId = generateUUID()
        val timestamp = unixTimestamp.toLong()
        val event: Event = ViewEvent(userId, itemId, timestamp.toDouble())
        try {
            incorrectIdClient!!.addEventLog(event)
        } catch (e: IOException) {
            Assert.fail()
        } catch (e: ZaiClientException) {
            Assert.assertEquals(e.httpStatusCode.toLong(), 401)
        }
    }

    @Test
    fun testAddViewEventWrongSecret() {
        val userId = generateUUID()
        val itemId = generateUUID()
        val timestamp = unixTimestamp.toLong()
        val event: Event = ViewEvent(userId, itemId, timestamp.toDouble())
        try {
            incorrectSecretClient!!.addEventLog(event)
        } catch (e: IOException) {
            Assert.fail()
        } catch (e: ZaiClientException) {
            Assert.assertEquals(e.httpStatusCode.toLong(), 401)
        }
    }

    @Test
    fun testUpdateViewEvent() {
        val userId = generateUUID()
        val oldItemId = generateUUID()
        val newItemId = generateUUID()
        val oldEvent: Event = ViewEvent(userId, oldItemId)
        val newEvent: Event = ViewEvent(userId, newItemId, oldEvent.timestamp)
        checkSuccessfulEventUpdate(oldEvent, newEvent)
    }

    @Test
    fun testDeleteViewEvent() {
        val userId = generateUUID()
        val itemId = generateUUID()
        val event: Event = ViewEvent(userId, itemId)
        checkSuccessfulEventDelete(event)
    }

    @Test
    fun testAddProductDetailViewEventManualTime() {
        val userId = generateUUID()
        val itemId = generateUUID()
        val timestamp = unixTimestamp.toLong()
        val event: Event = ProductDetailViewEvent(userId, itemId, timestamp.toDouble())
        checkSuccessfulEventAdd(event)
    }

    @Test
    fun testAddProductDetailViewEventWrongClientId() {
        val userId = generateUUID()
        val itemId = generateUUID()
        val timestamp = unixTimestamp.toLong()
        val event: Event = ProductDetailViewEvent(userId, itemId, timestamp.toDouble())
        try {
            incorrectIdClient!!.addEventLog(event)
        } catch (e: IOException) {
            Assert.fail()
        } catch (e: ZaiClientException) {
            Assert.assertEquals(e.httpStatusCode.toLong(), 401)
        }
    }

    @Test
    fun testAddProductDetailViewEventWrongSecret() {
        val userId = generateUUID()
        val itemId = generateUUID()
        val timestamp = unixTimestamp.toLong()
        val event: Event = ProductDetailViewEvent(userId, itemId, timestamp.toDouble())
        try {
            incorrectSecretClient!!.addEventLog(event)
        } catch (e: IOException) {
            Assert.fail()
        } catch (e: ZaiClientException) {
            Assert.assertEquals(e.httpStatusCode.toLong(), 401)
        }
    }

    @Test
    fun testUpdateProductDetailViewEvent() {
        val userId = generateUUID()
        val oldItemId = generateUUID()
        val newItemId = generateUUID()
        val oldEvent: Event = ProductDetailViewEvent(userId, oldItemId)
        val newEvent: Event = ProductDetailViewEvent(userId, newItemId, oldEvent.timestamp)
        checkSuccessfulEventUpdate(oldEvent, newEvent)
    }

    @Test
    fun testDeleteProductDetailViewEvent() {
        val userId = generateUUID()
        val itemId = generateUUID()
        val event: Event = ProductDetailViewEvent(userId, itemId)
        checkSuccessfulEventDelete(event)
    }

    @Test
    fun testAddPageViewEventManualTime() {
        val userId = generateUUID()
        val pageType = generatePageType()
        val timestamp = unixTimestamp.toLong()
        val event: Event = PageViewEvent(userId, pageType, timestamp.toDouble())
        checkSuccessfulEventAdd(event)
    }

    @Test
    fun testAddPageViewEventWrongClientId() {
        val userId = generateUUID()
        val pageType = generatePageType()
        val timestamp = unixTimestamp.toLong()
        val event: Event = PageViewEvent(userId, pageType, timestamp.toDouble())
        try {
            incorrectIdClient!!.addEventLog(event)
        } catch (e: IOException) {
            Assert.fail()
        } catch (e: ZaiClientException) {
            Assert.assertEquals(e.httpStatusCode.toLong(), 401)
        }
    }

    @Test
    fun testAddPageViewEventWrongSecret() {
        val userId = generateUUID()
        val pageType = generatePageType()
        val timestamp = unixTimestamp.toLong()
        val event: Event = PageViewEvent(userId, pageType, timestamp.toDouble())
        try {
            incorrectSecretClient!!.addEventLog(event)
        } catch (e: IOException) {
            Assert.fail()
        } catch (e: ZaiClientException) {
            Assert.assertEquals(e.httpStatusCode.toLong(), 401)
        }
    }

    @Test
    fun testUpdatePageViewEvent() {
        val userId = generateUUID()
        val oldPageType = generatePageType()
        val newPageType = generatePageType()
        val oldEvent: Event = PageViewEvent(userId, oldPageType)
        val newEvent: Event = PageViewEvent(userId, newPageType, oldEvent.timestamp)
        checkSuccessfulEventUpdate(oldEvent, newEvent)
    }

    @Test
    fun testDeletePageViewEvent() {
        val userId = generateUUID()
        val pageType = generatePageType()
        val event: Event = PageViewEvent(userId, pageType)
        checkSuccessfulEventDelete(event)
    }

    @Test
    fun testAddLikeEvent() {
        val userId = generateUUID()
        val itemId = generateUUID()
        val event: Event = LikeEvent(userId, itemId)
        checkSuccessfulEventAdd(event)
    }

    @Test
    fun testAddLikeEventManualTime() {
        val userId = generateUUID()
        val itemId = generateUUID()
        val timestamp = unixTimestamp.toLong()
        val event: Event = LikeEvent(userId, itemId, timestamp.toDouble())
        checkSuccessfulEventAdd(event)
    }

    @Test
    fun testAddLikeEventWrongClientId() {
        val userId = generateUUID()
        val itemId = generateUUID()
        val timestamp = unixTimestamp.toLong()
        val event: Event = LikeEvent(userId, itemId, timestamp.toDouble())
        try {
            incorrectIdClient!!.addEventLog(event)
        } catch (e: IOException) {
            Assert.fail()
        } catch (e: ZaiClientException) {
            Assert.assertEquals(e.httpStatusCode.toLong(), 401)
        }
    }

    @Test
    fun testAddLikeEventWrongSecret() {
        val userId = generateUUID()
        val itemId = generateUUID()
        val timestamp = unixTimestamp.toLong()
        val event: Event = LikeEvent(userId, itemId, timestamp.toDouble())
        try {
            incorrectSecretClient!!.addEventLog(event)
        } catch (e: IOException) {
            Assert.fail()
        } catch (e: ZaiClientException) {
            Assert.assertEquals(e.httpStatusCode.toLong(), 401)
        }
    }

    @Test
    fun testUpdateLikeEvent() {
        val userId = generateUUID()
        val oldItemId = generateUUID()
        val newItemId = generateUUID()
        val oldEvent: Event = LikeEvent(userId, oldItemId)
        val newEvent: Event = LikeEvent(userId, newItemId, oldEvent.timestamp)
        checkSuccessfulEventUpdate(oldEvent, newEvent)
    }

    @Test
    fun testDeleteLikeEvent() {
        val userId = generateUUID()
        val itemId = generateUUID()
        val event: Event = LikeEvent(userId, itemId)
        checkSuccessfulEventDelete(event)
    }

    @Test
    fun testAddCartaddEvent() {
        val userId = generateUUID()
        val itemId = generateUUID()
        val event: Event = CartaddEvent(userId, itemId)
        checkSuccessfulEventAdd(event)
    }

    @Test
    fun testAddCartaddEventManualTime() {
        val userId = generateUUID()
        val itemId = generateUUID()
        val timestamp = unixTimestamp.toLong()
        val event: Event = CartaddEvent(userId, itemId, timestamp.toDouble())
        checkSuccessfulEventAdd(event)
    }

    @Test
    fun testAddCartaddEventWrongClientId() {
        val userId = generateUUID()
        val itemId = generateUUID()
        val timestamp = unixTimestamp.toLong()
        val event: Event = CartaddEvent(userId, itemId, timestamp.toDouble())
        try {
            incorrectIdClient!!.addEventLog(event)
        } catch (e: IOException) {
            Assert.fail()
        } catch (e: ZaiClientException) {
            Assert.assertEquals(e.httpStatusCode.toLong(), 401)
        }
    }

    @Test
    fun testAddCartaddEventWrongSecret() {
        val userId = generateUUID()
        val itemId = generateUUID()
        val timestamp = unixTimestamp.toLong()
        val event: Event = CartaddEvent(userId, itemId, timestamp.toDouble())
        try {
            incorrectSecretClient!!.addEventLog(event)
        } catch (e: IOException) {
            Assert.fail()
        } catch (e: ZaiClientException) {
            Assert.assertEquals(e.httpStatusCode.toLong(), 401)
        }
    }

    @Test
    fun testUpdateCartaddEvent() {
        val userId = generateUUID()
        val oldItemId = generateUUID()
        val newItemId = generateUUID()
        val oldEvent: Event = CartaddEvent(userId, oldItemId)
        val newEvent: Event = CartaddEvent(userId, newItemId, oldEvent.timestamp)
        checkSuccessfulEventUpdate(oldEvent, newEvent)
    }

    @Test
    fun testDeleteCartaddEvent() {
        val userId = generateUUID()
        val itemId = generateUUID()
        val event: Event = CartaddEvent(userId, itemId)
        checkSuccessfulEventDelete(event)
    }

    @Test
    fun testAddRateEvent() {
        val userId = generateUUID()
        val itemId = generateUUID()
        val rating = generateRandomDouble(0, 5)
        val event: Event = RateEvent(userId, itemId, rating)
        checkSuccessfulEventAdd(event)
    }

    @Test
    fun testAddRateEventManualTime() {
        val userId = generateUUID()
        val itemId = generateUUID()
        val rating = generateRandomDouble(0, 5)
        val timestamp = unixTimestamp.toLong()
        val event: Event = RateEvent(userId, itemId, rating, timestamp.toDouble())
        checkSuccessfulEventAdd(event)
    }

    @Test
    fun testAddRateEventWrongClientId() {
        val userId = generateUUID()
        val itemId = generateUUID()
        val rating = generateRandomDouble(0, 5)
        val timestamp = unixTimestamp.toLong()
        val event: Event = RateEvent(userId, itemId, rating, timestamp.toDouble())
        try {
            incorrectIdClient!!.addEventLog(event)
        } catch (e: IOException) {
            Assert.fail()
        } catch (e: ZaiClientException) {
            Assert.assertEquals(e.httpStatusCode.toLong(), 401)
        }
    }

    @Test
    fun testAddRateEventWrongSecret() {
        val userId = generateUUID()
        val itemId = generateUUID()
        val rating = generateRandomDouble(0, 5)
        val timestamp = unixTimestamp.toLong()
        val event: Event = RateEvent(userId, itemId, rating, timestamp.toDouble())
        try {
            incorrectSecretClient!!.addEventLog(event)
        } catch (e: IOException) {
            Assert.fail()
        } catch (e: ZaiClientException) {
            Assert.assertEquals(e.httpStatusCode.toLong(), 401)
        }
    }

    @Test
    fun testUpdateRateEvent() {
        val userId = generateUUID()
        val oldRating = generateRandomDouble(0, 5)
        val oldItemId = generateUUID()
        val newRating = generateRandomDouble(0, 5)
        val newItemId = generateUUID()
        val oldEvent: Event = RateEvent(userId, oldItemId, oldRating)
        val newEvent: Event = RateEvent(userId, newItemId, newRating, oldEvent.timestamp)
        checkSuccessfulEventUpdate(oldEvent, newEvent)
    }

    @Test
    fun testDeleteRateEvent() {
        val userId = generateUUID()
        val itemId = generateUUID()
        val rating = generateRandomDouble(0, 5)
        val event: Event = RateEvent(userId, itemId, rating)
        checkSuccessfulEventDelete(event)
    }

    @Test
    fun testAddPurchaseEvent() {
        val userId = generateUUID()
        val itemId = generateUUID()
        val price = generateRandomInteger(10000, 100000)
        val event: Event = PurchaseEvent(userId, itemId, price)
        checkSuccessfulEventAdd(event)
    }

    @Test
    fun testAddPurchaseEventManualTime() {
        val userId = generateUUID()
        val itemId = generateUUID()
        val price = generateRandomInteger(10000, 100000)
        val timestamp = unixTimestamp.toLong()
        val event: Event = PurchaseEvent(userId, itemId, price, timestamp.toDouble())
        checkSuccessfulEventAdd(event)
    }

    @Test
    fun testAddPurchaseEventWrongClientId() {
        val userId = generateUUID()
        val itemId = generateUUID()
        val price = generateRandomInteger(10000, 100000)
        val timestamp = unixTimestamp.toLong()
        val event: Event = PurchaseEvent(userId, itemId, price, timestamp.toDouble())
        try {
            incorrectIdClient!!.addEventLog(event)
        } catch (e: IOException) {
            Assert.fail()
        } catch (e: ZaiClientException) {
            Assert.assertEquals(e.httpStatusCode.toLong(), 401)
        }
    }

    @Test
    fun testAddPurchaseEventWrongSecret() {
        val userId = generateUUID()
        val itemId = generateUUID()
        val price = generateRandomInteger(10000, 100000)
        val timestamp = unixTimestamp.toLong()
        val event: Event = PurchaseEvent(userId, itemId, price, timestamp.toDouble())
        try {
            incorrectSecretClient!!.addEventLog(event)
        } catch (e: IOException) {
            Assert.fail()
        } catch (e: ZaiClientException) {
            Assert.assertEquals(e.httpStatusCode.toLong(), 401)
        }
    }

    @Test
    fun testUpdatePurchaseEvent() {
        val userId = generateUUID()
        val oldPrice = generateRandomInteger(10000, 100000)
        val oldItemId = generateUUID()
        val newPrice = generateRandomInteger(10000, 100000)
        val newItemId = generateUUID()
        val oldEvent: Event = PurchaseEvent(userId, oldItemId, oldPrice)
        val newEvent: Event = PurchaseEvent(userId, newItemId, newPrice, oldEvent.timestamp)
        checkSuccessfulEventUpdate(oldEvent, newEvent)
    }

    @Test
    fun testDeletePurchaseEvent() {
        val userId = generateUUID()
        val itemId = generateUUID()
        val price = generateRandomInteger(10000, 100000)
        val event: Event = PurchaseEvent(userId, itemId, price)
        checkSuccessfulEventDelete(event)
    }


    @Test
    fun testAddSearchEventManualTime() {
        val userId = generateUUID()
        val searchQuery = generateSearchQuery()
        val timestamp = unixTimestamp.toLong()
        val event: Event = SearchEvent(userId, searchQuery, timestamp.toDouble())
        checkSuccessfulEventAdd(event)
    }

    @Test
    fun testAddSearchEventWrongClientId() {
        val userId = generateUUID()
        val searchQuery = generatePageType()
        val timestamp = unixTimestamp.toLong()
        val event: Event = SearchEvent(userId, searchQuery, timestamp.toDouble())
        try {
            incorrectIdClient!!.addEventLog(event)
        } catch (e: IOException) {
            Assert.fail()
        } catch (e: ZaiClientException) {
            Assert.assertEquals(e.httpStatusCode.toLong(), 401)
        }
    }

    @Test
    fun testAddSearchEventWrongSecret() {
        val userId = generateUUID()
        val searchQuery = generatePageType()
        val timestamp = unixTimestamp.toLong()
        val event: Event = SearchEvent(userId, searchQuery, timestamp.toDouble())
        try {
            incorrectSecretClient!!.addEventLog(event)
        } catch (e: IOException) {
            Assert.fail()
        } catch (e: ZaiClientException) {
            Assert.assertEquals(e.httpStatusCode.toLong(), 401)
        }
    }

    @Test
    fun testUpdateSearchEvent() {
        val userId = generateUUID()
        val oldSearchQuery = generatePageType()
        val newSearchQuery = generatePageType()
        val oldEvent: Event = SearchEvent(userId, oldSearchQuery)
        val newEvent: Event = SearchEvent(userId, newSearchQuery, oldEvent.timestamp)
        checkSuccessfulEventUpdate(oldEvent, newEvent)
    }

    @Test
    fun testDeleteSearchEvent() {
        val userId = generateUUID()
        val searchQuery = generatePageType()
        val event: Event = SearchEvent(userId, searchQuery)
        checkSuccessfulEventDelete(event)
    }

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
    fun testAddCustomEventManualTime() {
        val userId = generateUUID()
        val itemId = generateUUID()
        val eventType = "customEventType"
        val eventValue = "customEventValue"
        val timestamp = unixTimestamp.toLong()
        val event: Event = CustomEvent(userId, itemId, eventType, eventValue, timestamp.toDouble())
        checkSuccessfulEventAdd(event)
    }

    @Test
    fun testUpdateCustomEvent() {
        val userId = generateUUID()
        val oldItemId = generateUUID()
        val oldEventType = "oldEventType"
        val oldEventValue = "oldEventValue"
        val newItemId = generateUUID()
        val newEventType = "newEventType"
        val newEventValue = "newEventValue"
        val oldEvent: Event = CustomEvent(userId, oldItemId, oldEventType, oldEventValue)
        val newEvent: Event = CustomEvent(userId, newItemId, newEventType, newEventValue, oldEvent.timestamp)
        checkSuccessfulEventUpdate(oldEvent, newEvent)
    }

    @Test
    fun testDeleteCustomEvent() {
        val userId = generateUUID()
        val itemId = generateUUID()
        val eventType = "customEventType"
        val eventValue = "customEventValue"
        val event: Event = CustomEvent(userId, itemId, eventType, eventValue)
        checkSuccessfulEventDelete(event)
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
        private val region = Region.AP_NORTHEAST_2
    }
}
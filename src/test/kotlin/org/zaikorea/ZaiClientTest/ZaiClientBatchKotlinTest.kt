package org.zaikorea.ZaiClientTest

import org.zaikorea.ZaiClient.ZaiClient
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import java.util.UUID
import java.util.HashMap
import java.math.BigDecimal
import org.zaikorea.ZaiClient.request.EventBatch
import java.io.IOException
import org.zaikorea.ZaiClient.exceptions.ZaiClientException
import org.zaikorea.ZaiClient.exceptions.EmptyBatchException
import org.junit.Before
import org.junit.After
import org.junit.Assert
import org.junit.Test
import org.zaikorea.ZaiClient.configs.Config
import org.zaikorea.ZaiClient.exceptions.BatchSizeLimitExceededException
import org.zaikorea.ZaiClient.request.PurchaseEventBatch
import org.zaikorea.ZaiClient.request.CustomEventBatch
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.dynamodb.model.*
import java.lang.Exception
import java.time.Instant
import java.util.concurrent.ThreadLocalRandom

class ZaiClientBatchKotlinTest {
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

    private fun generateRandomDouble(min: Int, max: Int): Double {
        return ThreadLocalRandom.current().nextDouble(min.toDouble(), max.toDouble())
    }

    private fun getEventLogWithTimestamp(partitionValue: String, sortValue: Double): Map<String, String>? {
        val keyToGet = HashMap<String, AttributeValue>()
        val sortValue_ = BigDecimal(sortValue)
        keyToGet[eventTablePartitionKey] = AttributeValue.builder()
            .s(partitionValue)
            .build()
        keyToGet[eventTableSortKey] = AttributeValue.builder()
            .n(sortValue_.toString())
            .build()
        val request = GetItemRequest.builder().tableName(eventTableName).key(keyToGet).build()
        return try {
            val returnedItems = ddbClient!!.getItem(request)
            val returnedItem = returnedItems.item()
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

    private fun deleteEventLogWithTimestamp(partitionValue: String, sortValue: Double): Boolean {
        val keyToGet = HashMap<String, AttributeValue>()
        val sortValue_ = BigDecimal(sortValue)
        keyToGet[eventTablePartitionKey] = AttributeValue.builder().s(partitionValue).build()
        keyToGet[eventTableSortKey] = AttributeValue.builder().n(sortValue_.toString()).build()
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

    private fun checkSuccessfulEventBatchAdd(eventBatch: EventBatch) {
        try {
            testClient!!.addEventLog(eventBatch)
            val events = eventBatch.eventList
            for (event in events) {
                val userId = event.userId
                val timestamp = event.timestamp
                val itemId = event.itemId
                val eventType = event.eventType
                val eventValue = event.eventValue
                val logItem = getEventLogWithTimestamp(userId, timestamp)
                Assert.assertNotNull(logItem)
                Assert.assertNotEquals(logItem!!.size.toLong(), 0)
                Assert.assertEquals(logItem[eventTablePartitionKey], userId)
                Assert.assertEquals(logItem[eventTableItemIdKey], itemId)
                Assert.assertEquals(
                    logItem[eventTableSortKey]!!.toDouble(), timestamp, 0.0001
                )
                Assert.assertEquals(logItem[eventTableEventTypeKey], eventType)
                Assert.assertEquals(logItem[eventTableEventValueKey], eventValue)
                Assert.assertTrue(deleteEventLogWithTimestamp(userId, timestamp))
            }
        } catch (e: IOException) {
            e.printStackTrace()
            Assert.fail()
        } catch (e: ZaiClientException) {
            e.printStackTrace()
            Assert.fail()
        } catch (e: EmptyBatchException) {
            e.printStackTrace()
            Assert.fail()
        }
    }

    private fun checkSuccessfulEventBatchDelete(eventBatch: EventBatch) {
        try {
            testClient!!.addEventLog(eventBatch)
            val events = eventBatch.eventList
            for (event in events) {
                val userId = event.userId
                val timestamp = event.timestamp
                val itemId = event.itemId
                val eventType = event.eventType
                val eventValue = event.eventValue
                val logItem = getEventLogWithTimestamp(userId, timestamp)
                Assert.assertNotNull(logItem)
                Assert.assertNotEquals(logItem!!.size.toLong(), 0)
                Assert.assertEquals(logItem[eventTablePartitionKey], userId)
                Assert.assertEquals(logItem[eventTableItemIdKey], itemId)
                Assert.assertEquals(
                    logItem[eventTableSortKey]!!.toDouble(), timestamp, 0.0001
                )
                Assert.assertEquals(logItem[eventTableEventTypeKey], eventType)
                Assert.assertEquals(logItem[eventTableEventValueKey], eventValue)
            }
            testClient!!.deleteEventLog(eventBatch)
            for (event in events) {
                val userId = event.userId
                val timestamp = event.timestamp
                val newLogItem = getEventLogWithTimestamp(userId, timestamp)
                Assert.assertNotNull(newLogItem)
                Assert.assertEquals(newLogItem!!.size.toLong(), 0)
            }
        } catch (e: IOException) {
            Assert.fail()
        } catch (e: ZaiClientException) {
            Assert.fail()
        } catch (e: EmptyBatchException) {
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
    fun testAddPurchaseEventBatch() {
        val userId = generateUUID()
        try {
            val eventBatch = PurchaseEventBatch(userId)
            val NUM = 10
            for (i in 0 until NUM) {
                val itemId = generateUUID()
                val price = generateRandomInteger(10000, 100000)
                eventBatch.addEventItem(itemId, price)
            }
            checkSuccessfulEventBatchAdd(eventBatch)
        } catch (e: Exception) {
            Assert.fail()
        }
    }

    @Test
    fun testAddPurchaseEventBatchManualTime() {
        val userId = generateUUID()
        val timestamp = unixTimestamp.toLong()
        try {
            val eventBatch = PurchaseEventBatch(userId, timestamp.toDouble())
            val NUM = 10
            for (i in 0 until NUM) {
                val itemId = generateUUID()
                val price = generateRandomInteger(10000, 100000)
                eventBatch.addEventItem(itemId, price)
            }
            checkSuccessfulEventBatchAdd(eventBatch)
        } catch (e: Exception) {
            Assert.fail()
        }
    }

    @Test
    fun testDeleteItemPurchaseEventBatch() {
        val userId = generateUUID()
        var itemId = generateUUID()
        var price: Int
        try {
            val eventBatch = PurchaseEventBatch(userId)
            val NUM = 10
            for (i in 0 until NUM) {
                itemId = generateUUID()
                price = generateRandomInteger(10000, 100000)
                eventBatch.addEventItem(itemId, price)
            }
            eventBatch.deleteEventItem(itemId)
            checkSuccessfulEventBatchAdd(eventBatch)
        } catch (e: Exception) {
            Assert.fail()
        }
    }

    @Test
    fun testDeleteItemPurchaseEventBatchWithPrice() {
        val userId = generateUUID()
        var itemId = generateUUID()
        var price = 0
        try {
            val eventBatch = PurchaseEventBatch(userId)
            val NUM = 10
            for (i in 0 until NUM) {
                itemId = generateUUID()
                price = generateRandomInteger(10000, 100000)
                eventBatch.addEventItem(itemId, price)
            }
            eventBatch.deleteEventItem(itemId, price)
            checkSuccessfulEventBatchAdd(eventBatch)
        } catch (e: Exception) {
            Assert.fail()
        }
    }

    @Test
    fun testDeletePurchaseEventBatch() {
        val userId = generateUUID()
        try {
            val eventBatch = PurchaseEventBatch(userId)
            val NUM = 10
            for (i in 0 until NUM) {
                val itemId = generateUUID()
                val price = generateRandomInteger(10000, 100000)
                eventBatch.addEventItem(itemId, price)
            }
            checkSuccessfulEventBatchDelete(eventBatch)
        } catch (e: Exception) {
            Assert.fail()
        }
    }

    @Test
    fun testAddPurchaseEventBatchExceedMaxLimit() {
        val userId = generateUUID()
        try {
            val eventBatch = PurchaseEventBatch(userId)
            val NUM = Config.batchRequestCap + 1
            for (i in 0 until NUM) {
                val itemId = generateUUID()
                val price = generateRandomInteger(10000, 100000)
                eventBatch.addEventItem(itemId, price)
            }
            checkSuccessfulEventBatchAdd(eventBatch)
        } catch (e: Exception) {
            Assert.assertTrue(e is BatchSizeLimitExceededException)
        }
    }

    @Test
    fun testAddCustomEventBatch() {
        val userId = generateUUID()
        val eventType = "customEventType"
        try {
            val eventBatch = CustomEventBatch(userId, eventType)
            val NUM = 10
            for (i in 0 until NUM) {
                val itemId = generateUUID()
                val rate = generateRandomDouble(0, 5)
                eventBatch.addEventItem(itemId, java.lang.Double.toString(rate))
            }
            checkSuccessfulEventBatchAdd(eventBatch)
        } catch (e: Exception) {
            Assert.fail()
        }
    }

    @Test
    fun testAddCustomEventBatchManualTime() {
        val userId = generateUUID()
        val eventType = "customEventType"
        val timestamp = unixTimestamp.toLong()
        try {
            val eventBatch = CustomEventBatch(userId, eventType, timestamp.toDouble())
            val NUM = 10
            for (i in 0 until NUM) {
                val itemId = generateUUID()
                val rate = generateRandomDouble(0, 5)
                eventBatch.addEventItem(itemId, java.lang.Double.toString(rate))
            }
            checkSuccessfulEventBatchAdd(eventBatch)
        } catch (e: Exception) {
            Assert.fail()
        }
    }

    @Test
    fun testDeleteItemCustomEventBatch() {
        val userId = generateUUID()
        var itemId = generateUUID()
        val eventType = "customEventType"
        var eventValue: String? = ""
        val timestamp = unixTimestamp.toLong()
        try {
            val eventBatch = CustomEventBatch(userId, eventType, timestamp.toDouble())
            val NUM = 10
            for (i in 0 until NUM) {
                itemId = generateUUID()
                eventValue = java.lang.Double.toString(generateRandomDouble(0, 5))
                eventBatch.addEventItem(itemId, eventValue)
            }
            eventBatch.deleteEventItem(itemId)
            checkSuccessfulEventBatchAdd(eventBatch)
        } catch (e: Exception) {
            Assert.fail()
        }
    }

    @Test
    fun testDeleteItemCustomEventBatchWithValue() {
        val userId = generateUUID()
        var itemId = generateUUID()
        val eventType = "customEventType"
        var eventValue: String? = ""
        val timestamp = unixTimestamp.toLong()
        try {
            val eventBatch = CustomEventBatch(userId, eventType, timestamp.toDouble())
            val NUM = 10
            for (i in 0 until NUM) {
                itemId = generateUUID()
                eventValue = java.lang.Double.toString(generateRandomDouble(0, 5))
                eventBatch.addEventItem(itemId, eventValue)
            }
            eventBatch.deleteEventItem(itemId, eventValue)
            checkSuccessfulEventBatchAdd(eventBatch)
        } catch (e: Exception) {
            Assert.fail()
        }
    }

    @Test
    fun testDeleteCustomEventBatch() {
        val userId = generateUUID()
        val eventType = "customEventType"
        try {
            val eventBatch = CustomEventBatch(userId, eventType)
            val NUM = 10
            for (i in 0 until NUM) {
                val itemId = generateUUID()
                val rate = generateRandomDouble(0, 5)
                eventBatch.addEventItem(itemId, java.lang.Double.toString(rate))
            }
            checkSuccessfulEventBatchDelete(eventBatch)
        } catch (e: Exception) {
            Assert.fail()
        }
    }

    @Test
    fun testAddCustomEventBatchExceedMaxLimit() {
        val userId = generateUUID()
        val eventType = "customEventType"
        try {
            val eventBatch = CustomEventBatch(userId, eventType)
            val NUM = Config.batchRequestCap
            for (i in 0 until NUM) {
                val itemId = generateUUID()
                val rate = generateRandomDouble(0, 5)
                eventBatch.addEventItem(itemId, java.lang.Double.toString(rate))
            }
            checkSuccessfulEventBatchAdd(eventBatch)
        } catch (e: Exception) {
            Assert.assertTrue(e is BatchSizeLimitExceededException)
        }
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
        @JvmStatic
        val unixTimestamp: String
            get() {
                val utcnow = Instant.now().epochSecond
                return java.lang.Long.toString(utcnow)
            }
    }
}
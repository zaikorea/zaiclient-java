# Z.Ai REST Client for Java / Kotlin

![JitPack](https://jitpack.io/v/zaikorea/zaiclient-java.svg)  

Z.Ai API를 Java / Kotlin 환경에서 간편하게 이용할 수 있는 REST client SDK입니다.

[com.squareup.retrofit2](https://github.com/square/retrofit) 모듈을 이용하며, Java 1.8 이상을 지원합니다.



## Documentation

SDK에 대한 보다 구조적인 설명은 [ZaiDocs](https://docs.zaikorea.org/)에서 확인 가능합니다.



## Installation

이 SDK는 [JitPack](https://jitpack.io/#zaikorea/zaiclient-java) 을 통해 배포되어 있습니다. 다음과 같이 프로젝트에 추가할 수 있습니다.



### Gradle (build.gradle)

`build.gradle`의 repository 끝부분에 다음을 추가합니다.

```css
repositories {
  ...
  maven { url 'https://jitpack.io' }
}
```

Dependency를 추가합니다.

```css
dependencies {
  implementation 'com.github.zaikorea:zaiclient-java:v0.1.0'
}
```



### Gradle (build.gradle.kts)

`build.gradle.kts`의 repository 끝부분에 다음을 추가합니다.

```css
repositories {
  ...
  maven {
    url = uri("https://jitpack.io")
  }
}
```

Dependency를 추가합니다.

```css
dependencies {
  implementation("com.github.zaikorea:zaiclient-java:v0.1.0")
}
```



### Maven

다음 태그들을 프로젝트의 POM 파일에 추가합니다.

```xml
  <repositories>
    <repository>
      <id>jitpack.io</id>
      <url>https://jitpack.io</url>
    </repository>
  </repositories>
```

```xml
  <dependency>
    <groupId>com.github.zaikorea</groupId>
    <artifactId>zaiclient-java</artifactId>
    <version>v0.1.0</version>
  </dependency>
```



## Quickstart

유저 행동을 기록하기 위해 `ZaiClient.addEventLog(Event event)` 함수를 사용합니다.

### Java

```java
import org.zaikorea.ZaiClient.ZaiClient;
import org.zaikorea.ZaiClient.exceptions.ZaiClientException;
import org.zaikorea.ZaiClient.request.ViewEvent;
import org.zaikorea.ZaiClient.response.EventLoggerResponse;

...
  
ZaiClient zaiClient = new ZaiClient("{자이 UUID}", "{자이 API Secret}");

try {
  // 고객 행동 중 product detail view를 Z.Ai ML 데이터베이스에 기록
  zaiClient.addEventLog(new ViewEvent("{고객 식별자}", "{제품 식별자}"));
} catch (ZaiClientException e) {
  // TODO: Error handling
} catch (IOException e) {
  // TODO: Error handling
}
```

### Kotlin

```kotlin
import org.zaikorea.ZaiClient.ZaiClient
import org.zaikorea.ZaiClient.request.ViewEvent
  
...

val zaiClient = ZaiClient("{자이 UUID}", "{자이 API Secret}")

// 고객 행동 중 product detail view를 Z.Ai ML 데이터베이스에 기록
zaiClient.addEventLog(ViewEvent("{고객 식별자}", "{제품 식별자}"))
```



## API Client Instantiation

Z.Ai로부터 발급받은 `Z.Ai Client ID`와 `Z.Ai API Secret`을 이용해 Z.Ai API client의 인스턴스를 생성할 수 있습니다. 이후 이 인스턴스의 함수들을 호출하여 아래의 `Event Operations`를 수행 가능합니다.

### Java

```Java
import org.zaikorea.ZaiClient.ZaiClient;

...

String clientId = "YOUR_ZAI_CLIENT_ID";
String clientSecret = "YOUR_ZAI_API_SECRET";
ZaiClient zaiClient = new ZaiClient(clientId, clientSecret);
```

### Kotlin

```kotlin
import org.zaikorea.ZaiClient.ZaiClient;

...

val clientId = "YOUR_ZAI_CLIENT_ID"
val clientSecret = "YOUR_ZAI_API_SECRET"
val zaiClient = ZaiClient(clientId, clientSecret)
```



## Event Operations

Z.Ai API client 인스턴스를 이용해 `Event Operation` 수행이 가능합니다. 수행 가능한 Operations는 현재 버전 기준으로는 다음과 같습니다. Event 인스턴스의 종류는 아래 `Events` 섹션에서 확인 가능합니다.

- `addEventLog` : 고객 event를 Z.Ai ML 데이터베이스에 기록합니다. 
- `updateEventLog` : 이미 기록된 고객 event를 Z.Ai ML 데이터베이스에서 수정합니다. 이때 고객 식별자와 이벤트 시간이 동일해야 수정이 성공적으로 이루어질 수 있습니다.
- `deleteEventLog` : 이미 기록된 고객 event를 Z.Ai ML 데이터베이스에서 삭제합니다. 이때 고객 식별자와 이벤트 시간이 동일해야 삭제가 성공적으로 이루어질 수 있습니다. 

위 operation들은 각각 exception을 발생시킵니다. 이에 대한 자세한 설명은 아래 `Error Handling` 섹션에서 확인할 수 있습니다. 또한 operation의 리턴값으로 주어지는 `EventLoggerResponse` 오브젝트는, `getMessage()` 함수를 통해 서버로부터 리턴된 메시지를 `String` 값으로 확인할 수 있습니다. 다만, 현재는 성공적으로 작업이 수행되었다는 메시지만 리턴하므로 이 값을 반드시 받아 올 필요는 없습니다.

### Java

```java
try {
  // 새로운 product detail view event 기록
  Event event = new ViewEvent("CUSTOMER_ID", "PRODUCT_ID");
  zaiClient.addEventLog(event);
  
  // 위에서 기록된 event의 product ID를 수정
  Event newEvent = new ViewEvent("CUSTOMER_ID", "NEW_PRODUCT_ID", event.getTimestamp());
  zaiClient.updateEventLog(newEvent);
  
  // 위에서 기록된 event를 삭제
  zaiClient.deleteEventLog(newEvent);
} catch (ZaiClientException | IOException e) {
  // TODO: Error handling
}
```

### Kotlin

```kotlin
// 새로운 product detail view event 기록
val event = ViewEvent("CUSTOMER_ID", "PRODUCT_ID")
zaiClient.addEventLog(event)

// 위에서 기록된 event의 product ID를 수정
val newEvent = ViewEvent("CUSTOMER_ID", "NEW_PRODUCT_ID", event.timestamp)
zaiClient.updateEventLog(newEvent)

// 위에서 기록된 event를 삭제
zaiClient.deleteEventLog(newEvent)
```



## Events

`Event` 오브젝트의 종류는 현재 다음과 같이 5가지가 있으며, 도메인과 고객 행동 종류에 따라 적절히 만들어 사용할 수 있습니다.

- `ViewEvent` : 고객이 제품 상세보기 페이지에 들어가거나, 영상을 시청하는 등 클릭을 통해 어떠한 아이템에 대해 관심을 보이는 행동을 보일 때 사용 가능한 이벤트입니다.
- `LikeEvent` : 고객이 제품이나 영상 등에 좋아요를 누르거나, 찜을 해두는 등 적극적인 관심을 보이는 행동을 할 때 사용 가능한 이벤트입니다.
- `CartaddEvent` : 이커머스에서 고객이 제품을 장바구니에 담는 행동을 할 때 사용 가능한 이벤트입니다.
- `PurchaseEvent` : 고객이 제품을 구매하거나, 영상을 전부 시청하는 등 어떠한 아이템을 완전히 소비했을 때 사용 가능한 이벤트입니다. 추가 인풋으로 `int price`가 들어가며, 제품 구매를 통해 발생한 매출액을 기록하면 됩니다 (동일 제품을 복수개 구매한 경우 총 매출액).
- `RateEvent` : 고객이 제품이나 영상 등에 별점을 매길 때 사용 가능한 이벤트입니다. 추가 인풋으로 `double rating`이 들어가며, 고객이 입력한 점수를 넣으면 됩니다. 점수의 scale에 제한은 없으나, 가급적 0.0에서 1.0 사이의 실수값으로 정규화하여 넣는 것이 ML 모델 성능에 더 긍정적입니다.

### Java

```java
import org.zaikorea.ZaiClient.request.*;

...

Event viewEvent = new ViewEvent("CUSTOMER_ID", "PRODUCT_ID");
Event likeEvent = new LikeEvent("CUSTOMER_ID", "PRODUCT_ID");
Event cartaddEvent = new CartaddEvent("CUSTOMER_ID", "PRODUCT_ID");
Event purchaseEvent = new PurchaseEvent("CUSTOMER_ID", "PRODUCT_ID", 100000);
Event rateEvent = new RateEvent("CUSTOMER_ID", "PRODUCT_ID", 0.5);
```

### Kotlin

```kotlin
import org.zaikorea.ZaiClient.request.*

...

val viewEvent = ViewEvent("CUSTOMER_ID", "PRODUCT_ID")
val likeEvent = LikeEvent("CUSTOMER_ID", "PRODUCT_ID")
val cartaddEvent = CartaddEvent("CUSTOMER_ID", "PRODUCT_ID")
val purchaseEvent = PurchaseEvent("CUSTOMER_ID", "PRODUCT_ID", 100000)
val rateEvent = RateEvent("CUSTOMER_ID", "PRODUCT_ID", 0.5)
```

위와 같은 방식으로 `Event` 오브젝트를 생성하면 오브젝트 생성 시점을 기준으로 이벤트 발생 시간이 결정됩니다. 만약 이벤트 발생 시간을 다르게 기록해야 한다면 아래와 같이 `timestamp`를 직접 입력하는 생성자를 사용할 수 있습니다. 이때 `timestamp`는 유닉스 타임스탬프 값을 사용하며, 가급적 소수점 단위까지(ms단위까지 판별 가능) 있는 값을 사용하는 것을 권장합니다.

### Java

```Java
import org.zaikorea.ZaiClient.request.*;

...

double timestamp = Event.getCurrentUnixTimestamp();

Event viewEvent = new ViewEvent("CUSTOMER_ID", "PRODUCT_ID", timestamp);
Event likeEvent = new LikeEvent("CUSTOMER_ID", "PRODUCT_ID", timestamp);
Event cartaddEvent = new CartaddEvent("CUSTOMER_ID", "PRODUCT_ID", timestamp);
Event purchaseEvent = new PurchaseEvent("CUSTOMER_ID", "PRODUCT_ID", 100000, timestamp);
Event rateEvent = new RateEvent("CUSTOMER_ID", "PRODUCT_ID", 0.5, timestamp);
```

### Kotlin

```kotlin
import org.zaikorea.ZaiClient.request.*;

...

val timestamp = Event.getCurrentUnixTimestamp()

val viewEvent = ViewEvent("CUSTOMER_ID", "PRODUCT_ID", timestamp)
val likeEvent = LikeEvent("CUSTOMER_ID", "PRODUCT_ID", timestamp)
val cartaddEvent = CartaddEvent("CUSTOMER_ID", "PRODUCT_ID", timestamp)
val purchaseEvent = PurchaseEvent("CUSTOMER_ID", "PRODUCT_ID", 100000, timestamp)
val rateEvent = RateEvent("CUSTOMER_ID", "PRODUCT_ID", 0.5, timestamp)
```



## Error Handling

위 operation 실행 중 API 응답 오류 상황에서는 `ZaiClientException`가, 네트워크 장애 상황에서는 `IOException`가 throw됩니다. `ZaiClientException` 상황에서는 `ZaiClientException.getHttpStatusCode()`를 통해 어떠한 오류 상황인지 명확하게 파악 가능합니다.

### Java

```java
try {
  zaiClient.addEventLog(new ViewEvent("CUSTOMER_ID", "PRODUCT_ID"));
} catch (ZaiClientException e) {
  System.out.println(e.getMessage());

  int httpStatus = e.getHttpStatusCode();

  switch (httpStatus) {
    case 403:
      // Handle 403 error
      break;
    case 422:
      // Handle 422 error
      break;
    case 500:
      // Handle 500 error
      break;
    ...
  }
} catch (IOException e) {
  // Server connection error
  e.printStackTrace();
}
```

### Kotlin

```kotlin
try {
  zaiClient.addEventLog(ViewEvent("CUSTOMER_ID", "PRODUCT_ID"));
} catch (e: ZaiClientException) {
  println(e.message);

  val httpStatus = e.httpStatusCode;

  when (httpStatus) {
    403 -> // Handle 403 error
    422 -> // Handle 422 error
    500 -> // Handle 500 error
    ...
  }
} catch (e: IOException) {
  // Server connection error
  e.printStackTrace();
}
```


# Z.Ai REST Client for Java / Kotlin

![JitPack](https://jitpack.io/v/zaikorea/zaiclient-java.svg)

Z.Ai API를 Java / Kotlin 환경에서 간편하게 이용할 수 있는 REST client SDK입니다.

[com.squareup.retrofit2](https://github.com/square/retrofit) 모듈을 이용하며, Java 1.8 이상을 지원합니다.



## Documentation

SDK에 대한 documentation은 [ZAiDocs](https://docs.zaikorea.org/)에서 확인 가능합니다.



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
  implementation 'com.github.zaikorea:zaiclient-java:v3.1.1'
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
  implementation("com.github.zaikorea:zaiclient-java:v3.1.1")
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
    <version>v3.1.1</version>
  </dependency>
```

# Spring Boot / Spring Kafka Share Groups (KIP-932) POC
Simple Spring Boot / Spring Kafka Share Groups example for KIP-932 (Queues for Kafka)

## Fixes

The fix for the issues have been provided in:

* Pull request: [4374](https://github.com/spring-projects/spring-kafka/pull/4374)
* See commit: https://github.com/spring-projects/spring-kafka/commit/813b9af333a6b812617e970f34ca0ae447dda7fb

To see the fix in action using Spring Boot 4.1.0-SNAPSHOT & Spring Kafka 4.1.0-SNAPSHOT: check out branch: [fix](https://github.com/j-tim/spring-kafka-share-groups-kip-932-poc/tree/fix)

See also Spring Kafka documentation: [What’s New in 4.1 Since 4.0 - Share Consumer Acknowledgment Modes](https://docs.spring.io/spring-kafka/reference/4.1-SNAPSHOT/whats-new.html#x41-share-ack-mode)

## Versions 

* Java: 25
* Spring Boot: 4.0.4
* Spring Kafka: 4.0.4
* Apache Kafka Clients: 4.1.2 
* Apache Kafka Broker: 4.2.0

## Build and run

```bash
./mvnw clean package
```

```bash
docker compose up -d
```

```bash
docker compose down
```

## Issues

### Issue 1 ([#4369](https://github.com/spring-projects/spring-kafka/issues/4369)): Setting explicit acknowledgment via ShareKafkaListenerContainerFactory container properties does not enable `explicit` acknowledgment mode for Share Groups

The documentation states there are two ways to enable explicit acknowledgment in shared groups in Spring Kafka:

* [Option 1](https://docs.spring.io/spring-kafka/reference/4.1/kafka/kafka-queues.html#_option_1_using_kafka_client_configuration) is working `props.put(ConsumerConfig.SHARE_ACKNOWLEDGEMENT_MODE_CONFIG, "explicit");`.
* [Option 2](https://docs.spring.io/spring-kafka/reference/4.1/kafka/kafka-queues.html#_option_2_using_spring_container_configuration): is not working `factory.getContainerProperties().setExplicitShareAcknowledgment(true);`

#### How to reproduce:

Acknowledge mode: `explicit`

1. Start the application

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=explicit
```

2. Publish a valid message to the topic

```bash
curl -X POST "http://localhost:8080/produce?message=Hi"
```

3. Observe the logs, the setting on the `ShareConsumerConfig` show we are still using `implicit` acknowledgment mode

```log
2026-03-26T11:05:46.702+01:00  INFO 55104 --- [           main] o.a.kafka.common.config.AbstractConfig   : ShareConsumerConfig values: 
        share.acknowledgement.mode = implicit
```

4. Observe the logs, we can't acknowledge the message we cause we are in `implicit` acknowledgment mode. Resulting in this exception in our Spring Kafka consumer:

```log
2026-03-26T11:06:02.761+01:00 ERROR 55104 --- [Container#0-C-1] s.k.l.ShareKafkaMessageListenerContainer : Failed to process queued acknowledgment for record: ConsumerRecord(topic = high-throughput-topic, partition = 0, leaderEpoch = 0, offset = 2, CreateTime = 1774519562719, deliveryCount = 1, serialized key size = -1, serialized value size = 2, headers = RecordHeaders(headers = [], isReadOnly = false), key = null, value = Hi)

java.lang.IllegalStateException: Implicit acknowledgement of delivery is being used.
        at org.apache.kafka.clients.consumer.internals.ShareConsumerImpl.ensureExplicitAcknowledgement(ShareConsumerImpl.java:1060) ~[kafka-clients-4.1.2.jar:na]
        at org.apache.kafka.clients.consumer.internals.ShareConsumerImpl.acknowledge(ShareConsumerImpl.java:688) ~[kafka-clients-4.1.2.jar:na]
        at org.apache.kafka.clients.consumer.KafkaShareConsumer.acknowledge(KafkaShareConsumer.java:507) ~[kafka-clients-4.1.2.jar:na]
        at org.springframework.kafka.listener.ShareKafkaMessageListenerContainer$ShareListenerConsumer.processQueuedAcknowledgments(ShareKafkaMessageListenerContainer.java:489) ~[spring-kafka-4.0.4.jar:4.0.4]
        at org.springframework.kafka.listener.ShareKafkaMessageListenerContainer$ShareListenerConsumer.run(ShareKafkaMessageListenerContainer.java:344) ~[spring-kafka-4.0.4.jar:4.0.4]
        at java.base/java.util.concurrent.CompletableFuture$AsyncRun.run(CompletableFuture.java:1825) ~[na:na]
        at java.base/java.lang.Thread.run(Thread.java:1474) ~[na:na]
```

#### Expected behavior: 

When applying option 2, the `ShareConsumerConfig` should show we are using `explicit` acknowledgment mode, and we should be able to acknowledge messages in our Spring Kafka consumer.

### Issue 2: ShareGroup consumers in Spring Kafka are not correctly rejected on error in `implicit` acknowledgment mode

Acknowledge mode: `implicit`

The documentation states: 

> // Record is auto-acknowledged as ACCEPT on success, REJECT on error

See: [Implicit Mode Example (acknowledgment is null)](https://docs.spring.io/spring-kafka/reference/4.1/kafka/kafka-queues.html#_implicit_mode_example_acknowledgment_is_null)

But we observed the Kafka record is not correctly rejected on error more over it's actually acknowledged.

#### How to reproduce:

1. Start the application

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=implicit
```

2. Publish a message to the topic, to trigger the exception flow:

```bash
curl -X POST "http://localhost:8080/produce?message=kaboom"
```

3. Observe the logs, the setting on the `ShareConsumerConfig` show we are still using `implicit` acknowledgment mode

```log
2026-03-26T13:21:45.971+01:00  INFO 64662 --- [           main] o.a.kafka.common.config.AbstractConfig   : ShareConsumerConfig values: 
	share.acknowledgement.mode = implicit
```

4. Observe the logs, 

The Spring Kafka lister our code throwing an exception to simulate a failure.

```log
2026-03-26T13:22:42.230+01:00 ERROR 64662 --- [Container#0-C-2] s.k.l.ShareKafkaMessageListenerContainer : Error processing record: ConsumerRecord(topic = high-throughput-topic, partition = 0, leaderEpoch = 0, offset = 0, CreateTime = 1774527762090, deliveryCount = 1, serialized key size = -1, serialized value size = 6, headers = RecordHeaders(headers = [], isReadOnly = false), key = null, value = kaboom)

org.springframework.kafka.listener.ListenerExecutionFailedException: Listener method 'public void com.example.spring.kafka.share.group.exception.consumer.implicit.ImplicitConcurrentShareListener.listen(org.apache.kafka.clients.consumer.ConsumerRecord<java.lang.String, java.lang.String>)' threw exception
	at org.springframework.kafka.listener.adapter.MessagingMessageListenerAdapter.invokeHandler(MessagingMessageListenerAdapter.java:513) ~[spring-kafka-4.0.4.jar:4.0.4]
	at org.springframework.kafka.listener.adapter.MessagingMessageListenerAdapter.invoke(MessagingMessageListenerAdapter.java:425) ~[spring-kafka-4.0.4.jar:4.0.4]
	at org.springframework.kafka.listener.adapter.ShareRecordMessagingMessageListenerAdapter.onShareRecord(ShareRecordMessagingMessageListenerAdapter.java:84) ~[spring-kafka-4.0.4.jar:4.0.4]
	at org.springframework.kafka.listener.ShareKafkaMessageListenerContainer$ShareListenerConsumer.processRecords(ShareKafkaMessageListenerContainer.java:415) ~[spring-kafka-4.0.4.jar:4.0.4]
	at org.springframework.kafka.listener.ShareKafkaMessageListenerContainer$ShareListenerConsumer.run(ShareKafkaMessageListenerContainer.java:374) ~[spring-kafka-4.0.4.jar:4.0.4]
	at java.base/java.util.concurrent.CompletableFuture$AsyncRun.run$$$capture(CompletableFuture.java:1825) ~[na:na]
	at java.base/java.util.concurrent.CompletableFuture$AsyncRun.run(CompletableFuture.java) ~[na:na]
	at java.base/java.lang.Thread.run(Thread.java:1474) ~[na:na]
Caused by: java.lang.RuntimeException: Simulated exception for message: kaboom
	at com.example.spring.kafka.share.group.exception.consumer.implicit.ImplicitConcurrentShareListener.listen(ImplicitConcurrentShareListener.java:22) ~[classes/:na]
	at java.base/jdk.internal.reflect.DirectMethodHandleAccessor.invoke(DirectMethodHandleAccessor.java:104) ~[na:na]
	at java.base/java.lang.reflect.Method.invoke(Method.java:565) ~[na:na]
	at org.springframework.messaging.handler.invocation.InvocableHandlerMethod.doInvoke(InvocableHandlerMethod.java:168) ~[spring-messaging-7.0.6.jar:7.0.6]
	at org.springframework.kafka.listener.adapter.KotlinAwareInvocableHandlerMethod.doInvoke(KotlinAwareInvocableHandlerMethod.java:48) ~[spring-kafka-4.0.4.jar:4.0.4]
	at org.springframework.messaging.handler.invocation.InvocableHandlerMethod.invoke(InvocableHandlerMethod.java:119) ~[spring-messaging-7.0.6.jar:7.0.6]
	at org.springframework.kafka.listener.adapter.HandlerAdapter.invoke(HandlerAdapter.java:80) ~[spring-kafka-4.0.4.jar:4.0.4]
	at org.springframework.kafka.listener.adapter.MessagingMessageListenerAdapter.invokeHandler(MessagingMessageListenerAdapter.java:488) ~[spring-kafka-4.0.4.jar:4.0.4]
	... 7 common frames omitted

2026-03-26T13:22:42.232+01:00 ERROR 64662 --- [Container#0-C-2] s.k.l.ShareKafkaMessageListenerContainer : Failed to reject record after processing error
```

We expect the exception to result in an implicit reject acknowledgement and mark the record as `ARCHIVED`.
But we run into an `IllegalStateException` in the Kafka library.

```log
java.lang.IllegalStateException: Implicit acknowledgement of delivery is being used.
	at org.apache.kafka.clients.consumer.internals.ShareConsumerImpl.ensureExplicitAcknowledgement(ShareConsumerImpl.java:1060) ~[kafka-clients-4.1.2.jar:na]
	at org.apache.kafka.clients.consumer.internals.ShareConsumerImpl.acknowledge(ShareConsumerImpl.java:688) ~[kafka-clients-4.1.2.jar:na]
	at org.apache.kafka.clients.consumer.KafkaShareConsumer.acknowledge(KafkaShareConsumer.java:507) ~[kafka-clients-4.1.2.jar:na]
	at org.springframework.kafka.listener.ShareKafkaMessageListenerContainer$ShareListenerConsumer.handleProcessingError(ShareKafkaMessageListenerContainer.java:450) ~[spring-kafka-4.0.4.jar:4.0.4]
	at org.springframework.kafka.listener.ShareKafkaMessageListenerContainer$ShareListenerConsumer.processRecords(ShareKafkaMessageListenerContainer.java:426) ~[spring-kafka-4.0.4.jar:4.0.4]
	at org.springframework.kafka.listener.ShareKafkaMessageListenerContainer$ShareListenerConsumer.run(ShareKafkaMessageListenerContainer.java:374) ~[spring-kafka-4.0.4.jar:4.0.4]
	at java.base/java.util.concurrent.CompletableFuture$AsyncRun.run$$$capture(CompletableFuture.java:1825) ~[na:na]
	at java.base/java.util.concurrent.CompletableFuture$AsyncRun.run(CompletableFuture.java) ~[na:na]
	at java.base/java.lang.Thread.run(Thread.java:1474) ~[na:na]
```

5. Verify the share group state from the broker

Message is not in state `ARCHIVED` but in state `ACKNOWLEGED`

```bash
docker exec -it kafka \
/opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --formatter-property print.key=true \
  --topic __share_group_state \
  --from-beginning \
  --formatter=org.apache.kafka.tools.consumer.group.share.ShareGroupStateMessageFormatter
```

```json
{
  "key": {
    "type": 0,
    "data": {
      "groupId": "my-implicit-share-group",
      "topicId": "jTZ-0peFQom7x8ZI092Kmw",
      "partition": 0
    }
  },
  "value": {
    "version": 0,
    "data": {
      "snapshotEpoch": 0,
      "stateEpoch": 1,
      "leaderEpoch": 0,
      "startOffset": -1,
      "createTimestamp": 1774527681612,
      "writeTimestamp": 1774527681612,
      "stateBatches": []
    }
  }
}{
  "key": {
    "type": 0,
    "data": {
      "groupId": "my-implicit-share-group",
      "topicId": "jTZ-0peFQom7x8ZI092Kmw",
      "partition": 0
    }
  },
  "value": {
    "version": 0,
    "data": {
      "snapshotEpoch": 1,
      "stateEpoch": 1,
      "leaderEpoch": 0,
      "startOffset": 0,
      "deliveryCompleteCount": 1,
      "createTimestamp": 1774527681612,
      "writeTimestamp": 1774528279127,
      "stateBatches": [
        {
          "firstOffset": 0,
          "lastOffset": 0,
          "deliveryState": 2,
          "deliveryCount": 1
        }
      ]
    }
  }
}
```
Relevant part: `"deliveryState": 2`

* deliveryState 0 = `Available`
* deliveryState 2 = `Acked`
* deliveryState 4 = `Archived`

## Current Limitations

* Autoconfiguration
* Observability (Metrics + Tracing)
* Exponential back-off
* No DLT support




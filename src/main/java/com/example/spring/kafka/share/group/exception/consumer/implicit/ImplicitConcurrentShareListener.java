package com.example.spring.kafka.share.group.exception.consumer.implicit;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;

public class ImplicitConcurrentShareListener {

  private static final Logger log = LoggerFactory.getLogger(ImplicitConcurrentShareListener.class);

  @KafkaListener(
      topics = "high-throughput-topic",
      containerFactory = "implicitShareKafkaListenerContainerFactory", // Implicit mode by default
      groupId = "my-implicit-share-group"
  )
  public void listen(ConsumerRecord<String, String> record) {
    // This listener will use 10 consumer threads
    log.info("Processing record from topic: {}, partition: {}, offset: {} {}", record.topic(), record.partition(), record.offset(), record.value());

    if (record.value().equals("kaboom")) {
      throw new RuntimeException("Simulated exception for message: " + record.value());
    }


    // Record is auto-acknowledged as ACCEPT on success, REJECT on error
  }
}
package com.example.spring.kafka.share.group.exception.consumer.explicit;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.ShareAcknowledgment;
import org.springframework.stereotype.Component;

public class ExplicitConcurrentShareListener {

  private static final Logger log = LoggerFactory.getLogger(ExplicitConcurrentShareListener.class);

  @KafkaListener(
      topics = "high-throughput-topic",
      containerFactory = "explicitShareKafkaListenerContainerFactory", // Implicit mode by default
      groupId = "my-explicit-share-group"
  )
  public void listen(ConsumerRecord<String, String> record, ShareAcknowledgment shareAcknowledgment) {
    // This listener will use 10 consumer threads
    log.info("Processing: {}", record.value());

    if (record.value().equals("kaboom")) {
      shareAcknowledgment.reject();
      return;
    }

    shareAcknowledgment.acknowledge();
  }
}
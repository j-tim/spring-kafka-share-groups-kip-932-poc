package com.example.spring.kafka.share.group.exception.producer;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class KafkaProducerController {

  private final KafkaTemplate<String, String> kafkaTemplate;

  public KafkaProducerController(KafkaTemplate<String, String> kafkaTemplate) {
    this.kafkaTemplate = kafkaTemplate;
  }

  /**
   * Produce a message to a Kafka topic.
   *
   * @param message the message content
   * @return success response
   */
  @PostMapping("/produce")
  public ResponseEntity<String> produceMessage(
      @RequestParam String message) {
    try {
      String topic = "high-throughput-topic";
      kafkaTemplate.send(topic, message);
      return ResponseEntity.ok("Message sent successfully to topic: " + topic);
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("Failed to send message: " + e.getMessage());
    }
  }

}


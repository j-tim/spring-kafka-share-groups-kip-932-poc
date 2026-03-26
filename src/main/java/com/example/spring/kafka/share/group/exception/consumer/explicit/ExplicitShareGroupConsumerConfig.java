package com.example.spring.kafka.share.group.exception.consumer.explicit;

import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.config.ShareKafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultShareConsumerFactory;
import org.springframework.kafka.core.ShareConsumerFactory;

@Configuration
@Profile("explicit")
public class ExplicitShareGroupConsumerConfig {

  @Bean
  public ShareConsumerFactory<String, String> explicitShareConsumerFactory() {
    Map<String, Object> props = new HashMap<>();
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    return new DefaultShareConsumerFactory<>(props);
  }

  @Bean
  public ShareKafkaListenerContainerFactory<String, String> explicitShareKafkaListenerContainerFactory(
      ShareConsumerFactory<String, String> explicitShareConsumerFactory) {

    ShareKafkaListenerContainerFactory<String, String> factory =
        new ShareKafkaListenerContainerFactory<>(explicitShareConsumerFactory);

    // Set default concurrency for all containers created by this factory
    factory.setConcurrency(3);

    // Configure acknowledgment mode at container factory level
    // true means explicit acknowledgment is required
    factory.getContainerProperties().setExplicitShareAcknowledgment(true);

    return factory;
  }

  @Bean
  public ExplicitConcurrentShareListener explicitConcurrentShareListener() {
    return new ExplicitConcurrentShareListener();
  }

}

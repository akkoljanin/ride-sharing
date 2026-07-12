package notificationService;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Bean
    public Queue notificationQueue() {
        return QueueBuilder.durable("ride.notification.queue").build();
    }

    @Bean
    public TopicExchange rideExchange() {
        return new TopicExchange("ride.events");
    }

    @Bean
    public Binding notificationBinding(Queue notificationQueue, TopicExchange rideExchange) {
        return BindingBuilder.bind(notificationQueue).to(rideExchange).with("ride.#");
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}

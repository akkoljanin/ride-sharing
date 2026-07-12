package rideService;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE    = "ride.events";
    public static final String QUEUE_NOTIF = "ride.notification.queue";
    public static final String QUEUE_DRIVER = "ride.driver.queue";

    @Bean
    public TopicExchange rideExchange() {
        return new TopicExchange(EXCHANGE);
    }

    @Bean
    public Queue notificationQueue() {
        return QueueBuilder.durable(QUEUE_NOTIF).build();
    }

    @Bean
    public Queue driverQueue() {
        return QueueBuilder.durable(QUEUE_DRIVER).build();
    }

    @Bean
    public Binding notificationBinding(Queue notificationQueue, TopicExchange rideExchange) {
        return BindingBuilder.bind(notificationQueue).to(rideExchange).with("ride.#");
    }

    @Bean
    public Binding driverBinding(Queue driverQueue, TopicExchange rideExchange) {
        return BindingBuilder.bind(driverQueue).to(rideExchange).with("ride.completed");
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    // Explicitly configure RabbitTemplate to use Jackson JSON — without this
    // Spring Boot uses Java serialization which causes ClassNotFoundException
    // on the notification-service side when deserializing RideEvent.
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}

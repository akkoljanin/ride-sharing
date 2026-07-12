package apiGateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RoutingConfiguration {

    @Bean
    public RouteLocator gatewayRouting(RouteLocatorBuilder builder) {
        return builder.routes()
                .route(p -> p.path("/users/**").uri("lb://users-service"))
                .route(p -> p.path("/drivers/**").uri("lb://driver-service"))
                .route(p -> p.path("/rides/**").uri("lb://ride-service"))
                .route(p -> p.path("/pricing/**").uri("lb://pricing-service"))
                .route(p -> p.path("/notifications/**").uri("lb://notification-service"))
                .build();
    }
}

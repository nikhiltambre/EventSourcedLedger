package com.github.nikhiltambre.api_gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {
    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("query-service-read", r -> r
                        .path("/api/accounts/**")
                        .uri("lb://query-service"))
                .route("command-service-write", r -> r
                        .path("/api/event/**")
                        .uri("lb://command-service"))

                .build();
    }
}

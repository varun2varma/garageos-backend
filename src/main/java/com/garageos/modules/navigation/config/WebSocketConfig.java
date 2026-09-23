package com.garageos.modules.navigation.config;

import com.garageos.modules.navigation.security.TripLocationTopicInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig
        implements WebSocketMessageBrokerConfigurer {

    private final TripLocationTopicInterceptor tripLocationTopicInterceptor;

    @Override
    public void configureMessageBroker(
            MessageBrokerRegistry registry) {

        registry.enableSimpleBroker("/topic");

        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(
            StompEndpointRegistry registry) {

        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*");

        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    /**
     * Root-cause fix: previously absent entirely, which meant no STOMP
     * SUBSCRIBE frame was ever authorized - see TripLocationTopicInterceptor.
     * Any authenticated session could subscribe to any trip's live-location
     * topic regardless of ownership.
     */
    @Override
    public void configureClientInboundChannel(
            ChannelRegistration registration) {

        registration.interceptors(tripLocationTopicInterceptor);
    }
}

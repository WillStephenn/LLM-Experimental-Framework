package com.locallab.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Configuration for WebSocket messaging with STOMP protocol.
 *
 * <p>This configuration enables WebSocket communication using the STOMP sub-protocol, which
 * provides a messaging layer over WebSocket connections. It configures the message broker for
 * publishing experiment progress updates to subscribed clients.
 *
 * <h3>Endpoints:</h3>
 *
 * <ul>
 *   <li>{@code /ws} - WebSocket connection endpoint. Clients connect here using SockJS.
 * </ul>
 *
 * <h3>Topic Destinations:</h3>
 *
 * <ul>
 *   <li>{@code /topic/experiments/{id}/progress} - Subscribe for experiment progress updates
 * </ul>
 *
 * <h3>Example Client Usage (JavaScript):</h3>
 *
 * <pre>{@code
 * const socket = new SockJS('/ws');
 * const stompClient = Stomp.over(socket);
 *
 * stompClient.connect({}, function(frame) {
 *     stompClient.subscribe('/topic/experiments/1/progress', function(message) {
 *         const data = JSON.parse(message.body);
 *         console.log('Progress:', data.type, data.payload);
 *     });
 * });
 * }</pre>
 *
 * @author William Stephen
 * @see com.locallab.dto.WebSocketMessage
 * @see com.locallab.service.ExperimentExecutorService
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * Configures the message broker for handling WebSocket messages.
     *
     * <p>Sets up a simple in-memory message broker with the {@code /topic} prefix for
     * broadcast-style messaging. Application destination prefix is set to {@code /app} for
     * client-to-server messages (though not used in current implementation).
     *
     * @param registry the message broker registry to configure
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    /**
     * Registers the STOMP endpoint for WebSocket connections.
     *
     * <p>Configures the {@code /ws} endpoint with SockJS fallback support for browsers that don't
     * support WebSocket natively. Allows connections from the configured origins (localhost:5173
     * for development).
     *
     * @param registry the STOMP endpoint registry to configure
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("http://localhost:5173", "http://127.0.0.1:5173")
                .withSockJS();
    }
}

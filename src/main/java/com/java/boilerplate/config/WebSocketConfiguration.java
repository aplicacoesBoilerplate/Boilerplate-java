package com.java.boilerplate.config;

import com.java.boilerplate.config.security.TokenService;
import com.java.boilerplate.repository.IUsersRepository;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfiguration implements WebSocketMessageBrokerConfigurer {
    private final TokensProperties tokensProperties;
    private final TokenService tokenService;
    private final IUsersRepository usersRepository;

    public WebSocketConfiguration(TokensProperties tokensProperties, TokenService tokenService, IUsersRepository usersRepository) {
        this.tokensProperties = tokensProperties;
        this.tokenService = tokenService;
        this.usersRepository = usersRepository;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic");
        config.setUserDestinationPrefix("/user");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
                    try {
                        String authorizationHeader = accessor.getFirstNativeHeader("Authorization");

                        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
                            String token = authorizationHeader.substring(7);
                            String login = tokenService.validateToken(token);
                            String contextKey = tokenService.getContextKey(token);

                            if (login != null && !login.isBlank() && contextKey != null && !contextKey.isBlank()) {
                                UserDetails userDetails = usersRepository.findByUsernameOrEmailAndContextKey(login, contextKey);
                                UsernamePasswordAuthenticationToken authentication =
                                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                                accessor.setUser(authentication);
                            }
                        }
                    } catch (Exception e) {
                        return null;
                    }
                }
                return message;
            }
        });
    }
}

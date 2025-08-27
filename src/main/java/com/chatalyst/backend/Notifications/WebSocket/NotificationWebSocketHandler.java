package com.chatalyst.backend.Notifications.WebSocket;

import com.chatalyst.backend.security.jwt.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationWebSocketHandler implements ChannelInterceptor {

    private final JwtUtils jwtUtils;
    private final UserDetailsService userDetailsService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = accessor.getFirstNativeHeader("Authorization");
            
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
                
                if (jwtUtils.validateJwtToken(token)) {
                    String username = jwtUtils.getUserNameFromJwtToken(token);
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                    
                    UsernamePasswordAuthenticationToken authentication = 
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    
                    accessor.setUser(authentication);
                    log.info("WebSocket connection authenticated for user: {}", username);
                }
            }
        }
        
        return message;
    }
}


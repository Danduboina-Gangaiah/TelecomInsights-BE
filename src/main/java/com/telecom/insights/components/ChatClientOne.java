package com.telecom.insights.components;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class ChatClientOne
{

    @Bean
    public ChatClient createChatClient(ChatClient.Builder builder)
    {
        return builder.build();
    }
}

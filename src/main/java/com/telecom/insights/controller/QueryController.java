package com.telecom.insights.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class QueryController
{
    @Autowired
    ChatClient chatClient;

    @GetMapping("/ask")
    public String askanyQuestiontoSwetha(@RequestParam String msg)
    {
        return chatClient.prompt().user(msg).call().content();

    }
}

package com.telecom.insights.controller;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
@RestController
@RequestMapping("/insights")
@Tag(name = "Insights API", description = "AI Query APIs")
public class QueryController {

    @Autowired
    ChatClient chatClient;

    @Operation(summary = "Ask AI Question")
    @GetMapping("/ask")
    public String askanyQuestiontoSwetha(@RequestParam String msg) {
        return chatClient.prompt().user(msg).call().content();
    }
}
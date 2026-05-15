package com.telecom.insights.handler;

import java.util.Map;

public interface GuardrailResponseHandler {
    Map<String, Object> handleOffTopic(String userQuestion);
}
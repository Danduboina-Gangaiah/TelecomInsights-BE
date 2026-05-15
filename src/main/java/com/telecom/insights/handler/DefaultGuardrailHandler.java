package com.telecom.insights.handler;

import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class DefaultGuardrailHandler
        implements GuardrailResponseHandler {

    @Override
    public Map<String, Object> handleOffTopic(
            String userQuestion
    ) {

        return Map.of(

                "status", "REJECTED",

                "error", "OUT_OF_DOMAIN",

                "source", "GUARDRAIL",

                "message",
                "The requested query is outside the supported telecom analytics domain. " +
                        "Please submit queries related to network latency, packet loss, bandwidth, " +
                        "KPI metrics, anomalies, or telecom performance analysis."
        );
    }
}
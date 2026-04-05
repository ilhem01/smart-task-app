package com.smarttask.task.service;

import com.smarttask.task.entity.Task;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AiService {

    private static final String WEBHOOK_URL = "http://n8n:5678/webhook/task-ai";
    private static final String RECOMMENDATION_WEBHOOK_URL = "http://n8n:5678/webhook/task-recommendation";
    private static final String FALLBACK_SUMMARY = "No summary available";
    private static final String FALLBACK_PRIORITY = "MEDIUM";
    private static final String FALLBACK_DEADLINE = "";

    private final RestTemplate restTemplate;
      

    public AiService() {
        this.restTemplate = new RestTemplate();
    }

  

    public Map<String, Object> analyzeTask(String title) {
        TaskAiWebhookRequest payload = new TaskAiWebhookRequest(title);
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.CONTENT_TYPE, "application/json");
        HttpEntity<TaskAiWebhookRequest> requestEntity = new HttpEntity<>(payload, headers);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.exchange(
                    WEBHOOK_URL,
                    HttpMethod.POST,
                    requestEntity,
                    Map.class
            ).getBody();

            if (response == null) {
                System.out.println("AI RESPONSE: " + response);
                return fallbackResponse();
            }

            Map<String, Object> result = new HashMap<>();
            result.put("summary", defaultIfBlank(valueAsString(response.get("summary")), FALLBACK_SUMMARY));
            result.put("priority", defaultIfBlank(valueAsString(response.get("priority")), FALLBACK_PRIORITY));
            result.put("deadline", defaultIfBlank(valueAsString(response.get("deadline")), FALLBACK_DEADLINE));
            return result;
        } catch (Exception ex) {
            return fallbackResponse();
        }
    }

    public Map<String, Object> analyzeTitle(String title) {
        return analyzeTask(title);
    }

    public Map<String, Object> recommendTasks(List<Task> tasks) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.CONTENT_TYPE, "application/json");
        HttpEntity<List<Task>> requestEntity = new HttpEntity<>(tasks, headers);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.exchange(
                    RECOMMENDATION_WEBHOOK_URL,
                    HttpMethod.POST,
                    requestEntity,
                    Map.class
            ).getBody();

            return response == null ? fallbackRecommendationResponse() : response;
        } catch (Exception ex) {
            return fallbackRecommendationResponse();
        }
    }

    private Map<String, Object> fallbackResponse() {
        Map<String, Object> result = new HashMap<>();
        result.put("summary", FALLBACK_SUMMARY);
        result.put("priority", FALLBACK_PRIORITY);
        result.put("deadline", FALLBACK_DEADLINE);
        return result;
    }

    private Map<String, Object> fallbackRecommendationResponse() {
        Map<String, Object> result = new HashMap<>();
        result.put("recommendations", List.of());
        result.put("message", "No recommendations available");
        return result;
    }

    private String valueAsString(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private record TaskAiWebhookRequest(String title) {
    }

}

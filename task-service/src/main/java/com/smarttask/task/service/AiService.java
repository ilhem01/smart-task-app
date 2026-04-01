package com.smarttask.task.service;

import com.smarttask.task.entity.Task;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class AiService {

    private static final Logger log = LoggerFactory.getLogger(AiService.class);
    private static final String WEBHOOK_URL = "http://localhost:5678/webhook/task-ai";
    private static final String RECOMMENDATION_WEBHOOK_URL = "http://localhost:5678/webhook/task-recommendation";
    private static final String FALLBACK_SUMMARY = "No summary available";
    private static final String FALLBACK_PRIORITY = "MEDIUM";
    private static final String FALLBACK_DEADLINE = "";
    private static final int FALLBACK_EFFORT = 3;
    private static final String STRESS_LOW = "low";
    private static final String STRESS_MEDIUM = "medium";
    private static final String STRESS_HIGH = "high";

    /** Single match → high stress (urgency / severity). */
    private static final Set<String> STRESS_KEYWORDS_HIGH = Set.of(
            "urgent", "critical", "overwhelming", "impossible", "panic", "asap"
    );
    /** Complexity / load — often combined (e.g. hard + complex). */
    private static final Set<String> STRESS_KEYWORDS_GENERAL = Set.of(
            "hard", "complex", "long", "difficult", "stressful", "intensive", "heavy",
            "challenging", "demanding", "tough", "overdue", "pressured", "complicated", "lengthy"
    );

    private final RestTemplate restTemplate;

    public AiService() {
        this.restTemplate = new RestTemplate();
    }

    public Map<String, Object> analyzeTask(String title) {
        System.out.println("Calling AI...");
        log.info("Calling AI analyze webhook for title='{}'", title);
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
                log.warn("AI analyze webhook returned null body, using fallback values");
                return fallbackResponse(title);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("summary", defaultIfBlank(valueAsString(response.get("summary")), FALLBACK_SUMMARY));
            result.put("priority", defaultIfBlank(valueAsString(response.get("priority")), FALLBACK_PRIORITY));
            result.put("deadline", defaultIfBlank(valueAsString(response.get("deadline")), FALLBACK_DEADLINE));
            result.put("effort", response.getOrDefault("effort", FALLBACK_EFFORT));
            String keywordStress = detectKeywordStressLevel(title);
            String webhookStress = normalizeStressLevel(response.get("stressLevel"));
            result.put("stressLevel", mergeStressLevels(keywordStress, webhookStress));
            log.info("AI analyze webhook success: summaryPresent={}, priority='{}', deadline='{}', effort='{}', stressLevel='{}' (keywords={}, webhook={})",
                    !valueAsString(result.get("summary")).isBlank(),
                    valueAsString(result.get("priority")),
                    valueAsString(result.get("deadline")),
                    valueAsString(result.get("effort")),
                    valueAsString(result.get("stressLevel")),
                    keywordStress,
                    webhookStress);
            return result;
        } catch (Exception ex) {
            log.error("AI analyze webhook failed, using fallback values", ex);
            return fallbackResponse(title);
        }
    }

    public Map<String, Object> analyzeTitle(String title) {
        return analyzeTask(title);
    }

    public Map<String, Object> recommendTasks(List<Task> tasks) {
        log.info("Calling AI recommendation webhook for {} tasks", tasks == null ? 0 : tasks.size());
        List<Map<String, Object>> taskPayload = tasks == null
                ? List.of()
                : tasks.stream()
                .map(task -> {
                    Map<String, Object> mapped = new LinkedHashMap<>();
                    mapped.put("id", task.getId());
                    mapped.put("title", task.getTitle());
                    mapped.put("priority", task.getPriority());
                    mapped.put("deadline", task.getDeadline());
                    mapped.put("dueDate", task.getDueDate());
                    mapped.put("effort", task.getEffort());
                    return mapped;
                })
                .toList();
        Map<String, Object> requestPayload = new LinkedHashMap<>();
        requestPayload.put("tasks", taskPayload);
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.CONTENT_TYPE, "application/json");
        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestPayload, headers);
        System.out.println("Sending recommendation payload: " + requestPayload);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.exchange(
                    RECOMMENDATION_WEBHOOK_URL,
                    HttpMethod.POST,
                    requestEntity,
                    Map.class
            ).getBody();
            System.out.println("Recommendation response body: " + response);

            if (response == null) {
                log.warn("AI recommendation webhook returned null body, using fallback recommendation");
                return fallbackRecommendationResponse();
            }
            log.info("AI recommendation webhook success");
            return response;
        } catch (Exception ex) {
            log.error("AI recommendation webhook failed, using fallback recommendation", ex);
            return fallbackRecommendationResponse();
        }
    }

    private Map<String, Object> fallbackResponse(String title) {
        Map<String, Object> result = new HashMap<>();
        result.put("summary", FALLBACK_SUMMARY);
        result.put("priority", FALLBACK_PRIORITY);
        result.put("deadline", FALLBACK_DEADLINE);
        result.put("effort", FALLBACK_EFFORT);
        result.put("stressLevel", detectKeywordStressLevel(title));
        return result;
    }

    /**
     * Stress from title keywords (word-boundary matches). High-tier words → high;
     * general words (e.g. hard, complex, long): 0 low, 1 medium, 2+ high.
     */
    String detectKeywordStressLevel(String title) {
        if (title == null || title.isBlank()) {
            return STRESS_LOW;
        }
        String lower = title.toLowerCase(Locale.ROOT);
        if (countWordMatches(lower, STRESS_KEYWORDS_HIGH) > 0) {
            return STRESS_HIGH;
        }
        int general = countWordMatches(lower, STRESS_KEYWORDS_GENERAL);
        if (general >= 2) {
            return STRESS_HIGH;
        }
        if (general == 1) {
            return STRESS_MEDIUM;
        }
        return STRESS_LOW;
    }

    private int countWordMatches(String lowerTitle, Set<String> words) {
        int n = 0;
        for (String word : words) {
            Pattern p = Pattern.compile("\\b" + Pattern.quote(word) + "\\b");
            if (p.matcher(lowerTitle).find()) {
                n++;
            }
        }
        return n;
    }

    private String normalizeStressLevel(Object value) {
        if (value == null) {
            return STRESS_LOW;
        }
        String s = String.valueOf(value).trim().toLowerCase(Locale.ROOT);
        if (s.isEmpty()) {
            return STRESS_LOW;
        }
        if (s.contains("high") || s.equals("3") || s.equals("severe")) {
            return STRESS_HIGH;
        }
        if (s.contains("medium") || s.equals("2") || s.equals("moderate")) {
            return STRESS_MEDIUM;
        }
        if (s.contains("low") || s.equals("1") || s.equals("mild")) {
            return STRESS_LOW;
        }
        return STRESS_LOW;
    }

    private String mergeStressLevels(String a, String b) {
        return stressRank(a) >= stressRank(b) ? a : b;
    }

    private int stressRank(String level) {
        if (STRESS_HIGH.equals(level)) {
            return 2;
        }
        if (STRESS_MEDIUM.equals(level)) {
            return 1;
        }
        return 0;
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

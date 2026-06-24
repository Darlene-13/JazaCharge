package io.github.darlene.jazacharge.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.darlene.jazacharge.dto.request.ParsedRiderIntent;
import io.github.darlene.jazacharge.entity.BatteryType;
import io.github.darlene.jazacharge.exception.LLMParsingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class LLMService {

    @Value("${groq.api-key}")
    private String groqApiKey;

    @Value("${groq.api-url}")
    private String groqApiUrl;

    @Value("${groq.model}")
    private String groqModel;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public ParsedRiderIntent parseRiderMessage(String rawSms) {
        try {
            Map<String, Object> requestBody = Map.of(
                "model", groqModel,
                "temperature", 0.1,
                "messages", List.of(
                    Map.of("role", "system", "content", buildSystemPrompt()),
                    Map.of("role", "user", "content", rawSms)
                )
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(groqApiKey);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(groqApiUrl, request, Map.class);

            List<Map> choices = (List<Map>) response.getBody().get("choices");
            Map message = (Map) choices.get(0).get("message");
            String content = (String) message.get("content");

            log.info("LLM raw response: {}", content);

            Map<String, String> parsed = objectMapper.readValue(content, Map.class);

            ParsedRiderIntent intent = new ParsedRiderIntent();
            intent.setLocation(parsed.get("location"));
            intent.setBatteryType(BatteryType.valueOf(parsed.get("batteryType")));
            intent.setRawMessage(rawSms);

            return intent;

        } catch (Exception e) {
            log.error("LLM parsing failed for: {}", rawSms, e);
            throw new LLMParsingException("Could not understand rider message: " + e.getMessage());
        }
    }

    private String buildSystemPrompt() {
        return """
                You are an EV battery swap assistant for Nairobi, Kenya.
                Extract the rider's location and battery type from their SMS.
                The rider may write in English, Swahili, or Sheng (Kenyan slang).
                
                Battery types: LITHIUM_48V, LITHIUM_60V, LITHIUM_72V
                If battery type not mentioned, default to LITHIUM_48V.
                If location unclear, use "CBD".
                
                Common Nairobi locations: CBD, Westlands, Ngong Road,
                Kasarani, Eastleigh, Kayole, Githurai, Thika Road.
                
                Return ONLY valid JSON. No explanation. No markdown.
                Example: {"location": "CBD", "batteryType": "LITHIUM_72V"}
                """;
    }
}

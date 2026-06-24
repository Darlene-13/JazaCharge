package io.github.darlene.jazacharge.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.List;

@Service
@Slf4j
public class SmsService {

    @Value("${at.api-key}")
    private String apiKey;

    @Value("${at.username}")
    private String username;

    private static final String AT_SMS_URL = "https://api.sandbox.africastalking.com/version1/messaging";
    private static final String SENDER_ID = "JAZA";

    private final RestTemplate restTemplate;

    public SmsService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    // Send SMS to a single rider
    public void sendSms(String phoneNumber, String message) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            headers.set("apiKey", apiKey);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("username", username);
            body.add("to", phoneNumber);
            body.add("message", message);
            body.add("from", SENDER_ID);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(AT_SMS_URL, request, String.class);
            log.info("SMS sent to {} | Status: {} | Response: {}", phoneNumber, response.getStatusCode(), response.getBody());

        } catch (Exception e) {
            log.error("Failed to send SMS to {}: {}", phoneNumber, e.getMessage());
        }
    }

    // Send bulk SMS to multiple riders e.g. rerouting alerts
    public void sendBulkSms(List<String> phoneNumbers, String message) {
        String recipients = String.join(",", phoneNumbers);
        sendSms(recipients, message);
        log.info("Bulk SMS sent to {} riders", phoneNumbers.size());
    }

    // Pre-built message templates
    public String buildReservationConfirmation(String code, String stationName, String location, String expiresAt) {
        return String.format(
            "JAZA: Battery reserved! Code: %s | Station: %s, %s | Valid until: %s | Show code on arrival.",
            code, stationName, location, expiresAt
        );
    }

    public String buildRerouteAlert(String stationName, String alternativeName, int discount) {
        return String.format(
            "JAZA ALERT: %s is running low. Swap at %s and get %d%% discount. Reply SWAP to confirm.",
            stationName, alternativeName, discount
        );
    }

    public String buildNoStationMessage(String location) {
        return String.format(
            "JAZA: Sorry, no batteries available near %s right now. Try: CBD, Westlands, or Kasarani. Reply with new location.",
            location
        );
    }
}

package io.github.darlene.jazacharge.controller;

import io.github.darlene.jazacharge.dto.request.ParsedRiderIntent;
import io.github.darlene.jazacharge.dto.request.UssdRequest;
import io.github.darlene.jazacharge.dto.response.ReservationResponse;
import io.github.darlene.jazacharge.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/webhook")
@Slf4j
@RequiredArgsConstructor
public class WebhookController {

    private final LLMService llmService;
    private final AgentService agentService;
    private final SmsService smsService;
    private final UssdSessionService ussdSessionService;

    // AT posts here when a rider sends an SMS
    @PostMapping("/sms")
    public ResponseEntity<String> receiveSms(
        @RequestParam("from") String from,
        @RequestParam("text") String text,
        @RequestParam(value = "to", required = false) String to
    ) {
        log.info("Inbound SMS from={} text={}", from, text);

        try {
            // Parse intent via LLM
            ParsedRiderIntent intent = llmService.parseRiderMessage(text);

            // Agent decides and reserves
            ReservationResponse response = agentService.processRiderRequest(from, intent);

            // Send SMS confirmation back to rider
            smsService.sendSms(from, response.getSmsMessage());

            return ResponseEntity.ok("OK");

        } catch (Exception e) {
            log.error("SMS processing failed: {}", e.getMessage());
            // Send friendly error back to rider
            smsService.sendSms(from,
                "JAZACHARGE: Sorry, we could not process your request. " +
                "Try: 'CBD 72V' or dial *384*100# to use our menu."
            );
            return ResponseEntity.ok("OK"); // always return 200 to AT
        }
    }

    // AT posts here when a rider dials the USSD code
    @PostMapping("/ussd")
    public ResponseEntity<String> receiveUssd(@ModelAttribute UssdRequest request) {
        log.info("USSD session={} phone={} text={}", 
            request.getSessionId(), request.getPhoneNumber(), request.getText());

        String response = ussdSessionService.handleUssd(request);
        // AT expects plain text response starting with CON (continue) or END (terminate)
        return ResponseEntity.ok(response);
    }
}

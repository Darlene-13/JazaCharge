package io.github.darlene.jazacharge.service;

import io.github.darlene.jazacharge.dto.request.ParsedRiderIntent;
import io.github.darlene.jazacharge.dto.response.ReservationResponse;
import io.github.darlene.jazacharge.dto.request.UssdRequest;
import io.github.darlene.jazacharge.entity.BatteryType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class UssdSessionService {

    private final AgentService agentService;

    // AT USSD text accumulates input like "1*2*CBD"
    // Each * separates a menu level response
    public String handleUssd(UssdRequest request) {
        String sessionId = request.getSessionId();
        String phoneNumber = request.getPhoneNumber();
        String text = request.getText(); // e.g. "" | "1" | "1*1" | "1*1*CBD"

        log.info("USSD session={}, phone={}, text={}", sessionId, phoneNumber, text);

        // Level 0 — first dial, show main menu
        if (text == null || text.isEmpty()) {
            return "CON Welcome to JazaCharge EV Swap\n" +
                   "1. Find nearest battery\n" +
                   "2. Check my reservation\n" +
                   "3. Cancel reservation";
        }

        String[] parts = text.split("\\*");

        // Level 1 — user picked from main menu
        if (parts.length == 1) {
            return switch (parts[0]) {
                case "1" -> "CON Select battery type:\n" +
                            "1. 48V (Light boda)\n" +
                            "2. 60V (Standard boda)\n" +
                            "3. 72V (Heavy/matatu)";
                case "2" -> "CON Enter your reservation code:";
                case "3" -> "CON Enter reservation code to cancel:";
                default  -> "END Invalid option. Please try again.";
            };
        }

        // Level 2 — battery type selected, ask for location
        if (parts.length == 2 && parts[0].equals("1")) {
            return "CON Enter your location:\n(e.g. CBD, Westlands, Kasarani)";
        }

        // Level 3 — location entered, process request
        if (parts.length == 3 && parts[0].equals("1")) {
            BatteryType batteryType = switch (parts[1]) {
                case "1" -> BatteryType.LITHIUM_48V;
                case "2" -> BatteryType.LITHIUM_60V;
                case "3" -> BatteryType.LITHIUM_72V;
                default  -> BatteryType.LITHIUM_48V;
            };

            String location = parts[2];

            try {
                ParsedRiderIntent intent = new ParsedRiderIntent();
                intent.setLocation(location);
                intent.setBatteryType(batteryType);
                intent.setRawMessage("USSD request");

                ReservationResponse response = agentService.processRiderRequest(phoneNumber, intent);

                return "END Battery reserved!\n" +
                       "Code: " + response.getReservationCode() + "\n" +
                       "Station: " + response.getStationName() + "\n" +
                       "Valid until: " + response.getExpiresAt() + "\n" +
                       "SMS confirmation sent.";

            } catch (Exception e) {
                return "END Sorry, no batteries available near " + location +
                       ". Try CBD, Westlands or Kasarani.";
            }
        }

        return "END Session ended. Dial again to swap.";
    }
}

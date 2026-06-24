package io.github.darlene.jazacharge.dto.request;

import io.github.darlene.jazacharge.entity.BatteryType;
import lombok.Data;

// What the LLM extracts from the raw SMS
@Data
public class ParsedRiderIntent {
    private String location;         // e.g. "CBD"
    private BatteryType batteryType; // e.g. LITHIUM_72V
    private String rawMessage;       // original SMS for logging
}
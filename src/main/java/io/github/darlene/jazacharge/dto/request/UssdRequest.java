package io.github.darlene.jazacharge.dto.request;

import lombok.Data;

// Maps exactly to Africa's Talking USSD webhook payload
@Data
public class UssdRequest {
    private String sessionId;
    private String serviceCode;
    private String phoneNumber;
    private String text;    // accumulates input e.g. "1*CBD*2"
}
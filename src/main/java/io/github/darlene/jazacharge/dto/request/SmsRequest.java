package io.github.darlene.jazacharge.dto.request;

import lombok.Data;

// Maps exactly to Africa's Talking SMS webhook payload
@Data
public class SmsRequest {
    private String from;    // rider phone number
    private String to;      // your shortcode
    private String text;    // raw SMS e.g. "nina battery low CBD nataka 72V"
    private String date;
    private String id;
    private String linkId;
}
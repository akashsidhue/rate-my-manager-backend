package com.ratemymanager.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "otp")
@Data
public class OtpConfig {
    private int expiryMinutes = 5;
    private int maxRequests = 3;  // Maximum OTP requests allowed within the time window
    private int maxVerificationAttempts = 5;
}

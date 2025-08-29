package com.ratemymanager.service;

import com.ratemymanager.repository.OtpRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class DataMigrationService {
    
    private final OtpRepository otpRepository;
    
    @Autowired
    public DataMigrationService(OtpRepository otpRepository) {
        this.otpRepository = otpRepository;
    }
    
    /**
     * Migrate existing OTP data to include new rate limiting fields
     * This runs once when the application starts
     */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void migrateExistingOtpData() {
        try {
            log.info("Starting OTP data migration...");
            
            // Find all OTPs that don't have the new fields populated
            List<com.ratemymanager.model.Otp> otpsToUpdate = otpRepository.findAll();
            int updatedCount = 0;
            
            for (com.ratemymanager.model.Otp otp : otpsToUpdate) {
                boolean needsUpdate = false;
                
                // Set expires_at if null
                if (otp.getExpiresAt() == null) {
                    otp.setExpiresAt(otp.getExpiryTime());
                    needsUpdate = true;
                }
                
                // Set resend_count if null
                if (otp.getResendCount() == null) {
                    otp.setResendCount(0);
                    needsUpdate = true;
                }
                
                // Set verification_attempts if null
                if (otp.getVerificationAttempts() == null) {
                    otp.setVerificationAttempts(0);
                    needsUpdate = true;
                }
                
                if (needsUpdate) {
                    otpRepository.save(otp);
                    updatedCount++;
                }
            }
            
            log.info("OTP data migration completed. Updated {} records.", updatedCount);
            
        } catch (Exception e) {
            log.error("Error during OTP data migration: {}", e.getMessage(), e);
        }
    }
}

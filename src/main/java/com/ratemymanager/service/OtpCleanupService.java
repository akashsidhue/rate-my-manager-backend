package com.ratemymanager.service;

import com.ratemymanager.repository.OtpRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Slf4j
public class OtpCleanupService {
    
    private final OtpRepository otpRepository;
    
    @Autowired
    public OtpCleanupService(OtpRepository otpRepository) {
        this.otpRepository = otpRepository;
    }
    
    /**
     * Clean up expired OTPs every 10 minutes
     * This prevents the database from accumulating expired OTP records
     */
    @Scheduled(fixedRate = 600000) // 10 minutes in milliseconds
    @Transactional
    public void cleanupExpiredOtps() {
        try {
            LocalDateTime now = LocalDateTime.now();
            log.info("Starting cleanup of expired OTPs at: {}", now);
            
            // Delete OTPs that have expired
            int deletedCount = otpRepository.deleteExpiredOtps(now);
            
            log.info("Completed cleanup of expired OTPs. Deleted {} records.", deletedCount);
        } catch (Exception e) {
            log.error("Error during OTP cleanup: {}", e.getMessage(), e);
        }
    }
}

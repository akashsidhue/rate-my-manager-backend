package com.ratemymanager.service;

import com.ratemymanager.config.OtpConfig;
import com.ratemymanager.model.Otp;
import com.ratemymanager.model.User;
import com.ratemymanager.repository.OtpRepository;
import com.ratemymanager.repository.UserRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.security.Key;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Optional;
import java.util.Random;

@Service
@Slf4j
public class AuthService {
	private final OtpRepository otpRepository;
	private final UserRepository userRepository;
	private final JavaMailSender mailSender;
	private final OtpConfig otpConfig;

	// NOTE: For MVP, keeping a hardcoded secret. Move to env var/Secret Manager in production.
	private static final String JWT_SECRET_BASE64 = "bXktdmVyeS1sb25nLXN1cGVyLXNlY3JldC1rZXktZm9yLWp3dC10ZXN0aW5nLW9ubHk=";
	private static final long JWT_EXPIRATION_MS = 1000L * 60 * 60 * 24; // 24 hours

	public AuthService(OtpRepository otpRepository, UserRepository userRepository, JavaMailSender mailSender, OtpConfig otpConfig) {
		this.otpRepository = otpRepository;
		this.userRepository = userRepository;
		this.mailSender = mailSender;
		this.otpConfig = otpConfig;
	}

	public void requestOtp(String email) {
		log.info("Processing OTP request for email: {}", email);
		validateCompanyEmail(email);
		
		// Check if there's an existing valid OTP within the window
		Optional<Otp> existingOtp = otpRepository.findTopByEmailOrderByCreatedAtDesc(email.toLowerCase());
		if (existingOtp.isPresent()) {
			Otp otp = existingOtp.get();
			LocalDateTime windowStart = LocalDateTime.now().minusMinutes(otpConfig.getExpiryMinutes());
			
			log.debug("Found existing OTP for email: {}, created at: {}, window start: {}", 
				email, otp.getCreatedAt(), windowStart);
			
			// If existing OTP is within the 5-minute window
			if (otp.getCreatedAt().isAfter(windowStart)) {
				// Check if we've reached the maximum requests limit
				// resend_count tracks total requests: 0=1st request, 1=2nd request, 2=3rd request
				Integer currentResendCount = otp.getResendCount() != null ? otp.getResendCount() : 0;
				int totalRequests = currentResendCount + 1;
				
				log.debug("Existing OTP within window. Total requests so far: {}, max allowed: {}", 
					totalRequests, otpConfig.getMaxRequests());
				
				if (totalRequests >= otpConfig.getMaxRequests()) {
					log.warn("Rate limit exceeded for email: {}, total requests: {}, max allowed: {}", 
						email, totalRequests, otpConfig.getMaxRequests());
					throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, 
						"Maximum " + otpConfig.getMaxRequests() + " OTP requests allowed within " + otpConfig.getExpiryMinutes() + " minutes. Please wait before requesting another OTP.");
				}
				
				// Increment resend count and regenerate OTP
				otp.setResendCount(currentResendCount + 1);
				otp.setOtpCode(generateSixDigitOtp());
				// Keep the same expiresAt (don't reset the 5-minute window)
				if (otp.getExpiresAt() == null) {
					otp.setExpiresAt(otp.getExpiryTime());
				}
				otpRepository.save(otp);
				
				int newTotalRequests = otp.getResendCount() + 1;
				log.info("OTP resent for email: {}, total requests: {}/{}, expires at: {}", 
					email, newTotalRequests, otpConfig.getMaxRequests(), otp.getExpiresAt());
				
				sendOtpEmail(email, otp.getOtpCode());
				return;
			} else {
				log.debug("Existing OTP is outside the 5-minute window, will create new OTP");
			}
		} else {
			log.debug("No existing OTP found for email: {}", email);
		}
		
		// Check rate limiting for new OTP requests - sliding 5-minute window
		// This is a safety check in case there are multiple OTP records
		LocalDateTime fiveMinutesAgo = LocalDateTime.now().minusMinutes(otpConfig.getExpiryMinutes());
		Long recentOtpCount = otpRepository.countByEmailAndCreatedAtAfter(email.toLowerCase(), fiveMinutesAgo);
		
		log.debug("Rate limiting check: {} OTPs found in last {} minutes for email: {}", 
			recentOtpCount, otpConfig.getExpiryMinutes(), email);
		
		if (recentOtpCount >= otpConfig.getMaxRequests()) {
			log.warn("Rate limit exceeded for email: {}. {} OTPs requested in last {} minutes", 
				email, recentOtpCount, otpConfig.getExpiryMinutes());
			throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, 
				"Too many OTP requests. Please wait before requesting another OTP.");
		}
		
		// Create new OTP
		String otp = generateSixDigitOtp();
		LocalDateTime now = LocalDateTime.now();
		LocalDateTime expiresAt = now.plusMinutes(otpConfig.getExpiryMinutes());

		Otp otpEntity = new Otp();
		otpEntity.setEmail(email.toLowerCase());
		otpEntity.setOtpCode(otp);
		otpEntity.setExpiryTime(expiresAt);
		otpEntity.setExpiresAt(expiresAt);
		otpEntity.setResendCount(0);  // This represents the 1st request
		otpEntity.setVerificationAttempts(0);
		otpRepository.save(otpEntity);
		log.info("New OTP created for email: {}, total requests: 1/{}, expires at: {}", 
			email, otpConfig.getMaxRequests(), expiresAt);

		sendOtpEmail(email, otp);
		log.info("OTP email sent successfully to: {}", email);
	}

	public String verifyOtpAndGenerateToken(String email, String otp) {
		log.info("Verifying OTP for email: {}", email);
		validateCompanyEmail(email);
		
		Optional<Otp> match = otpRepository.findByEmailAndOtpCode(email.toLowerCase(), otp);
		if (match.isEmpty()) {
			log.warn("Invalid OTP provided for email: {}", email);
			// Increment verification attempts for the latest OTP
			incrementVerificationAttempts(email.toLowerCase());
			throw new IllegalArgumentException("Invalid OTP");
		}
		
		Otp otpEntity = match.get();
		
		// Check if OTP is expired - use expiresAt if available, otherwise fallback to expiryTime
		LocalDateTime expiryTime = otpEntity.getExpiresAt() != null ? otpEntity.getExpiresAt() : otpEntity.getExpiryTime();
		if (expiryTime.isBefore(LocalDateTime.now())) {
			log.warn("Expired OTP used for email: {}, expires at: {}", email, expiryTime);
			throw new IllegalArgumentException("OTP expired");
		}
		
		// Check if verification attempts exceeded
		Integer attempts = otpEntity.getVerificationAttempts() != null ? otpEntity.getVerificationAttempts() : 0;
		if (attempts >= otpConfig.getMaxVerificationAttempts()) {
			log.warn("Too many verification attempts for email: {}, attempts: {}", 
				email, attempts);
			throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, 
				"Too many failed verification attempts. Please request a new OTP.");
		}

		log.info("OTP verified successfully for email: {}", email);
		User user = userRepository.findByEmail(email.toLowerCase())
				.orElseGet(() -> {
					log.info("Creating new user for email: {}", email);
					User u = new User();
					u.setEmail(email.toLowerCase());
					return u;
				});
		user.setVerified(true);
		userRepository.save(user);
		log.info("User verified and saved for email: {}", email);

		// Clear existing OTPs for this email
		otpRepository.deleteByEmail(email.toLowerCase());
		log.info("Cleared existing OTPs for email: {}", email);

		String token = generateJwtToken(email.toLowerCase());
		log.info("JWT token generated successfully for email: {}", email);
		return token;
	}
	
	public void resendOtp(String email) {
		log.info("Processing OTP resend request for email: {}", email);
		validateCompanyEmail(email);
		
		// Check if there's an existing OTP
		Optional<Otp> existingOtp = otpRepository.findTopByEmailOrderByCreatedAtDesc(email.toLowerCase());
		if (existingOtp.isEmpty()) {
			log.warn("No existing OTP found for resend request: {}", email);
			throw new IllegalArgumentException("No OTP found. Please request a new OTP first.");
		}
		
		Otp otp = existingOtp.get();
		LocalDateTime now = LocalDateTime.now();
		
		// Check if OTP is still within the 5-minute window
		LocalDateTime windowStart = now.minusMinutes(otpConfig.getExpiryMinutes());
		if (otp.getCreatedAt().isBefore(windowStart)) {
			log.info("Existing OTP expired, creating new OTP for email: {}", email);
			// Create new OTP since the window has expired
			requestOtp(email);
			return;
		}
		
		// Check if resend limit exceeded
		Integer currentResendCount = otp.getResendCount() != null ? otp.getResendCount() : 0;
		int totalRequests = currentResendCount + 1;
		
		if (totalRequests >= otpConfig.getMaxRequests()) {
			log.warn("Resend limit exceeded for email: {}, total requests: {}, max allowed: {}", 
				email, totalRequests, otpConfig.getMaxRequests());
			throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, 
				"Maximum " + otpConfig.getMaxRequests() + " OTP requests allowed within " + otpConfig.getExpiryMinutes() + " minutes. Please wait before requesting another OTP.");
		}
		
		// Increment resend count and regenerate OTP
		otp.setResendCount(currentResendCount + 1);
		otp.setOtpCode(generateSixDigitOtp());
		// Keep the same expiresAt (don't reset the 5-minute window)
		otpRepository.save(otp);
		log.info("OTP resend #{} for email: {}, expires at: {}", 
			otp.getResendCount(), email, otp.getExpiresAt());
		
		sendOtpEmail(email, otp.getOtpCode());
		log.info("OTP resend email sent successfully to: {}", email);
	}
	
	private void incrementVerificationAttempts(String email) {
		Optional<Otp> latestOtp = otpRepository.findTopByEmailOrderByCreatedAtDesc(email);
		if (latestOtp.isPresent()) {
			Otp otp = latestOtp.get();
			Integer currentAttempts = otp.getVerificationAttempts() != null ? otp.getVerificationAttempts() : 0;
			otp.setVerificationAttempts(currentAttempts + 1);
			otpRepository.save(otp);
			log.debug("Incremented verification attempts for email: {}, new count: {}", 
				email, otp.getVerificationAttempts());
		}
	}

	private void validateCompanyEmail(String email) {
		if (email == null || email.trim().isEmpty()) {
			log.warn("Email validation failed: email is null or empty");
			throw new IllegalArgumentException("Email is required");
		}
		// For testing purposes, allow any valid email format
		if (!email.contains("@")) {
			log.warn("Email validation failed: invalid email format for: {}", email);
			throw new IllegalArgumentException("Invalid email format");
		}
		log.debug("Email validation passed for: {}", email);
	}

	private String generateSixDigitOtp() {
		Random random = new Random();
		int number = 100000 + random.nextInt(900000);
		String otp = String.valueOf(number);
		log.debug("Generated 6-digit OTP: {}", otp);
		return otp;
	}

	private void sendOtpEmail(String email, String otp) {
		log.info("Sending OTP email to: {}", email);
		try {
			SimpleMailMessage message = new SimpleMailMessage();
			message.setTo(email);
			message.setSubject("Your Login OTP");
			message.setText("Your OTP is: " + otp + "\nIt expires in " + otpConfig.getExpiryMinutes() + " minutes.");
			mailSender.send(message);
			log.info("OTP email sent successfully to: {}", email);
		} catch (Exception e) {
			log.error("Failed to send OTP email to: {}, error: {}", email, e.getMessage(), e);
			throw e;
		}
	}

	private String generateJwtToken(String subjectEmail) {
		log.debug("Generating JWT token for subject: {}", subjectEmail);
		Key key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(JWT_SECRET_BASE64));
		Date now = new Date();
		Date expiry = new Date(now.getTime() + JWT_EXPIRATION_MS);
		String token = Jwts.builder()
				.setSubject(subjectEmail)
				.setIssuedAt(now)
				.setExpiration(expiry)
				.signWith(key, SignatureAlgorithm.HS256)
				.compact();
		log.debug("JWT token generated successfully for subject: {}", subjectEmail);
		return token;
	}
} 
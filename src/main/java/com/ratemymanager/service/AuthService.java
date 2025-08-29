package com.ratemymanager.service;

import com.ratemymanager.model.Otp;
import com.ratemymanager.model.User;
import com.ratemymanager.repository.OtpRepository;
import com.ratemymanager.repository.UserRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

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

	// NOTE: For MVP, keeping a hardcoded secret. Move to env var/Secret Manager in production.
	private static final String JWT_SECRET_BASE64 = "bXktdmVyeS1sb25nLXN1cGVyLXNlY3JldC1rZXktZm9yLWp3dC10ZXN0aW5nLW9ubHk=";
	private static final long JWT_EXPIRATION_MS = 1000L * 60 * 60 * 24; // 24 hours

	public AuthService(OtpRepository otpRepository, UserRepository userRepository, JavaMailSender mailSender) {
		this.otpRepository = otpRepository;
		this.userRepository = userRepository;
		this.mailSender = mailSender;
	}

	public void requestOtp(String email) {
		log.info("Processing OTP request for email: {}", email);
		validateCompanyEmail(email);
		String otp = generateSixDigitOtp();
		LocalDateTime expiry = LocalDateTime.now().plusMinutes(5);

		Otp otpEntity = new Otp();
		otpEntity.setEmail(email.toLowerCase());
		otpEntity.setOtpCode(otp);
		otpEntity.setExpiryTime(expiry);
		otpRepository.save(otpEntity);
		log.info("OTP saved to database for email: {}, expires at: {}", email, expiry);

		sendOtpEmail(email, otp);
		log.info("OTP email sent successfully to: {}", email);
	}

	public String verifyOtpAndGenerateToken(String email, String otp) {
		log.info("Verifying OTP for email: {}", email);
		validateCompanyEmail(email);
		Optional<Otp> match = otpRepository.findByEmailAndOtpCode(email.toLowerCase(), otp);
		if (match.isEmpty()) {
			log.warn("Invalid OTP provided for email: {}", email);
			throw new IllegalArgumentException("Invalid OTP");
		}
		Otp otpEntity = match.get();
		if (otpEntity.getExpiryTime().isBefore(LocalDateTime.now())) {
			log.warn("Expired OTP used for email: {}, expiry time: {}", email, otpEntity.getExpiryTime());
			throw new IllegalArgumentException("OTP expired");
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
			message.setText("Your OTP is: " + otp + "\nIt expires in 5 minutes.");
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
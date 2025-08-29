package com.ratemymanager.service;

import com.ratemymanager.model.Otp;
import com.ratemymanager.model.User;
import com.ratemymanager.repository.OtpRepository;
import com.ratemymanager.repository.UserRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Optional;
import java.util.Random;

@Service
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
		validateCompanyEmail(email);
		String otp = generateSixDigitOtp();
		LocalDateTime expiry = LocalDateTime.now().plusMinutes(5);

		Otp otpEntity = new Otp();
		otpEntity.setEmail(email.toLowerCase());
		otpEntity.setOtpCode(otp);
		otpEntity.setExpiryTime(expiry);
		otpRepository.save(otpEntity);

		sendOtpEmail(email, otp);
	}

	public String verifyOtpAndGenerateToken(String email, String otp) {
		validateCompanyEmail(email);
		Optional<Otp> match = otpRepository.findByEmailAndOtpCode(email.toLowerCase(), otp);
		if (match.isEmpty()) {
			throw new IllegalArgumentException("Invalid OTP");
		}
		Otp otpEntity = match.get();
		if (otpEntity.getExpiryTime().isBefore(LocalDateTime.now())) {
			throw new IllegalArgumentException("OTP expired");
		}

		User user = userRepository.findByEmail(email.toLowerCase())
				.orElseGet(() -> {
					User u = new User();
					u.setEmail(email.toLowerCase());
					return u;
				});
		user.setVerified(true);
		userRepository.save(user);

		// Clear existing OTPs for this email
		otpRepository.deleteByEmail(email.toLowerCase());

		return generateJwtToken(email.toLowerCase());
	}

	private void validateCompanyEmail(String email) {
		if (email == null || !email.toLowerCase().endsWith("@mycompany.com")) {
			throw new IllegalArgumentException("Only @mycompany.com emails are allowed");
		}
	}

	private String generateSixDigitOtp() {
		Random random = new Random();
		int number = 100000 + random.nextInt(900000);
		return String.valueOf(number);
	}

	private void sendOtpEmail(String email, String otp) {
		SimpleMailMessage message = new SimpleMailMessage();
		message.setTo(email);
		message.setSubject("Your Login OTP");
		message.setText("Your OTP is: " + otp + "\nIt expires in 5 minutes.");
		mailSender.send(message);
	}

	private String generateJwtToken(String subjectEmail) {
		Key key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(JWT_SECRET_BASE64));
		Date now = new Date();
		Date expiry = new Date(now.getTime() + JWT_EXPIRATION_MS);
		return Jwts.builder()
				.setSubject(subjectEmail)
				.setIssuedAt(now)
				.setExpiration(expiry)
				.signWith(key, SignatureAlgorithm.HS256)
				.compact();
	}
} 
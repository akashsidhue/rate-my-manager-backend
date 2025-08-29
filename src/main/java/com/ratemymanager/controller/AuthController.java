package com.ratemymanager.controller;

import com.ratemymanager.service.AuthService;
import com.ratemymanager.model.Manager;
import com.ratemymanager.model.Review;
import com.ratemymanager.model.User;
import com.ratemymanager.model.Otp;
import com.ratemymanager.repository.ManagerRepository;
import com.ratemymanager.repository.ReviewRepository;
import com.ratemymanager.repository.UserRepository;
import com.ratemymanager.repository.OtpRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@Slf4j
@RequestMapping("/auth")
@CrossOrigin(origins = "*")
public class AuthController {
	private final AuthService authService;
	
	@Autowired
	private JavaMailSender mailSender;
	
	@Autowired
	private ManagerRepository managerRepository;
	
	@Autowired
	private ReviewRepository reviewRepository;
	
	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private OtpRepository otpRepository;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@PostMapping("/request-otp")
	public ResponseEntity<?> requestOtp(@RequestHeader("email") String email) {
		log.info("OTP request received for email: {}", email);
		try {
			authService.requestOtp(email);
			Map<String, Object> resp = new HashMap<>();
			resp.put("message", "OTP sent if the email is allowed");
			log.info("OTP request processed successfully for email: {}", email);
			return ResponseEntity.ok(resp);
		} catch (Exception e) {
			log.error("Failed to process OTP request for email: {}, error: {}", email, e.getMessage(), e);
			Map<String, Object> errorResp = new HashMap<>();
			errorResp.put("error", "Failed to send OTP: " + e.getMessage());
			return ResponseEntity.status(500).body(errorResp);
		}
	}

	@PostMapping("/verify-otp")
	public ResponseEntity<?> verifyOtp(@RequestBody Map<String, String> body) {
		String email = body.get("email");
		String otp = body.get("otp");
		log.info("OTP verification request received for email: {}", email);
		
		try {
			String token = authService.verifyOtpAndGenerateToken(email, otp);
			Map<String, Object> resp = new HashMap<>();
			resp.put("token", token);
			log.info("OTP verification successful for email: {}", email);
			return ResponseEntity.ok(resp);
		} catch (Exception e) {
			log.error("OTP verification failed for email: {}, error: {}", email, e.getMessage(), e);
			Map<String, Object> errorResp = new HashMap<>();
			errorResp.put("error", e.getMessage());
			return ResponseEntity.status(400).body(errorResp);
		}
	}
	
	@PostMapping("/test-email")
	public ResponseEntity<?> testEmail(@RequestBody Map<String, String> body) {
		String toEmail = body.get("toEmail");
		String subject = body.get("subject");
		String message = body.get("message");
		
		log.info("Test email request received for: {}, subject: {}", toEmail, subject);
		
		try {
			if (toEmail == null || subject == null || message == null) {
				log.warn("Missing required fields in test email request: toEmail={}, subject={}, message={}", 
					toEmail, subject, message != null ? "present" : "null");
				Map<String, Object> errorResp = new HashMap<>();
				errorResp.put("error", "Missing required fields: toEmail, subject, message");
				return ResponseEntity.badRequest().body(errorResp);
			}
			
			SimpleMailMessage mailMessage = new SimpleMailMessage();
			mailMessage.setTo(toEmail);
			mailMessage.setSubject(subject);
			mailMessage.setText(message);
			
			mailSender.send(mailMessage);
			
			Map<String, Object> resp = new HashMap<>();
			resp.put("message", "Test email sent successfully to " + toEmail);
			resp.put("status", "success");
			log.info("Test email sent successfully to: {}", toEmail);
			return ResponseEntity.ok(resp);
			
		} catch (Exception e) {
			log.error("Failed to send test email to: {}, error: {}", toEmail, e.getMessage(), e);
			Map<String, Object> errorResp = new HashMap<>();
			errorResp.put("error", "Failed to send email: " + e.getMessage());
			errorResp.put("status", "error");
			return ResponseEntity.status(500).body(errorResp);
		}
	}
	
	@GetMapping("/test-data")
	public ResponseEntity<?> getTestData() {
		log.info("Test data request received");
		try {
			Map<String, Object> response = new HashMap<>();
			
			// Get top 5 managers
			List<Manager> managers = managerRepository.findAll().stream()
				.limit(5)
				.toList();
			response.put("managers", managers);
			log.info("Retrieved {} managers", managers.size());
			
			// Get top 5 reviews
			List<Review> reviews = reviewRepository.findAll().stream()
				.limit(5)
				.toList();
			response.put("reviews", reviews);
			log.info("Retrieved {} reviews", reviews.size());
			
			// Get top 5 users
			List<User> users = userRepository.findAll().stream()
				.limit(5)
				.toList();
			response.put("users", users);
			log.info("Retrieved {} users", users.size());
			
			// Get top 5 OTPs
			List<Otp> otps = otpRepository.findAll().stream()
				.limit(5)
				.toList();
			response.put("otps", otps);
			log.info("Retrieved {} OTPs", otps.size());
			
			// Add summary
			Map<String, Object> summary = new HashMap<>();
			summary.put("totalManagers", managerRepository.count());
			summary.put("totalReviews", reviewRepository.count());
			summary.put("totalUsers", userRepository.count());
			summary.put("totalOtps", otpRepository.count());
			response.put("summary", summary);
			
			log.info("Test data retrieved successfully - Managers: {}, Reviews: {}, Users: {}, OTPs: {}", 
				managers.size(), reviews.size(), users.size(), otps.size());
			
			return ResponseEntity.ok(response);
			
		} catch (Exception e) {
			log.error("Failed to retrieve test data, error: {}", e.getMessage(), e);
			Map<String, Object> errorResp = new HashMap<>();
			errorResp.put("error", "Failed to retrieve test data: " + e.getMessage());
			return ResponseEntity.status(500).body(errorResp);
		}
	}
	
	@PostMapping("/add-test-data")
	public ResponseEntity<?> addTestData() {
		log.info("Adding test data to all tables");
		try {
			Map<String, Object> response = new HashMap<>();
			
			// Add test manager
			Manager testManager = new Manager();
			testManager.setName("John Doe");
			testManager.setCompany("Tech Corp");
			testManager.setPosition("Engineering Manager");
			testManager.setRole("Team Lead");
			Manager savedManager = managerRepository.save(testManager);
			log.info("Added test manager: {}", savedManager.getName());
			
			// Add test user
			User testUser = new User();
			testUser.setEmail("test@example.com");
			testUser.setVerified(true);
			User savedUser = userRepository.save(testUser);
			log.info("Added test user: {}", savedUser.getEmail());
			
			// Add test review
			Review testReview = new Review();
			testReview.setManager(savedManager);
			testReview.setRating(4);
			testReview.setReviewText("Great manager, very supportive!");
			Review savedReview = reviewRepository.save(testReview);
			log.info("Added test review with rating: {}", savedReview.getRating());
			
			// Add test OTP
			Otp testOtp = new Otp();
			testOtp.setEmail("test@example.com");
			testOtp.setOtpCode("123456");
			testOtp.setExpiryTime(java.time.LocalDateTime.now().plusMinutes(5));
			Otp savedOtp = otpRepository.save(testOtp);
			log.info("Added test OTP for email: {}", savedOtp.getEmail());
			
			response.put("message", "Test data added successfully");
			response.put("manager", savedManager);
			response.put("user", savedUser);
			response.put("review", savedReview);
			response.put("otp", savedOtp);
			
			log.info("Test data added successfully to all tables");
			return ResponseEntity.ok(response);
			
		} catch (Exception e) {
			log.error("Failed to add test data, error: {}", e.getMessage(), e);
			Map<String, Object> errorResp = new HashMap<>();
			errorResp.put("error", "Failed to add test data: " + e.getMessage());
			return ResponseEntity.status(500).body(errorResp);
		}
	}
} 
package com.ratemymanager.controller;

import com.ratemymanager.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*")
public class AuthController {
	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@PostMapping("/request-otp")
	public ResponseEntity<?> requestOtp(@RequestParam String email) {
		authService.requestOtp(email);
		Map<String, Object> resp = new HashMap<>();
		resp.put("message", "OTP sent if the email is allowed");
		return ResponseEntity.ok(resp);
	}

	@PostMapping("/verify-otp")
	public ResponseEntity<?> verifyOtp(@RequestBody Map<String, String> body) {
		String email = body.get("email");
		String otp = body.get("otp");
		String token = authService.verifyOtpAndGenerateToken(email, otp);
		Map<String, Object> resp = new HashMap<>();
		resp.put("token", token);
		return ResponseEntity.ok(resp);
	}
} 
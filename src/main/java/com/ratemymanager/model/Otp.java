package com.ratemymanager.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Otp {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String email;

	@Column(nullable = false, length = 6)
	private String otpCode;

	@Column(nullable = false)
	private LocalDateTime expiryTime;

	@Column(nullable = false)
	private LocalDateTime createdAt = LocalDateTime.now();
	
	@Column(nullable = true) // Keep nullable to avoid DDL errors during migration
	private LocalDateTime expiresAt;
	
	@Column(nullable = true) // Keep nullable to avoid DDL errors during migration
	private Integer resendCount = 0;
	
	@Column(nullable = true) // Keep nullable to avoid DDL errors during migration
	private Integer verificationAttempts = 0;
} 
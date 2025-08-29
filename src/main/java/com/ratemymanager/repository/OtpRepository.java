package com.ratemymanager.repository;

import com.ratemymanager.model.Otp;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface OtpRepository extends JpaRepository<Otp, Long> {
	Optional<Otp> findTopByEmailOrderByCreatedAtDesc(String email);
	Optional<Otp> findByEmailAndOtpCode(String email, String otpCode);
	void deleteByEmail(String email);
} 
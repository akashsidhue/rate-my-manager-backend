package com.ratemymanager.repository;

import com.ratemymanager.model.Otp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OtpRepository extends JpaRepository<Otp, Long> {
	Optional<Otp> findTopByEmailOrderByCreatedAtDesc(String email);
	Optional<Otp> findByEmailAndOtpCode(String email, String otpCode);
	void deleteByEmail(String email);
	
	// Find OTPs created within the last X minutes for rate limiting
	@Query("SELECT o FROM Otp o WHERE o.email = :email AND o.createdAt >= :since")
	List<Otp> findByEmailAndCreatedAtAfter(@Param("email") String email, @Param("since") LocalDateTime since);
	
	// Count OTPs created within the last X minutes
	@Query("SELECT COUNT(o) FROM Otp o WHERE o.email = :email AND o.createdAt >= :since")
	Long countByEmailAndCreatedAtAfter(@Param("email") String email, @Param("since") LocalDateTime since);
	
	// Delete expired OTPs
	@Modifying
	@Transactional
	@Query("DELETE FROM Otp o WHERE o.expiresAt < :now")
	int deleteExpiredOtps(@Param("now") LocalDateTime now);
} 
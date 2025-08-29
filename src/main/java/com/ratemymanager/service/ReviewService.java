package com.ratemymanager.service;

import com.ratemymanager.model.Manager;
import com.ratemymanager.model.Review;
import com.ratemymanager.repository.ReviewRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@Slf4j
public class ReviewService {
    private final ReviewRepository reviewRepository;

    public ReviewService(ReviewRepository reviewRepository) {
        this.reviewRepository = reviewRepository;
    }

    public Review addReview(Review review) {
        log.info("Adding review for manager: {} (ID: {}), rating: {}", 
            review.getManager().getName(), review.getManager().getId(), review.getRating());
        
        if (review.getRating() < 1 || review.getRating() > 5) {
            log.warn("Invalid rating provided: {} for manager: {}", review.getRating(), review.getManager().getName());
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }
        
        // Basic profanity block (very MVP): reject if contains banned words
        if (review.getReviewText() != null) {
            var text = review.getReviewText().toLowerCase();
            String[] banned = {"abuse1", "abuse2", "abuse3"}; // replace with real list later
            for (String b : banned) {
                if (text.contains(b)) {
                    log.warn("Profanity detected in review for manager: {}, banned word: {}", 
                        review.getManager().getName(), b);
                    throw new IllegalArgumentException("Profanity not allowed");
                }
            }
            log.debug("Review text validation passed for manager: {}", review.getManager().getName());
        }
        
        Review savedReview = reviewRepository.save(review);
        log.info("Review saved successfully with ID: {} for manager: {}", 
            savedReview.getId(), savedReview.getManager().getName());
        return savedReview;
    }

    public List<Review> getReviews(Manager manager) {
        log.info("Getting reviews for manager: {} (ID: {})", manager.getName(), manager.getId());
        List<Review> reviews = reviewRepository.findByManager(manager);
        log.info("Found {} reviews for manager: {}", reviews.size(), manager.getName());
        return reviews;
    }
}

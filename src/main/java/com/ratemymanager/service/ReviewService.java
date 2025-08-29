package com.ratemymanager.service;

import com.ratemymanager.model.Manager;
import com.ratemymanager.model.Review;
import com.ratemymanager.repository.ReviewRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ReviewService {
    private final ReviewRepository reviewRepository;

    public ReviewService(ReviewRepository reviewRepository) {
        this.reviewRepository = reviewRepository;
    }

    public Review addReview(Review review) {
        if (review.getRating() < 1 || review.getRating() > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }
        // Basic profanity block (very MVP): reject if contains banned words
        if (review.getReviewText() != null) {
            var text = review.getReviewText().toLowerCase();
            String[] banned = {"abuse1", "abuse2", "abuse3"}; // replace with real list later
            for (String b : banned) {
                if (text.contains(b)) {
                    throw new IllegalArgumentException("Profanity not allowed");
                }
            }
        }
        return reviewRepository.save(review);
    }

    public List<Review> getReviews(Manager manager) {
        return reviewRepository.findByManager(manager);
    }
}

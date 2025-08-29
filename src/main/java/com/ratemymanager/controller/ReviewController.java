package com.ratemymanager.controller;

import com.ratemymanager.model.Review;
import com.ratemymanager.service.ReviewService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reviews")
@CrossOrigin(origins = "*")
@Slf4j
public class ReviewController {
    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping
    public Review addReview(@RequestBody Review review) {
        log.info("Adding new review for manager ID: {}, rating: {}", review.getManager().getId(), review.getRating());
        try {
            Review savedReview = reviewService.addReview(review);
            log.info("Review added successfully with ID: {} for manager: {}", 
                savedReview.getId(), savedReview.getManager().getName());
            return savedReview;
        } catch (Exception e) {
            log.error("Failed to add review for manager ID: {}, error: {}", 
                review.getManager().getId(), e.getMessage(), e);
            throw e;
        }
    }
}

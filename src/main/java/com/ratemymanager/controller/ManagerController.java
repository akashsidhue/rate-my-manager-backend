package com.ratemymanager.controller;

import com.ratemymanager.model.Manager;
import com.ratemymanager.model.Review;
import com.ratemymanager.service.ManagerService;
import com.ratemymanager.service.ReviewService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/managers")
@CrossOrigin(origins = "*")
@Slf4j
public class ManagerController {
    private final ManagerService managerService;
    private final ReviewService reviewService;

    public ManagerController(ManagerService managerService, ReviewService reviewService) {
        this.managerService = managerService;
        this.reviewService = reviewService;
    }

    @PostMapping
    public Manager addManager(@RequestBody Manager manager) {
        log.info("Adding new manager: {}", manager.getName());
        try {
            Manager savedManager = managerService.addManager(manager);
            log.info("Manager added successfully: {}", savedManager.getName());
            return savedManager;
        } catch (Exception e) {
            log.error("Failed to add manager: {}, error: {}", manager.getName(), e.getMessage(), e);
            throw e;
        }
    }

    @GetMapping
    public List<Manager> searchManagers(@RequestParam(required = false) String q) {
        log.info("Searching managers with query: {}", q != null ? q : "all");
        try {
            List<Manager> managers = managerService.searchManagers(q);
            log.info("Found {} managers for query: {}", managers.size(), q != null ? q : "all");
            return managers;
        } catch (Exception e) {
            log.error("Failed to search managers with query: {}, error: {}", q, e.getMessage(), e);
            throw e;
        }
    }

    @GetMapping("/{id}")
    public Map<String, Object> getManagerWithReviews(@PathVariable Long id) {
        log.info("Getting manager with reviews for ID: {}", id);
        try {
            Manager manager = managerService.getManager(id);
            List<Review> reviews = reviewService.getReviews(manager);
            double avg = reviews.stream().mapToInt(Review::getRating).average().orElse(0.0);
            
            Map<String, Object> resp = new HashMap<>();
            resp.put("manager", manager);
            resp.put("averageRating", avg);
            resp.put("reviews", reviews);
            
            log.info("Retrieved manager: {} with {} reviews, average rating: {}", 
                manager.getName(), reviews.size(), avg);
            return resp;
        } catch (Exception e) {
            log.error("Failed to get manager with reviews for ID: {}, error: {}", id, e.getMessage(), e);
            throw e;
        }
    }
}

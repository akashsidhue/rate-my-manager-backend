package com.ratemymanager.controller;

import com.ratemymanager.model.Manager;
import com.ratemymanager.model.Review;
import com.ratemymanager.service.ManagerService;
import com.ratemymanager.service.ReviewService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/managers")
@CrossOrigin(origins = "*")
public class ManagerController {
    private final ManagerService managerService;
    private final ReviewService reviewService;

    public ManagerController(ManagerService managerService, ReviewService reviewService) {
        this.managerService = managerService;
        this.reviewService = reviewService;
    }

    @PostMapping
    public Manager addManager(@RequestBody Manager manager) {
        return managerService.addManager(manager);
    }

    @GetMapping
    public List<Manager> searchManagers(@RequestParam(required = false) String q) {
        return managerService.searchManagers(q);
    }

    @GetMapping("/{id}")
    public Map<String, Object> getManagerWithReviews(@PathVariable Long id) {
        Manager manager = managerService.getManager(id);
        List<Review> reviews = reviewService.getReviews(manager);
        double avg = reviews.stream().mapToInt(Review::getRating).average().orElse(0.0);
        Map<String, Object> resp = new HashMap<>();
        resp.put("manager", manager);
        resp.put("averageRating", avg);
        resp.put("reviews", reviews);
        return resp;
    }
}

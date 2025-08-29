package com.ratemymanager.repository;

import com.ratemymanager.model.Review;
import com.ratemymanager.model.Manager;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByManager(Manager manager);
}

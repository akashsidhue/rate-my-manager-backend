package com.ratemymanager.repository;

import com.ratemymanager.model.Manager;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ManagerRepository extends JpaRepository<Manager, Long> {
    List<Manager> findByNameContainingIgnoreCase(String name);
    List<Manager> findByCompanyContainingIgnoreCase(String company);
}

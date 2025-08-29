package com.ratemymanager.service;

import com.ratemymanager.model.Manager;
import com.ratemymanager.repository.ManagerRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@Slf4j
public class ManagerService {
    private final ManagerRepository managerRepository;

    public ManagerService(ManagerRepository managerRepository) {
        this.managerRepository = managerRepository;
    }

    public Manager addManager(Manager manager) {
        log.info("Adding new manager: {} from company: {}", manager.getName(), manager.getCompany());
        Manager savedManager = managerRepository.save(manager);
        log.info("Manager saved successfully with ID: {}", savedManager.getId());
        return savedManager;
    }

    public List<Manager> searchManagers(String query) {
        if (query == null || query.isBlank()) {
            log.info("Searching all managers (no query provided)");
            List<Manager> allManagers = managerRepository.findAll();
            log.info("Found {} managers", allManagers.size());
            return allManagers;
        }
        
        log.info("Searching managers with query: '{}'", query);
        // Simple search by name or company for MVP
        var byName = managerRepository.findByNameContainingIgnoreCase(query);
        var byCompany = managerRepository.findByCompanyContainingIgnoreCase(query);
        log.debug("Found {} managers by name, {} managers by company", byName.size(), byCompany.size());
        
        // Merge & de-duplicate
        java.util.Set<Manager> set = new java.util.LinkedHashSet<>();
        set.addAll(byName);
        set.addAll(byCompany);
        List<Manager> results = new java.util.ArrayList<>(set);
        log.info("Returning {} unique managers for query: '{}'", results.size(), query);
        return results;
    }

    public Manager getManager(Long id) {
        log.info("Getting manager by ID: {}", id);
        Manager manager = managerRepository.findById(id)
            .orElseThrow(() -> {
                log.warn("Manager not found with ID: {}", id);
                return new RuntimeException("Manager not found");
            });
        log.info("Retrieved manager: {} (ID: {})", manager.getName(), manager.getId());
        return manager;
    }
}

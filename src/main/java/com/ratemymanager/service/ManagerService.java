package com.ratemymanager.service;

import com.ratemymanager.model.Manager;
import com.ratemymanager.repository.ManagerRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ManagerService {
    private final ManagerRepository managerRepository;

    public ManagerService(ManagerRepository managerRepository) {
        this.managerRepository = managerRepository;
    }

    public Manager addManager(Manager manager) {
        return managerRepository.save(manager);
    }

    public List<Manager> searchManagers(String query) {
        if (query == null || query.isBlank()) {
            return managerRepository.findAll();
        }
        // Simple search by name or company for MVP
        var byName = managerRepository.findByNameContainingIgnoreCase(query);
        var byCompany = managerRepository.findByCompanyContainingIgnoreCase(query);
        // Merge & de-duplicate
        java.util.Set<Manager> set = new java.util.LinkedHashSet<>();
        set.addAll(byName);
        set.addAll(byCompany);
        return new java.util.ArrayList<>(set);
    }

    public Manager getManager(Long id) {
        return managerRepository.findById(id).orElseThrow(() -> new RuntimeException("Manager not found"));
    }
}

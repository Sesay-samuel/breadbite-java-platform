package com.breadbitt.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.breadbitt.domain.Bread;


public interface BreadRepository extends JpaRepository<Bread, Long> {
    // Additional query methods (if any) can be defined here
}

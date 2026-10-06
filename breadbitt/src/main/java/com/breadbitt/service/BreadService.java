package com.breadbitt.service;

import java.util.List;
import java.util.Optional;

import com.breadbitt.domain.Bread;


public interface BreadService {
    List<Bread> findAll();
    Optional<Bread> findById(Long id); 
	Optional<Bread> findById(Bread bread);
}
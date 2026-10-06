package com.breadbitt.service.impl;


import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.breadbitt.domain.Bread;
import com.breadbitt.repository.BreadRepository;
import com.breadbitt.service.BreadService;

@Service
public class BreadServiceImpl implements BreadService {

    @Autowired
    private BreadRepository breadRepository;

    @Override
    public List<Bread> findAll() {
        return breadRepository.findAll();
    }

    @Override
    public Optional<Bread> findById(Long id) {
        return breadRepository.findById(id);
    }

	@Override
	public Optional<Bread> findById(Bread bread) {
		// TODO Auto-generated method stub
		return Optional.of(bread);
	}
}



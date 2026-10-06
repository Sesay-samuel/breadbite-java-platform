package com.adminportal.service.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.adminportal.domain.Bread;
import com.adminportal.repository.BreadRepository;
import com.adminportal.service.BreadService;

@Service
public class BreadServiceImpl implements BreadService{
	
	@Autowired
	private BreadRepository breadRepository;
	
	public Bread save(Bread bread) {
		return breadRepository.save(bread);
	}
	public List<Bread> findAll() {
		return (List<Bread>) breadRepository.findAll();
	}
	

	public Optional<Bread> findOne(Long id) {
		return breadRepository.findById(id);
	}
	
}


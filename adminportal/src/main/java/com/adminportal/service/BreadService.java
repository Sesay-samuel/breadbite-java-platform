package com.adminportal.service;

import java.util.List;
import java.util.Optional;

import com.adminportal.domain.Bread;

public interface BreadService {
	
	Bread save(Bread book);
	
	List<Bread> findAll();
	
	Optional<Bread> findOne(Long id);

}

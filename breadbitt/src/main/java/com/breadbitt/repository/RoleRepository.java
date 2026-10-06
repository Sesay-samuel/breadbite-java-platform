package com.breadbitt.repository;

import org.springframework.data.repository.CrudRepository;

import com.breadbitt.domain.security.Role;

public interface RoleRepository extends CrudRepository<Role, Long> {
	Role findByname(String name);
}
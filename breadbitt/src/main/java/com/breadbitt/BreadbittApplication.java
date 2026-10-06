package com.breadbitt;


import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

import com.breadbitt.domain.User;
import com.breadbitt.domain.security.Role;
import com.breadbitt.domain.security.UserRole;
import com.breadbitt.repository.RoleRepository;
import com.breadbitt.service.UserService;
import com.breadbitt.utility.SecurityUtility;

@SpringBootApplication
@ComponentScan(basePackages = {"com.breadbitt"})
public class BreadbittApplication implements CommandLineRunner {

    @Autowired
    private UserService userService;

    @Autowired
    private RoleRepository roleRepository;

    public static void main(String[] args) {
        SpringApplication.run(BreadbittApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        User user1 = new User();
        user1.setFirstName("Lobah");
        user1.setLastName("Sesay");
        user1.setUsername("S");
        user1.setPassword(SecurityUtility.passwordEncoder().encode("p"));
        user1.setEmail("SSesay@gmail.com");

        // Create and save role
        Role role1 = new Role();
        role1.setRoleId(1);
        role1.setName("ROLE_USER");
        role1 = roleRepository.save(role1); // Save role directly using RoleRepository

        Set<UserRole> userRoles = new HashSet<>();
        userRoles.add(new UserRole(user1, role1));

        userService.createUser(user1, userRoles);
    }
}

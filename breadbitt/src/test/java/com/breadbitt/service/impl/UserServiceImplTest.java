
package com.breadbitt.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.breadbitt.domain.User;
import com.breadbitt.domain.security.Role;
import com.breadbitt.domain.security.UserRole;
import com.breadbitt.repository.RoleRepository;
import com.breadbitt.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private UserServiceImpl userService;

    private User user;
    private Role customerRole;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setUsername("testcustomer");
        user.setEmail("customer@example.com");

        customerRole = new Role();
        customerRole.setRoleId(1);
        customerRole.setName("ROLE_USER");
    }

    @Test
    void registrationAssignsCustomerRole() {

        when(userRepository.findByUsername("testcustomer"))
                .thenReturn(null);

        when(roleRepository.findByName("ROLE_USER"))
                .thenReturn(customerRole);

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        User savedUser = userService.createUser(
                user, new HashSet<>());

        assertEquals(
                1, savedUser.getUserRoles().size());

        assertEquals(
                "ROLE_USER",
                savedUser.getUserRoles()
                        .iterator()
                        .next()
                        .getRole()
                        .getName());
    }

    @Test
    void registrationCannotAssignAdminRole() {

        Role adminRole = new Role();
        adminRole.setRoleId(2);
        adminRole.setName("ROLE_ADMIN");

        Set<UserRole> requestedRoles = new HashSet<>();
        requestedRoles.add(new UserRole(user, adminRole));

        when(userRepository.findByUsername("testcustomer"))
                .thenReturn(null);

        when(roleRepository.findByName("ROLE_USER"))
                .thenReturn(customerRole);

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        User savedUser = userService.createUser(
                user, requestedRoles);

        assertTrue(
                savedUser.getUserRoles()
                        .stream()
                        .allMatch(userRole -> "ROLE_USER".equals(
                                userRole.getRole()
                                        .getName())));

        assertFalse(
                savedUser.getUserRoles()
                        .stream()
                        .anyMatch(userRole -> "ROLE_ADMIN".equals(
                                userRole.getRole()
                                        .getName())));
    }

    @Test
    void duplicateUsernameIsRejected() {

        when(userRepository.findByUsername("testcustomer"))
                .thenReturn(new User());

        assertThrows(
                IllegalArgumentException.class,
                () -> userService.createUser(
                        user, new HashSet<>()));

        verify(userRepository, never())
                .save(any(User.class));
    }
}

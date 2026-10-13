
package com.breadbitt.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.access.AccessDeniedException;

import com.breadbitt.domain.User;
import com.breadbitt.domain.UserPayment;
import com.breadbitt.domain.UserShipping;
import com.breadbitt.domain.security.Role;
import com.breadbitt.domain.security.UserRole;

import com.breadbitt.repository.RoleRepository;
import com.breadbitt.repository.UserPaymentRepository;
import com.breadbitt.repository.UserRepository;
import com.breadbitt.repository.UserShippingRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

        @Mock
        private UserRepository userRepository;

        @Mock
        private RoleRepository roleRepository;

        @Mock
        private UserPaymentRepository userPaymentRepository;

        @Mock
        private UserShippingRepository userShippingRepository;

        @InjectMocks
        private UserServiceImpl userService;

        private User user;
        private Role customerRole;

        @BeforeEach
        void setUp() {

                user = new User();
                user.setId(100L);
                user.setUsername("testcustomer");
                user.setEmail("customer@example.com");

                customerRole = new Role();
                customerRole.setRoleId(1);
                customerRole.setName("ROLE_USER");
        }

        // =====================================================
        // EXISTING REGISTRATION SECURITY TESTS
        // =====================================================

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
                                1,
                                savedUser.getUserRoles().size());

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

        // =====================================================
        // PAYMENT OWNERSHIP SECURITY TESTS
        // =====================================================

        @Test
        void customerCanSetOwnDefaultPayment() {

                UserPayment firstPayment = new UserPayment();
                firstPayment.setId(1L);
                firstPayment.setUser(user);
                firstPayment.setDefaultPayment(true);

                UserPayment secondPayment = new UserPayment();
                secondPayment.setId(2L);
                secondPayment.setUser(user);
                secondPayment.setDefaultPayment(false);

                List<UserPayment> payments = Arrays.asList(firstPayment, secondPayment);

                when(userPaymentRepository.findByUser_Id(100L))
                                .thenReturn(payments);

                userService.setUserDefaultPayment(2L, user);

                assertFalse(firstPayment.isDefaultPayment());
                assertTrue(secondPayment.isDefaultPayment());

                verify(userPaymentRepository)
                                .saveAll(payments);

                verify(userPaymentRepository, never())
                                .findAll();
        }

        @Test
        void customerCannotSetAnotherUsersDefaultPayment() {

                UserPayment ownPayment = new UserPayment();
                ownPayment.setId(1L);
                ownPayment.setUser(user);
                ownPayment.setDefaultPayment(true);

                when(userPaymentRepository.findByUser_Id(100L))
                                .thenReturn(Collections.singletonList(ownPayment));

                assertThrows(
                                AccessDeniedException.class,
                                () -> userService.setUserDefaultPayment(
                                                999L, user));

                assertTrue(ownPayment.isDefaultPayment());

                verify(userPaymentRepository, never())
                                .saveAll(any());

                verify(userPaymentRepository, never())
                                .findAll();
        }

        @Test
        void customerCannotSetDefaultPaymentWithInvalidUser() {

                assertThrows(
                                IllegalArgumentException.class,
                                () -> userService.setUserDefaultPayment(
                                                1L, null));

                verifyNoInteractions(userPaymentRepository);
        }

        // =====================================================
        // SHIPPING OWNERSHIP SECURITY TESTS
        // =====================================================

        @Test
        void customerCanSetOwnDefaultShippingAddress() {

                UserShipping firstAddress = new UserShipping();
                firstAddress.setId(10L);
                firstAddress.setUser(user);
                firstAddress.setUserShippingDefault(true);

                UserShipping secondAddress = new UserShipping();
                secondAddress.setId(20L);
                secondAddress.setUser(user);
                secondAddress.setUserShippingDefault(false);

                List<UserShipping> addresses = Arrays.asList(firstAddress, secondAddress);

                when(userShippingRepository.findByUser_Id(100L))
                                .thenReturn(addresses);

                userService.setUserDefaultShipping(20L, user);

                assertFalse(firstAddress.isUserShippingDefault());
                assertTrue(secondAddress.isUserShippingDefault());

                verify(userShippingRepository)
                                .saveAll(addresses);

                verify(userShippingRepository, never())
                                .findAll();
        }

        @Test
        void customerCannotSetAnotherUsersDefaultShippingAddress() {

                UserShipping ownAddress = new UserShipping();
                ownAddress.setId(10L);
                ownAddress.setUser(user);
                ownAddress.setUserShippingDefault(true);

                when(userShippingRepository.findByUser_Id(100L))
                                .thenReturn(
                                                Collections.singletonList(ownAddress));

                assertThrows(
                                AccessDeniedException.class,
                                () -> userService.setUserDefaultShipping(
                                                999L, user));

                assertTrue(ownAddress.isUserShippingDefault());

                verify(userShippingRepository, never())
                                .saveAll(any());

                verify(userShippingRepository, never())
                                .findAll();
        }

        @Test
        void customerCannotSetDefaultShippingWithInvalidUser() {

                assertThrows(
                                IllegalArgumentException.class,
                                () -> userService.setUserDefaultShipping(
                                                10L, null));

                verifyNoInteractions(userShippingRepository);
        }
}

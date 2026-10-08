
package com.adminportal.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.never;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.adminportal.domain.Bread;
import com.adminportal.service.BreadService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BreadService breadService;

    // 1. Anonymous users cannot access the admin portal.

    @Test
    void anonymousUserCannotAccessAdminPortal() throws Exception {

        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/login"));
    }

    // 2. Customers cannot access admin-only pages.

    @Test
    void customerCannotAccessAdminPortal() throws Exception {

        mockMvc.perform(
                get("/")
                        .with(user("customer").roles("USER")))
                .andExpect(status().isForbidden());
    }

    // 3. Administrators can access the admin portal.

    @Test
    void administratorCanAccessAdminPortal() throws Exception {

        mockMvc.perform(
                get("/")
                        .with(user("administrator").roles("ADMIN")))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/home"));
    }

    // 4. Logout without CSRF token must be rejected.

    @Test
    void postLogoutWithoutCsrfTokenIsForbidden() throws Exception {

        mockMvc.perform(
                post("/logout")
                        .with(user("administrator").roles("ADMIN")))
                .andExpect(status().isForbidden());
    }

    // 5. Logout with a valid CSRF token succeeds.

    @Test
    void postLogoutWithCsrfTokenSucceeds() throws Exception {

        mockMvc.perform(
                post("/logout")
                        .with(user("administrator").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?logout"));
    }

    // 6. GET logout must not log out the administrator.

    @Test
    void getLogoutDoesNotLogOutAdministrator() throws Exception {

        mockMvc.perform(
                get("/logout")
                        .with(user("administrator").roles("ADMIN")))
                .andExpect(status().isNotFound());
    }

    // 7. Adding bread without CSRF must be rejected.

    @Test
    void addBreadWithoutCsrfTokenIsForbidden() throws Exception {

        mockMvc.perform(
                post("/home/bread/add")
                        .with(user("administrator").roles("ADMIN")))
                .andExpect(status().isForbidden());
    }

    // 8. Customers cannot add bread, even with CSRF.

    @Test
    void customerCannotAddBreadEvenWithCsrfToken() throws Exception {

        mockMvc.perform(
                post("/home/bread/add")
                        .with(user("customer").roles("USER"))
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    // 9. Administrator can add bread with a valid CSRF token.

    @Test
    void administratorCanAddBreadWithCsrfToken() throws Exception {

        // Configure the mocked service to return saved bread.

        Bread savedBread = new Bread();
        savedBread.setId(1L);
        savedBread.setTitle("Test Bread");

        when(breadService.save(any(Bread.class)))
                .thenReturn(savedBread);

        // Empty image upload prevents filesystem writes.

        MockMultipartFile breadImage = new MockMultipartFile(
                "breadImage",
                "",
                "application/octet-stream",
                new byte[0]);

        mockMvc.perform(
                multipart("/home/bread/add")
                        .file(breadImage)
                        .param("title", "Test Bread")
                        .param("category", "Bakery")
                        .param("listPrice", "5.00")
                        .param("ourPrice", "4.50")
                        .param("inStockNumber", "10")
                        .with(user("administrator").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(
                        redirectedUrl("/home/bread/breadList"));

        // Confirm the controller called the service.

        verify(breadService).save(any(Bread.class));
    }

    @Test
    void administratorCannotUploadFakePng() throws Exception {

        MockMultipartFile fakePng = new MockMultipartFile(
                "breadImage",
                "fake.png",
                "image/png",
                new byte[] { 1, 2, 3, 4 });

        mockMvc.perform(
                multipart("/home/bread/add")
                        .file(fakePng)
                        .param("title", "Test Bread")
                        .with(user("administrator").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isBadRequest());

        verify(breadService, never()).save(any(Bread.class));
    }

    @Test
    void administratorCannotUploadOversizedImage() throws Exception {

        byte[] oversizedImage = new byte[5 * 1024 * 1024 + 1];

        MockMultipartFile image = new MockMultipartFile(
                "breadImage",
                "large.png",
                "image/png",
                oversizedImage);

        mockMvc.perform(
                multipart("/home/bread/add")
                        .file(image)
                        .param("title", "Test Bread")
                        .with(user("administrator").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isBadRequest());

        verify(breadService, never()).save(any(Bread.class));
    }

    @Test
    void customerCannotUploadBreadImage() throws Exception {

        MockMultipartFile image = new MockMultipartFile(
                "breadImage",
                "bread.png",
                "image/png",
                new byte[] {
                        (byte) 0x89, 0x50, 0x4E, 0x47,
                        0x0D, 0x0A, 0x1A, 0x0A
                });

        mockMvc.perform(
                multipart("/home/bread/add")
                        .file(image)
                        .param("title", "Test Bread")
                        .with(user("customer").roles("USER"))
                        .with(csrf()))
                .andExpect(status().isForbidden());

        verify(breadService, never()).save(any(Bread.class));
    }

}
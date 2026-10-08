
package com.adminportal.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;

import com.adminportal.domain.Bread;
import com.adminportal.service.BreadService;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BreadControllerUpdateTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BreadService breadService;

    private Bread existingBread;

    @BeforeEach
    void setUp() {

        existingBread = new Bread();
        existingBread.setId(1L);
        existingBread.setTitle("Original Bread");
        existingBread.setBaker("Original Baker");
        existingBread.setSku("BREAD-001");
        existingBread.setIngredients("Flour, Water, Yeast");
        existingBread.setCategory("Bakery");
        existingBread.setOurPrice(4.00);
        existingBread.setListPrice(5.00);
        existingBread.setInStockNumber(20);
        existingBread.setActive(true);

        when(breadService.findOne(1L))
                .thenReturn(Optional.of(existingBread));

        when(breadService.save(any(Bread.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void customerCannotUpdateBread() throws Exception {

        mockMvc.perform(
                post("/home/bread/updateBread")
                        .param("id", "1")
                        .param("title", "Changed Bread")
                        .with(user("customer").roles("USER"))
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void administratorCannotUpdateWithoutCsrf() throws Exception {

        mockMvc.perform(
                post("/home/bread/updateBread")
                        .param("id", "1")
                        .param("title", "Changed Bread")
                        .with(user("administrator").roles("ADMIN")))
                .andExpect(status().isForbidden());
    }

    @Test
    void administratorCanUpdateBreadWithCsrf() throws Exception {

        mockMvc.perform(
                post("/home/bread/updateBread")
                        .param("id", "1")
                        .param("title", "Updated Bread")
                        .param("category", "Bakery")
                        .param("listPrice", "6.00")
                        .param("ourPrice", "5.50")
                        .param("inStockNumber", "15")
                        .param("active", "true")
                        .with(user("administrator").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(
                        redirectedUrl("/home/bread/breadInfo?id=1"));

        verify(breadService).save(any(Bread.class));
    }

    @Test
    void updatingBreadPreservesExistingFields() throws Exception {

        mockMvc.perform(
                post("/home/bread/updateBread")
                        .param("id", "1")
                        .param("title", "Updated Bread")
                        .param("category", "Bakery")
                        .param("listPrice", "6.00")
                        .param("ourPrice", "5.50")
                        .param("inStockNumber", "15")
                        .param("active", "true")
                        .with(user("administrator").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());

        ArgumentCaptor<Bread> breadCaptor = ArgumentCaptor.forClass(Bread.class);

        verify(breadService).save(breadCaptor.capture());

        Bread savedBread = breadCaptor.getValue();

        assertNotNull(savedBread);

        // Updated fields
        assertEquals("Updated Bread", savedBread.getTitle());
        assertEquals(5.50, savedBread.getOurPrice());
        assertEquals(15, savedBread.getInStockNumber());

        // Existing fields must remain unchanged
        assertEquals("Original Baker", savedBread.getBaker());
        assertEquals("BREAD-001", savedBread.getSku());
        assertEquals(
                "Flour, Water, Yeast",
                savedBread.getIngredients());
    }
}

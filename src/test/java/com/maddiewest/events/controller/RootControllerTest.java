package com.maddiewest.events.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RootControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new RootController()).build();
    }

    @Test
    void rootPathRedirectsToSwaggerUiWhenHostIncludesExplicitPort() throws Exception {
        mockMvc.perform(get("/").header("Host", "localhost:8080"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/swagger-ui.html"));
    }

    @Test
    void rootPathDoesNotRedirectForBareLocalhostHost() throws Exception {
        mockMvc.perform(get("/").header("Host", "localhost"))
                .andExpect(status().isNotFound());
    }
}

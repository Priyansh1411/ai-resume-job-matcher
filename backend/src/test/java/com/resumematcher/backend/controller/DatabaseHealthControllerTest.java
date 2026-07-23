package com.resumematcher.backend.controller;

import static org.mockito.Mockito.when;

import com.resumematcher.backend.service.DatabaseHealthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@WebMvcTest(DatabaseHealthController.class)
class DatabaseHealthControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private DatabaseHealthService databaseHealthService;

	@Test
	void databaseHealthEndpointReturnsUpStatusWhenDatabaseIsReachable() throws Exception {
		when(databaseHealthService.isDatabaseUp()).thenReturn(true);

		mockMvc.perform(MockMvcRequestBuilders.get("/api/database/health").accept(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.jsonPath("$.status").value("UP"))
				.andExpect(MockMvcResultMatchers.jsonPath("$.database").value("MySQL"));
	}

}

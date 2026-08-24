package com.resumematcher.backend.security;

import com.resumematcher.backend.testsupport.AbstractMySqlIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

/**
 * Proves the actual wiring of {@link SecurityConfiguration}'s referrer-policy
 * configuration, not just that the builder call compiles: unlike
 * X-Content-Type-Options/X-Frame-Options/X-XSS-Protection (already on by
 * Spring Security's own default, needing no configuration - see
 * SecurityConfigurationIntegrationTest for those already being exercised
 * indirectly), Referrer-Policy is opt-in, so this is the only thing that
 * would catch a typo or a dropped `.headers(...)` call.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SecurityHeadersIntegrationTest extends AbstractMySqlIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void responsesIncludeAStrictReferrerPolicy() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.get("/api/health"))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.header().string("Referrer-Policy", "strict-origin-when-cross-origin"));
	}

}

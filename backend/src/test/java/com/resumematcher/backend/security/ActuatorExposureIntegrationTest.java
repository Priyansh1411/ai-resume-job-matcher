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
 * Proves the actual wiring of {@code management.endpoints.web.exposure.include}
 * (application.properties), not just the property value: unauthenticated,
 * {@code /actuator/metrics} used to disclose DB connection pool internals,
 * per-route request/status/timing breakdowns, and even internal filter class
 * names (spring.security.filterchains.JwtAuthenticationFilter.*) - verified
 * live against a running instance during Phase 8 planning. {@code /actuator/health}
 * stays exposed - it's low-risk (show-details defaults to "never", so this is
 * UP/DOWN only) and costs nothing to leave available for the platform
 * convention of probing it, even though nothing in this repo currently does.
 *
 * <p>Deliberately not folded into {@link SecurityConfigurationIntegrationTest}:
 * that file's stated scope is the 401-vs-403 JSON error-handling wiring: this
 * is a different concern (endpoint exposure, not authentication semantics),
 * and {@code SecurityConfiguration.java} isn't touched by this fix at all -
 * the exposure list, not an authorization rule, is what changed.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ActuatorExposureIntegrationTest extends AbstractMySqlIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void actuatorHealthRemainsPubliclyAccessible() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.get("/actuator/health"))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.jsonPath("$.status").value("UP"));
	}

	@Test
	void actuatorMetricsIsNoLongerExposed() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.get("/actuator/metrics"))
				.andExpect(MockMvcResultMatchers.status().isNotFound());
	}

	@Test
	void actuatorMetricsDetailIsNoLongerExposed() throws Exception {
		// A specific metric id, not just the listing endpoint above - proves the
		// whole metrics group is gone, not merely its index.
		mockMvc.perform(MockMvcRequestBuilders.get("/actuator/metrics/jvm.memory.used"))
				.andExpect(MockMvcResultMatchers.status().isNotFound());
	}

}

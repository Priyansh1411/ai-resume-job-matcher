package com.resumematcher.backend.security;

import com.resumematcher.backend.testsupport.AbstractMySqlIntegrationTest;
import com.resumematcher.backend.testsupport.TestAuthSupport;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.micrometer.metrics.test.autoconfigure.AutoConfigureMetrics;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

/**
 * Proves the Phase 13 authorization rule added to {@link SecurityConfiguration}
 * for {@code /actuator/prometheus}: unlike {@code /actuator/health}, which is
 * left to {@code .anyRequest().permitAll()} (see {@link ActuatorExposureIntegrationTest}),
 * prometheus carries the same class of sensitive detail Phase 8 removed from
 * {@code /actuator/metrics} - DB pool internals, per-route timing, and this
 * app's own {@code OpenAiMetrics}/{@code RateLimitMetrics}/{@code ResumeProcessingMetrics}
 * - so it requires the existing JWT authentication, not a new mechanism.
 *
 * <p>Deliberately not folded into {@link ActuatorExposureIntegrationTest}: that
 * file's stated scope is exposure-list wiring where {@code SecurityConfiguration.java}
 * is untouched. This change does touch it, matching why authorization-semantics
 * tests live in {@link SecurityConfigurationIntegrationTest} rather than there too.
 *
 * <p>Two test-context-only quirks needed working around here, neither of which
 * reflects production behavior (verified by directly dumping the Environment's
 * property sources during Phase 13 implementation):
 * <ul>
 * <li>{@code @AutoConfigureMetrics} - Spring Boot's test support disables metrics
 * export by default for every {@code @SpringBootTest} (a {@code DisableMetricsExportContextCustomizer}
 * from {@code spring-boot-starter-actuator-test}, to keep unrelated tests fast) and
 * this opts back in for this class only.</li>
 * <li>{@code @TestPropertySource} - {@code src/test/resources/application.properties}
 * shadows {@code src/main/resources/application.properties} entirely during test
 * runs (only one {@code application.properties} loads from the classpath, and the
 * test-classes one wins the lookup), so {@code management.endpoints.web.exposure.include}
 * from the main file - the actual production setting, added above in
 * {@code application.properties} - is invisible to every test, not just this one.
 * This restates it locally rather than editing the shared test properties file,
 * which would be a broader change than this phase approved.</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureMetrics
@TestPropertySource(properties = "management.endpoints.web.exposure.include=health,prometheus")
class PrometheusEndpointSecurityIntegrationTest extends AbstractMySqlIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void authenticatedRequestToPrometheusEndpointSucceeds() throws Exception {
		String authHeader = TestAuthSupport.registerAndGetAuthorizationHeader(mockMvc);

		mockMvc.perform(MockMvcRequestBuilders.get("/actuator/prometheus")
						.header("Authorization", authHeader))
				.andExpect(MockMvcResultMatchers.status().isOk())
				// Proves the Prometheus registry is actually wired in and scraping real
				// meters, not just that the endpoint returns 200 with an empty body.
				.andExpect(MockMvcResultMatchers.content().string(
						Matchers.containsString("jvm_memory_used_bytes")));
	}

	@Test
	void unauthenticatedRequestToPrometheusEndpointIsRejected() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.get("/actuator/prometheus"))
				.andExpect(MockMvcResultMatchers.status().isUnauthorized())
				// Same JSON shape as every other 401 in this app (JsonAuthenticationEntryPoint),
				// not the framework's default error page - proves this went through the
				// normal JWT-authentication path, not some endpoint-specific handling.
				.andExpect(MockMvcResultMatchers.jsonPath("$.error")
						.value("Authentication is required to access this resource"));
	}

	@Test
	void unauthenticatedRequestWithAMalformedTokenIsAlsoRejected() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.get("/actuator/prometheus")
						.header("Authorization", "Bearer not-a-real-jwt"))
				.andExpect(MockMvcResultMatchers.status().isUnauthorized());
	}

	@Test
	void actuatorHealthStaysPubliclyAccessibleAndUnaffectedByThePrometheusRule() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.get("/actuator/health"))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.jsonPath("$.status").value("UP"));
	}

	@Test
	void actuatorMetricsStaysRemovedByThePhase8ExposureFix() throws Exception {
		String authHeader = TestAuthSupport.registerAndGetAuthorizationHeader(mockMvc);

		// Even with a valid token, /actuator/metrics was never added back to
		// management.endpoints.web.exposure.include - it stays 404 regardless of
		// authentication, proving Phase 13 only broadened prometheus, not metrics.
		mockMvc.perform(MockMvcRequestBuilders.get("/actuator/metrics")
						.header("Authorization", authHeader))
				.andExpect(MockMvcResultMatchers.status().isNotFound());
	}

}

package com.resumematcher.backend.security;

import java.util.UUID;

import com.resumematcher.backend.testsupport.AbstractMySqlIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.transaction.annotation.Transactional;

/**
 * Proves the actual gap this phase closes: {@link AuthRateLimitingFilter}'s
 * existing per-IP budget (kept unchanged) only stops a single source from
 * flooding any account - it does nothing against attempts spread across many
 * IPs targeting one account. The per-IP limit here is set generously high so
 * every individual attempt is nowhere near it, isolating the per-account check
 * added to {@link AuthService#login} as the only thing that could plausibly
 * cause the final assertion to fail.
 *
 * <p>Runs in its own Spring context (via the property overrides below) rather
 * than the shared one every other integration test uses, since the shared
 * context has rate limiting disabled entirely (see the reasoning in
 * src/test/resources/application.properties) specifically so unrelated tests'
 * fixture setup - which calls /api/auth/register and /api/auth/login
 * liberally - never collides with real limits the way it did before that was
 * added.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = {
		"rate-limit.enabled=true",
		"rate-limit.auth-login.requests-per-minute=1000",
		"rate-limit.auth-login-per-account.requests-per-minute=2",
})
class LoginPerAccountThrottlingIntegrationTest extends AbstractMySqlIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void throttlesLoginAttemptsAgainstOneAccountEvenWhenSpreadAcrossManyIps() throws Exception {
		String email = "target-" + UUID.randomUUID() + "@example.com";
		registerUser(email, "correct-horse-battery-staple");

		// Two attempts against the same account from two different IPs - each
		// individually nowhere near the 1000/min per-IP budget - consume the
		// 2-request-per-minute per-account budget.
		loginAttempt(email, "wrong-password", "10.0.0.1")
				.andExpect(MockMvcResultMatchers.status().isUnauthorized());
		loginAttempt(email, "wrong-password", "10.0.0.2")
				.andExpect(MockMvcResultMatchers.status().isUnauthorized());

		// A third attempt against the same account, from yet another fresh IP -
		// still nowhere near its own per-IP limit - must now be throttled at the
		// account level, proving the per-IP filter alone could never have caught this.
		loginAttempt(email, "wrong-password", "10.0.0.3")
				.andExpect(MockMvcResultMatchers.status().isTooManyRequests())
				.andExpect(MockMvcResultMatchers.header().exists("Retry-After"))
				.andExpect(MockMvcResultMatchers.jsonPath("$.error")
						.value("Too many requests. Please try again later."));
	}

	@Test
	void aDifferentAccountIsUnaffectedByAnotherAccountsExhaustedBudget() throws Exception {
		String targetEmail = "target-" + UUID.randomUUID() + "@example.com";
		String otherEmail = "other-" + UUID.randomUUID() + "@example.com";
		registerUser(targetEmail, "correct-horse-battery-staple");
		registerUser(otherEmail, "correct-horse-battery-staple");

		loginAttempt(targetEmail, "wrong-password", "10.0.0.1")
				.andExpect(MockMvcResultMatchers.status().isUnauthorized());
		loginAttempt(targetEmail, "wrong-password", "10.0.0.2")
				.andExpect(MockMvcResultMatchers.status().isUnauthorized());
		loginAttempt(targetEmail, "wrong-password", "10.0.0.3")
				.andExpect(MockMvcResultMatchers.status().isTooManyRequests());

		// A different account's budget was never touched by the above.
		loginAttempt(otherEmail, "wrong-password", "10.0.0.4")
				.andExpect(MockMvcResultMatchers.status().isUnauthorized());
	}

	private void registerUser(String email, String password) throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
				.andExpect(MockMvcResultMatchers.status().isCreated());
	}

	private ResultActions loginAttempt(String email, String password, String remoteAddr) throws Exception {
		return mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/login")
				.with(request -> {
					request.setRemoteAddr(remoteAddr);
					return request;
				})
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"));
	}

}

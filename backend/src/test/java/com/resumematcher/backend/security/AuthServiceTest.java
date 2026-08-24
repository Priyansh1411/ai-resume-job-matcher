package com.resumematcher.backend.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import com.resumematcher.backend.dto.AuthResponse;
import com.resumematcher.backend.entity.User;
import com.resumematcher.backend.observability.RateLimitMetrics;
import com.resumematcher.backend.ratelimit.RateLimiterService;
import com.resumematcher.backend.repository.UserRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

	@Mock
	private UserRepository userRepository;

	private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
	private final JwtService jwtService = new JwtService("test-only-jwt-signing-secret-must-be-at-least-32-bytes", 24);
	private final TokenRevocationService tokenRevocationService = new TokenRevocationService();
	// Generous enough that no existing test below (each logs in at most once or
	// twice for a given email) can ever collide with the per-account budget -
	// the dedicated throttling tests further down build their own
	// AuthService/RateLimiterService with a deliberately tight limit instead.
	private final RateLimiterService rateLimiterService =
			new RateLimiterService(true, 100, 100, 100, 100, 100, new RateLimitMetrics(new SimpleMeterRegistry()));

	private AuthService authService;

	@BeforeEach
	void setUp() {
		// Constructed here, not as a field initializer: @Mock fields are injected by
		// MockitoExtension after the test instance is created, so building authService
		// as a field initializer would capture a still-null userRepository.
		authService = new AuthService(userRepository, passwordEncoder, jwtService, tokenRevocationService,
				rateLimiterService);
	}

	@Test
	void registersANewUserWithAHashedPasswordAndReturnsAToken() {
		when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.empty());
		when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
			User user = invocation.getArgument(0);
			user.setId("user-1");
			return user;
		});

		AuthResponse response = authService.register("jane@example.com", "correct-horse");

		assertThat(response.token()).isNotBlank();
		assertThat(jwtService.validateAndGetClaims(response.token()).map(JwtClaims::userId)).contains("user-1");

		ArgumentCaptor<User> savedUser = ArgumentCaptor.forClass(User.class);
		verify(userRepository).save(savedUser.capture());
		assertThat(savedUser.getValue().getEmail()).isEqualTo("jane@example.com");
		assertThat(savedUser.getValue().getPasswordHash()).isNotEqualTo("correct-horse");
		assertThat(passwordEncoder.matches("correct-horse", savedUser.getValue().getPasswordHash())).isTrue();
	}

	@Test
	void rejectsRegistrationWhenTheEmailIsAlreadyTaken() {
		User existing = new User();
		existing.setEmail("jane@example.com");
		when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(existing));

		assertThatThrownBy(() -> authService.register("jane@example.com", "correct-horse"))
				.isInstanceOf(DuplicateEmailException.class);
	}

	@Test
	void logsInWithCorrectCredentialsAndReturnsAToken() {
		User user = new User();
		user.setId("user-1");
		user.setEmail("jane@example.com");
		user.setPasswordHash(passwordEncoder.encode("correct-horse"));
		when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(user));

		AuthResponse response = authService.login("jane@example.com", "correct-horse");

		assertThat(jwtService.validateAndGetClaims(response.token()).map(JwtClaims::userId)).contains("user-1");
	}

	@Test
	void rejectsLoginWithTheWrongPassword() {
		User user = new User();
		user.setId("user-1");
		user.setEmail("jane@example.com");
		user.setPasswordHash(passwordEncoder.encode("correct-horse"));
		when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(user));

		assertThatThrownBy(() -> authService.login("jane@example.com", "wrong-password"))
				.isInstanceOf(InvalidCredentialsException.class);
	}

	@Test
	void rejectsLoginWithAnUnknownEmailUsingTheSameErrorAsAWrongPassword() {
		when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> authService.login("nobody@example.com", "anything"))
				.isInstanceOf(InvalidCredentialsException.class)
				.hasMessage("Invalid email or password");
	}

	@Test
	void logoutRevokesTheTokenSoItIsNoLongerValid() {
		String token = jwtService.generateToken("user-1");
		JwtClaims claims = jwtService.validateAndGetClaims(token).orElseThrow();
		assertThat(tokenRevocationService.isRevoked(claims.tokenId())).isFalse();

		authService.logout(claims);

		assertThat(tokenRevocationService.isRevoked(claims.tokenId())).isTrue();
	}

	@Test
	void exceedingThePerAccountLoginBudgetThrowsRateLimitExceeded() {
		AuthService throttledAuthService = authServiceWithLoginBudget(1);
		when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.empty());

		// First attempt consumes the one-request budget - fails normally (unknown
		// email), not because of throttling.
		assertThatThrownBy(() -> throttledAuthService.login("jane@example.com", "anything"))
				.isInstanceOf(InvalidCredentialsException.class);

		// Second attempt against the same account, immediately after: the budget is
		// now exhausted, so this must be throttled before it ever reaches the
		// repository lookup - not just fail with the same "invalid credentials".
		assertThatThrownBy(() -> throttledAuthService.login("jane@example.com", "anything"))
				.isInstanceOf(RateLimitExceededException.class);
	}

	@Test
	void differentAccountsHaveIndependentPerAccountLoginBudgets() {
		AuthService throttledAuthService = authServiceWithLoginBudget(1);
		when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.empty());
		when(userRepository.findByEmail("nobody-else@example.com")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> throttledAuthService.login("jane@example.com", "anything"))
				.isInstanceOf(InvalidCredentialsException.class);

		// A different account's budget is untouched by the first account's use of
		// its own budget.
		assertThatThrownBy(() -> throttledAuthService.login("nobody-else@example.com", "anything"))
				.isInstanceOf(InvalidCredentialsException.class);
	}

	@Test
	void caseVariantEmailsShareTheSamePerAccountLoginBudget() {
		AuthService throttledAuthService = authServiceWithLoginBudget(1);
		when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> throttledAuthService.login("jane@example.com", "anything"))
				.isInstanceOf(InvalidCredentialsException.class);

		// Same account, different capitalization - must not be treated as a fresh
		// budget, or the throttle would be trivially bypassable.
		assertThatThrownBy(() -> throttledAuthService.login("Jane@Example.com", "anything"))
				.isInstanceOf(RateLimitExceededException.class);
	}

	private AuthService authServiceWithLoginBudget(int perAccountRequestsPerMinute) {
		RateLimiterService throttledRateLimiterService = new RateLimiterService(true, 100, 100, 100, 100,
				perAccountRequestsPerMinute, new RateLimitMetrics(new SimpleMeterRegistry()));
		return new AuthService(userRepository, passwordEncoder, jwtService, tokenRevocationService,
				throttledRateLimiterService);
	}

}

package com.resumematcher.backend.security;

import java.util.Locale;

import com.resumematcher.backend.dto.AuthResponse;
import com.resumematcher.backend.entity.User;
import com.resumematcher.backend.ratelimit.RateLimitDecision;
import com.resumematcher.backend.ratelimit.RateLimiterService;
import com.resumematcher.backend.ratelimit.RateLimitTier;
import com.resumematcher.backend.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

	private static final String ACCOUNT_KEY_PREFIX = "account:";

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;
	private final TokenRevocationService tokenRevocationService;
	private final RateLimiterService rateLimiterService;

	public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService,
			TokenRevocationService tokenRevocationService, RateLimiterService rateLimiterService) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
		this.tokenRevocationService = tokenRevocationService;
		this.rateLimiterService = rateLimiterService;
	}

	public AuthResponse register(String email, String password) {
		if (userRepository.findByEmail(email).isPresent()) {
			throw new DuplicateEmailException("An account with this email already exists");
		}

		User user = new User();
		user.setEmail(email);
		user.setPasswordHash(passwordEncoder.encode(password));
		User saved = userRepository.save(user);

		return new AuthResponse(jwtService.generateToken(saved.getId()));
	}

	public AuthResponse login(String email, String password) {
		// Runs before any DB lookup or password hashing, and independently of the
		// existing per-IP AuthRateLimitingFilter budget: that one stops a single
		// source from flooding any account, this one stops many sources (e.g.
		// distributed across IPs) from flooding one specific account. Keyed on the
		// normalized email rather than the (not-yet-looked-up) user id so a request
		// against an unknown email is throttled identically to one against a real
		// account - never leaking which is which.
		String accountKey = ACCOUNT_KEY_PREFIX + email.trim().toLowerCase(Locale.ROOT);
		RateLimitDecision decision = rateLimiterService.tryConsume(accountKey, RateLimitTier.AUTH_LOGIN_PER_ACCOUNT);
		if (!decision.allowed()) {
			throw new RateLimitExceededException(decision.retryAfterSeconds());
		}

		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

		// Same error message and code path as the "user not found" case above -
		// distinguishing them in the response would let a caller enumerate which
		// emails have accounts.
		if (!passwordEncoder.matches(password, user.getPasswordHash())) {
			throw new InvalidCredentialsException("Invalid email or password");
		}

		return new AuthResponse(jwtService.generateToken(user.getId()));
	}

	public void logout(JwtClaims claims) {
		tokenRevocationService.revoke(claims.tokenId(), claims.expiresAt());
	}

}

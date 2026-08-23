package com.resumematcher.backend.security;

import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Reads the authenticated user id off the SecurityContext. Unused by any
 * ownership check yet (Phase 2) - this only exists so that infrastructure is
 * ready and independently testable ahead of time.
 */
@Component
public class CurrentUserProvider {

	public Optional<String> getCurrentUserId() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated()) {
			return Optional.empty();
		}

		Object principal = authentication.getPrincipal();
		return principal instanceof String userId ? Optional.of(userId) : Optional.empty();
	}

}

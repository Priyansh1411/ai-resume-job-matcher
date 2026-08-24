package com.resumematcher.backend.security;

import java.io.IOException;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

/**
 * Replaces Spring Security's default access-denied handling so a
 * filter-chain-level denial (authenticated, but not permitted) gets this
 * app's usual {@code { "error": ... }} JSON shape on 403, matching what
 * {@link JsonAuthenticationEntryPoint} does for the 401 case. Not currently
 * reachable through real traffic - authorization rules in
 * {@link SecurityConfiguration} only ever check {@code authenticated()}, never
 * a role/authority - but configured now for a consistent security posture and
 * so any future authority-based rule doesn't silently fall back to the
 * framework's raw error page.
 */
public class JsonAccessDeniedHandler implements AccessDeniedHandler {

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Override
	public void handle(HttpServletRequest request, HttpServletResponse response,
			AccessDeniedException accessDeniedException) throws IOException {
		response.setStatus(HttpStatus.FORBIDDEN.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.getWriter().write(
				objectMapper.writeValueAsString(Map.of("error", "You do not have permission to access this resource")));
	}

}

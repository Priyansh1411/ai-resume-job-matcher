package com.resumematcher.backend.security;

import java.io.IOException;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

/**
 * Replaces Spring Security's default {@code Http403ForbiddenEntryPoint} so a
 * missing, malformed, or expired token gets the same {@code { "error": ... }}
 * JSON shape every other error response in this app already uses, on 401 -
 * not the framework's raw error page, and not 403, which this app reserves
 * for "authenticated but not allowed" (see {@link ResumeAccessDeniedException}
 * and {@link JsonAccessDeniedHandler}).
 */
public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException authException) throws IOException {
		response.setStatus(HttpStatus.UNAUTHORIZED.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.getWriter().write(
				objectMapper.writeValueAsString(Map.of("error", "Authentication is required to access this resource")));
	}

}

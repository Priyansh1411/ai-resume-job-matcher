package com.resumematcher.backend.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestCorrelationFilterTest {

	private static final Pattern UUID_PATTERN = Pattern.compile(
			"^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

	private final RequestCorrelationFilter filter = new RequestCorrelationFilter();

	@Test
	void generatesAUuidWhenTheHeaderIsMissing() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/health");
		MockHttpServletResponse response = new MockHttpServletResponse();
		AtomicReference<String> mdcDuringChain = new AtomicReference<>();

		filter.doFilter(request, response,
				(req, res) -> mdcDuringChain.set(MDC.get(RequestCorrelationFilter.REQUEST_ID_MDC_KEY)));

		assertThat(mdcDuringChain.get()).matches(UUID_PATTERN);
		assertThat(response.getHeader(RequestCorrelationFilter.REQUEST_ID_HEADER)).isEqualTo(mdcDuringChain.get());
	}

	@Test
	void reusesAValidIncomingUuid() throws Exception {
		String incomingId = UUID.randomUUID().toString();
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/health");
		request.addHeader(RequestCorrelationFilter.REQUEST_ID_HEADER, incomingId);
		MockHttpServletResponse response = new MockHttpServletResponse();
		AtomicReference<String> mdcDuringChain = new AtomicReference<>();

		filter.doFilter(request, response,
				(req, res) -> mdcDuringChain.set(MDC.get(RequestCorrelationFilter.REQUEST_ID_MDC_KEY)));

		assertThat(mdcDuringChain.get()).isEqualTo(incomingId);
		assertThat(response.getHeader(RequestCorrelationFilter.REQUEST_ID_HEADER)).isEqualTo(incomingId);
	}

	@Test
	void generatesANewUuidWhenTheIncomingHeaderIsNotAValidUuid() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/health");
		request.addHeader(RequestCorrelationFilter.REQUEST_ID_HEADER, "not-a-uuid");
		MockHttpServletResponse response = new MockHttpServletResponse();
		AtomicReference<String> mdcDuringChain = new AtomicReference<>();

		filter.doFilter(request, response,
				(req, res) -> mdcDuringChain.set(MDC.get(RequestCorrelationFilter.REQUEST_ID_MDC_KEY)));

		assertThat(mdcDuringChain.get()).matches(UUID_PATTERN);
		assertThat(mdcDuringChain.get()).isNotEqualTo("not-a-uuid");
	}

	// Sensitive-logging guard: an attacker stuffing credential-shaped text into
	// this header must never reach the MDC (and therefore the logs) or the
	// response header verbatim - only a value this filter generated or validated
	// as a real UUID is ever surfaced.
	@Test
	void neverLetsAttackerSuppliedHeaderContentReachMdcOrTheResponseHeader() throws Exception {
		String maliciousHeader = "password=hunter2; Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.fake.token";
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/health");
		request.addHeader(RequestCorrelationFilter.REQUEST_ID_HEADER, maliciousHeader);
		MockHttpServletResponse response = new MockHttpServletResponse();
		AtomicReference<String> mdcDuringChain = new AtomicReference<>();

		filter.doFilter(request, response,
				(req, res) -> mdcDuringChain.set(MDC.get(RequestCorrelationFilter.REQUEST_ID_MDC_KEY)));

		assertThat(mdcDuringChain.get()).matches(UUID_PATTERN);
		assertThat(response.getHeader(RequestCorrelationFilter.REQUEST_ID_HEADER)).matches(UUID_PATTERN);
	}

	@Test
	void clearsMdcAfterTheRequestCompletes() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/health");
		MockHttpServletResponse response = new MockHttpServletResponse();

		filter.doFilter(request, response, (req, res) -> { });

		assertThat(MDC.get(RequestCorrelationFilter.REQUEST_ID_MDC_KEY)).isNull();
	}

	// Guards against MDC leaking onto the next, unrelated request handled by the
	// same reused Tomcat thread if the downstream chain fails.
	@Test
	void clearsMdcEvenWhenTheDownstreamChainThrows() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/health");
		MockHttpServletResponse response = new MockHttpServletResponse();

		assertThatThrownBy(() -> filter.doFilter(request, response, (req, res) -> {
			throw new IllegalStateException("downstream failure");
		})).isInstanceOf(IllegalStateException.class);

		assertThat(MDC.get(RequestCorrelationFilter.REQUEST_ID_MDC_KEY)).isNull();
	}

	// Simulates what JsonAuthenticationEntryPoint/JsonAccessDeniedHandler and the
	// rate-limit filters do downstream: set an error status and body, but never
	// touch headers set earlier in the chain - proving the correlation header
	// survives on 401/403/404/429/500 responses, not just 200s.
	@Test
	void setsTheResponseHeaderBeforeDownstreamProcessingSoItSurvivesAnErrorResponse() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/resumes/upload");
		MockHttpServletResponse response = new MockHttpServletResponse();

		filter.doFilter(request, response, (req, res) -> ((MockHttpServletResponse) res).setStatus(401));

		assertThat(response.getStatus()).isEqualTo(401);
		assertThat(response.getHeader(RequestCorrelationFilter.REQUEST_ID_HEADER)).matches(UUID_PATTERN);
	}

}

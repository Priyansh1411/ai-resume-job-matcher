package com.resumematcher.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumematcher.backend.testsupport.AbstractMySqlIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.TestPropertySource;

/**
 * Covers the one {@link GlobalExceptionHandler} case
 * {@link GlobalExceptionHandlerIntegrationTest}'s MockMvc-based tests can't
 * reproduce: an oversized multipart upload. Verified empirically during
 * implementation that MockMvc's {@code MockMultipartFile} bypasses Spring's
 * real container-level multipart size enforcement entirely - even with
 * {@code spring.servlet.multipart.max-file-size} lowered to 1KB and a
 * 2000-byte file, a MockMvc-driven request still succeeded with 201,
 * completely ignoring the property, because MockMvc attaches the mock file
 * directly rather than re-parsing it through the same pipeline a real request
 * body goes through.
 *
 * <p>This class instead sends a real HTTP request over a real socket to a
 * real embedded server ({@code RANDOM_PORT}), which does correctly enforce
 * it. The limit is lowered to 1KB for this class specifically (rather than
 * reusing the real 10MB production limit) so the request body is small enough
 * to write in a single instant write - at the real 10MB boundary, writing the
 * body takes long enough for the server to reject and close the connection
 * mid-write, which Java's HttpClient surfaces as a hard IOException
 * ("Broken pipe") instead of a clean response; this was reproduced directly
 * while implementing this test, across three different request-encoding
 * strategies (plain Content-Length, Expect: 100-continue, chunked transfer).
 * A small body sidesteps the race entirely rather than working around it. The
 * production 10MB boundary itself was confirmed manually via curl during
 * planning (succeeds at 10,400,000 bytes, fails at exactly 10*1024*1024) but
 * isn't separately covered by any automated test in this codebase - what this
 * test verifies is narrower and doesn't depend on the exact configured size:
 * that {@link org.springframework.web.multipart.MaxUploadSizeExceededException}
 * itself produces this app's consistent JSON body instead of an empty one,
 * whatever the limit is set to.
 *
 * <p>Uses the JDK's built-in {@code java.net.http.HttpClient} rather than
 * Spring Boot's {@code TestRestTemplate}: that class now lives in the
 * separate {@code spring-boot-resttestclient} module (Spring Boot 4's test
 * infrastructure was split into several new modules), and wiring it up
 * requires {@code spring-boot-restclient} too (for {@code RestTemplateBuilder})
 * - confirmed by trying it first and hitting a {@code ClassNotFoundException}
 * for exactly that class. Neither is currently a project dependency, and
 * adding one is out of this phase's approved scope, so this uses what's
 * already on the classpath (the JDK itself) instead.
 *
 * <p>Note: unlike the MockMvc-based tests elsewhere in this package,
 * {@code @Transactional} can't roll back what happens here - a real HTTP
 * request runs on the server's own thread, outside the test's transaction.
 * The one row this creates (a throwaway registered user, via a
 * uniquely-generated email) is harmless and is discarded along with the rest
 * of the ephemeral Testcontainers database when the suite finishes.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = "spring.servlet.multipart.max-file-size=1KB")
class GlobalExceptionHandlerRealHttpIntegrationTest extends AbstractMySqlIntegrationTest {

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	@LocalServerPort
	private int port;

	private final HttpClient httpClient = HttpClient.newHttpClient();

	@Test
	void oversizedUploadReturnsPayloadTooLargeWithConsistentBody() throws Exception {
		String authHeader = registerAndGetAuthorizationHeader();
		String boundary = "GlobalExceptionHandlerTestBoundary" + UUID.randomUUID();

		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create("http://localhost:" + port + "/api/resumes/upload"))
				.header("Authorization", authHeader)
				.header("Content-Type", "multipart/form-data; boundary=" + boundary)
				.POST(HttpRequest.BodyPublishers.ofByteArray(oversizedFileMultipartBody(boundary)))
				.build();

		HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

		assertThat(response.statusCode()).isEqualTo(413);
		JsonNode body = OBJECT_MAPPER.readTree(response.body());
		assertThat(body.get("error").asText()).isEqualTo("Uploaded file exceeds the maximum allowed size");
	}

	private byte[] oversizedFileMultipartBody(String boundary) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		String partHeader = "--" + boundary + "\r\n"
				+ "Content-Disposition: form-data; name=\"file\"; filename=\"big.pdf\"\r\n"
				+ "Content-Type: application/pdf\r\n\r\n";
		out.write(partHeader.getBytes(StandardCharsets.US_ASCII));
		// One byte over this test class's 1KB spring.servlet.multipart.max-file-size.
		out.write(new byte[1025]);
		out.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.US_ASCII));
		return out.toByteArray();
	}

	private String registerAndGetAuthorizationHeader() throws Exception {
		String email = "real-http-test-" + UUID.randomUUID() + "@example.com";
		String requestBody = "{\"email\":\"" + email + "\",\"password\":\"correct-horse-battery-staple\"}";

		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create("http://localhost:" + port + "/api/auth/register"))
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(requestBody))
				.build();

		HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
		JsonNode json = OBJECT_MAPPER.readTree(response.body());
		return "Bearer " + json.get("token").asText();
	}

}

package com.resumematcher.backend.analysis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OpenAiChatClientTest {

	private HttpServer server;
	private String baseUrl;
	private volatile String lastRequestBody;

	@BeforeEach
	void startLocalServer() throws IOException {
		server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.start();
		baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
	}

	@AfterEach
	void stopLocalServer() {
		server.stop(0);
	}

	@Test
	void returnsTheMessageContentFromASuccessfulResponse() throws IOException {
		respondWith(200, "{\"choices\":[{\"message\":{\"content\":\"{\\\"strengths\\\":[]}\"}}]}");

		OpenAiChatClient client = new OpenAiChatClient(baseUrl, "test-key", "test-model");

		String content = client.complete("system prompt", "user prompt");

		assertThat(content).isEqualTo("{\"strengths\":[]}");
		assertThat(lastRequestBody).contains("\"role\":\"system\"", "\"content\":\"system prompt\"",
				"\"role\":\"user\"", "\"content\":\"user prompt\"");
	}

	@Test
	void throwsAnalysisUnavailableExceptionOnNonSuccessStatus() throws IOException {
		respondWith(500, "internal error");

		OpenAiChatClient client = new OpenAiChatClient(baseUrl, "test-key", "test-model");

		assertThatThrownBy(() -> client.complete("system prompt", "user prompt"))
				.isInstanceOf(AnalysisUnavailableException.class);
	}

	@Test
	void throwsAnalysisUnavailableExceptionOnMalformedJsonResponse() throws IOException {
		respondWith(200, "this is not json");

		OpenAiChatClient client = new OpenAiChatClient(baseUrl, "test-key", "test-model");

		assertThatThrownBy(() -> client.complete("system prompt", "user prompt"))
				.isInstanceOf(AnalysisUnavailableException.class);
	}

	@Test
	void throwsAnalysisUnavailableExceptionWhenResponseHasNoMessageContent() throws IOException {
		respondWith(200, "{\"choices\":[]}");

		OpenAiChatClient client = new OpenAiChatClient(baseUrl, "test-key", "test-model");

		assertThatThrownBy(() -> client.complete("system prompt", "user prompt"))
				.isInstanceOf(AnalysisUnavailableException.class);
	}

	@Test
	void throwsAnalysisUnavailableExceptionWhenBaseUrlIsNotConfigured() {
		OpenAiChatClient client = new OpenAiChatClient("", "test-key", "test-model");

		assertThatThrownBy(() -> client.complete("system prompt", "user prompt"))
				.isInstanceOf(AnalysisUnavailableException.class);
	}

	private void respondWith(int status, String body) throws IOException {
		server.createContext("/chat/completions", exchange -> {
			lastRequestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
			byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().add("Content-Type", "application/json");
			exchange.sendResponseHeaders(status, bytes.length);
			try (OutputStream outputStream = exchange.getResponseBody()) {
				outputStream.write(bytes);
			}
		});
	}

}

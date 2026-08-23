package com.resumematcher.backend.matching;

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

class OpenAiCompatibleEmbeddingClientTest {

	private HttpServer server;
	private String baseUrl;

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
	void parsesTheEmbeddingFromASuccessfulResponse() throws IOException {
		respondWith(200, "{\"data\":[{\"embedding\":[0.1,0.2,0.3]}]}");

		OpenAiCompatibleEmbeddingClient client =
				new OpenAiCompatibleEmbeddingClient(baseUrl, "test-key", "test-model");

		float[] embedding = client.embed("sample text");

		assertThat(embedding).containsExactly(0.1f, 0.2f, 0.3f);
	}

	@Test
	void throwsEmbeddingExceptionOnNonSuccessStatus() throws IOException {
		respondWith(500, "internal error");

		OpenAiCompatibleEmbeddingClient client =
				new OpenAiCompatibleEmbeddingClient(baseUrl, "test-key", "test-model");

		assertThatThrownBy(() -> client.embed("sample text"))
				.isInstanceOf(EmbeddingException.class);
	}

	@Test
	void throwsEmbeddingExceptionOnMalformedJsonResponse() throws IOException {
		respondWith(200, "this is not json");

		OpenAiCompatibleEmbeddingClient client =
				new OpenAiCompatibleEmbeddingClient(baseUrl, "test-key", "test-model");

		assertThatThrownBy(() -> client.embed("sample text"))
				.isInstanceOf(EmbeddingException.class);
	}

	@Test
	void throwsEmbeddingExceptionWhenResponseHasNoEmbeddingArray() throws IOException {
		respondWith(200, "{\"data\":[]}");

		OpenAiCompatibleEmbeddingClient client =
				new OpenAiCompatibleEmbeddingClient(baseUrl, "test-key", "test-model");

		assertThatThrownBy(() -> client.embed("sample text"))
				.isInstanceOf(EmbeddingException.class);
	}

	@Test
	void throwsEmbeddingExceptionWhenBaseUrlIsNotConfigured() {
		OpenAiCompatibleEmbeddingClient client = new OpenAiCompatibleEmbeddingClient("", "test-key", "test-model");

		assertThatThrownBy(() -> client.embed("sample text"))
				.isInstanceOf(EmbeddingException.class);
	}

	private void respondWith(int status, String body) throws IOException {
		server.createContext("/embeddings", exchange -> {
			byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().add("Content-Type", "application/json");
			exchange.sendResponseHeaders(status, bytes.length);
			try (OutputStream outputStream = exchange.getResponseBody()) {
				outputStream.write(bytes);
			}
		});
	}

}

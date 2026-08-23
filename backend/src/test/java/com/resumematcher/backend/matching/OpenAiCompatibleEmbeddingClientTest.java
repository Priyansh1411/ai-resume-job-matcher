package com.resumematcher.backend.matching;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OpenAiCompatibleEmbeddingClientTest {

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

	@Test
	void parsesEmbeddingsFromABatchedResponseInRequestOrder() throws IOException {
		respondWith(200, "{\"data\":[{\"index\":0,\"embedding\":[0.1,0.2]},{\"index\":1,\"embedding\":[0.3,0.4]}]}");

		OpenAiCompatibleEmbeddingClient client =
				new OpenAiCompatibleEmbeddingClient(baseUrl, "test-key", "test-model");

		List<float[]> embeddings = client.embed(List.of("first text", "second text"));

		assertThat(embeddings.get(0)).containsExactly(0.1f, 0.2f);
		assertThat(embeddings.get(1)).containsExactly(0.3f, 0.4f);
		assertThat(lastRequestBody).contains("\"input\":[\"first text\",\"second text\"]");
	}

	@Test
	void reordersBatchedEmbeddingsUsingTheIndexFieldRatherThanArrayPosition() throws IOException {
		respondWith(200, "{\"data\":[{\"index\":1,\"embedding\":[0.3,0.4]},{\"index\":0,\"embedding\":[0.1,0.2]}]}");

		OpenAiCompatibleEmbeddingClient client =
				new OpenAiCompatibleEmbeddingClient(baseUrl, "test-key", "test-model");

		List<float[]> embeddings = client.embed(List.of("first text", "second text"));

		assertThat(embeddings.get(0)).containsExactly(0.1f, 0.2f);
		assertThat(embeddings.get(1)).containsExactly(0.3f, 0.4f);
	}

	@Test
	void throwsEmbeddingExceptionWhenBatchResponseSizeDoesNotMatchRequestSize() throws IOException {
		respondWith(200, "{\"data\":[{\"index\":0,\"embedding\":[0.1,0.2]}]}");

		OpenAiCompatibleEmbeddingClient client =
				new OpenAiCompatibleEmbeddingClient(baseUrl, "test-key", "test-model");

		assertThatThrownBy(() -> client.embed(List.of("first text", "second text")))
				.isInstanceOf(EmbeddingException.class);
	}

	@Test
	void throwsEmbeddingExceptionWhenBatchResponseHasADuplicateIndex() throws IOException {
		respondWith(200, "{\"data\":[{\"index\":0,\"embedding\":[0.1,0.2]},{\"index\":0,\"embedding\":[0.3,0.4]}]}");

		OpenAiCompatibleEmbeddingClient client =
				new OpenAiCompatibleEmbeddingClient(baseUrl, "test-key", "test-model");

		assertThatThrownBy(() -> client.embed(List.of("first text", "second text")))
				.isInstanceOf(EmbeddingException.class);
	}

	private void respondWith(int status, String body) throws IOException {
		server.createContext("/embeddings", exchange -> {
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

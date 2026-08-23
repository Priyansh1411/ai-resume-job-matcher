package com.resumematcher.backend.matching;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Generic {@link EmbeddingClient} for any provider that exposes an
 * OpenAI-compatible "/embeddings" REST endpoint (request: {"model", "input"},
 * response: {"data": [{"embedding": [...]}]}). Endpoint, key, and model are
 * entirely configuration-driven so no provider-specific logic lives here or
 * in any matcher that depends on this interface.
 */
@Component
public class OpenAiCompatibleEmbeddingClient implements EmbeddingClient {

	private final String baseUrl;
	private final String apiKey;
	private final String model;
	private final HttpClient httpClient;
	private final ObjectMapper objectMapper;

	public OpenAiCompatibleEmbeddingClient(
			@Value("${embedding.api.base-url:}") String baseUrl,
			@Value("${embedding.api.key:}") String apiKey,
			@Value("${embedding.api.model:text-embedding-3-small}") String model) {
		this.baseUrl = baseUrl;
		this.apiKey = apiKey;
		this.model = model;
		this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
		this.objectMapper = new ObjectMapper();
	}

	@Override
	public float[] embed(String text) {
		if (baseUrl == null || baseUrl.isBlank()) {
			throw new EmbeddingException("Embedding API base URL is not configured");
		}

		HttpResponse<String> response;
		try {
			String requestJson = objectMapper.writeValueAsString(Map.of("model", model, "input", text));

			HttpRequest request = HttpRequest.newBuilder()
					.uri(URI.create(baseUrl + "/embeddings"))
					.header("Content-Type", "application/json")
					.header("Authorization", "Bearer " + apiKey)
					.timeout(Duration.ofSeconds(10))
					.POST(HttpRequest.BodyPublishers.ofString(requestJson))
					.build();

			response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
		} catch (IOException e) {
			throw new EmbeddingException("Failed to call embedding API", e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new EmbeddingException("Embedding request was interrupted", e);
		}

		if (response.statusCode() != 200) {
			throw new EmbeddingException("Embedding API returned status " + response.statusCode());
		}

		return parseEmbedding(response.body());
	}

	private float[] parseEmbedding(String responseBody) {
		JsonNode embeddingNode;
		try {
			JsonNode root = objectMapper.readTree(responseBody);
			embeddingNode = root.at("/data/0/embedding");
		} catch (IOException e) {
			throw new EmbeddingException("Embedding API returned an unparseable response", e);
		}

		if (!embeddingNode.isArray() || embeddingNode.isEmpty()) {
			throw new EmbeddingException("Embedding API response did not contain an embedding array");
		}

		float[] embedding = new float[embeddingNode.size()];
		for (int i = 0; i < embeddingNode.size(); i++) {
			embedding[i] = (float) embeddingNode.get(i).asDouble();
		}
		return embedding;
	}

}

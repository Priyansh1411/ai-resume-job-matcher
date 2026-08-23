package com.resumematcher.backend.matching;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumematcher.backend.observability.OpenAiMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Generic {@link EmbeddingClient} for any provider that exposes an
 * OpenAI-compatible "/embeddings" REST endpoint (request: {"model", "input"},
 * response: {"data": [{"embedding": [...], "index": ...}]}). Endpoint, key, and
 * model are entirely configuration-driven so no provider-specific logic lives here
 * or in any matcher that depends on this interface.
 */
@Component
public class OpenAiCompatibleEmbeddingClient implements EmbeddingClient {

	private static final Logger log = LoggerFactory.getLogger(OpenAiCompatibleEmbeddingClient.class);

	private final String baseUrl;
	private final String apiKey;
	private final String model;
	private final HttpClient httpClient;
	private final ObjectMapper objectMapper;
	private final OpenAiMetrics openAiMetrics;

	public OpenAiCompatibleEmbeddingClient(
			@Value("${embedding.api.base-url:}") String baseUrl,
			@Value("${embedding.api.key:}") String apiKey,
			@Value("${embedding.api.model:text-embedding-3-small}") String model,
			OpenAiMetrics openAiMetrics) {
		this.baseUrl = baseUrl;
		this.apiKey = apiKey;
		this.model = model;
		this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
		this.objectMapper = new ObjectMapper();
		this.openAiMetrics = openAiMetrics;
	}

	@Override
	public float[] embed(String text) {
		JsonNode dataNode = parseDataArray(requestEmbeddings(text));
		if (dataNode.isEmpty()) {
			throw new EmbeddingException("Embedding API response did not contain an embedding array");
		}
		return toVector(dataNode.get(0).path("embedding"));
	}

	@Override
	public List<float[]> embed(List<String> texts) {
		JsonNode dataNode = parseDataArray(requestEmbeddings(texts));
		if (dataNode.size() != texts.size()) {
			throw new EmbeddingException(
					"Embedding API returned " + dataNode.size() + " embeddings for " + texts.size() + " inputs");
		}

		// The API doesn't guarantee response order matches request order, so each
		// entry is placed by its own "index" field rather than array position.
		float[][] embeddings = new float[texts.size()][];
		for (JsonNode entry : dataNode) {
			int index = entry.path("index").asInt(-1);
			if (index < 0 || index >= embeddings.length || embeddings[index] != null) {
				throw new EmbeddingException("Embedding API response contained an invalid or duplicate index");
			}
			embeddings[index] = toVector(entry.path("embedding"));
		}

		return new ArrayList<>(List.of(embeddings));
	}

	private String requestEmbeddings(Object input) {
		Instant start = Instant.now();
		try {
			String result = callEmbeddingApi(input);
			openAiMetrics.recordEmbeddingCall(Duration.between(start, Instant.now()), true);
			return result;
		} catch (EmbeddingException e) {
			openAiMetrics.recordEmbeddingCall(Duration.between(start, Instant.now()), false);
			log.warn("OpenAI embedding call failed. Reason: {}", e.getMessage());
			throw e;
		}
	}

	private String callEmbeddingApi(Object input) {
		if (baseUrl == null || baseUrl.isBlank()) {
			throw new EmbeddingException("Embedding API base URL is not configured");
		}

		HttpResponse<String> response;
		try {
			String requestJson = objectMapper.writeValueAsString(Map.of("model", model, "input", input));

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

		return response.body();
	}

	private JsonNode parseDataArray(String responseBody) {
		JsonNode dataNode;
		try {
			dataNode = objectMapper.readTree(responseBody).path("data");
		} catch (IOException e) {
			throw new EmbeddingException("Embedding API returned an unparseable response", e);
		}

		if (!dataNode.isArray()) {
			throw new EmbeddingException("Embedding API response did not contain an embedding array");
		}
		return dataNode;
	}

	private float[] toVector(JsonNode embeddingNode) {
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

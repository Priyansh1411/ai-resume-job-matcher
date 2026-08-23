package com.resumematcher.backend.analysis;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
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
 * Thin wrapper around an OpenAI-compatible "/chat/completions" endpoint. Endpoint,
 * key, and model are entirely configuration-driven, same convention as the
 * embedding client in the matching package.
 */
@Component
public class OpenAiChatClient {

	private static final Logger log = LoggerFactory.getLogger(OpenAiChatClient.class);

	private final String baseUrl;
	private final String apiKey;
	private final String model;
	private final HttpClient httpClient;
	private final ObjectMapper objectMapper;
	private final OpenAiMetrics openAiMetrics;

	public OpenAiChatClient(
			@Value("${chat.api.base-url:}") String baseUrl,
			@Value("${chat.api.key:}") String apiKey,
			@Value("${chat.api.model:gpt-4o-mini}") String model,
			OpenAiMetrics openAiMetrics) {
		this.baseUrl = baseUrl;
		this.apiKey = apiKey;
		this.model = model;
		this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
		this.objectMapper = new ObjectMapper();
		this.openAiMetrics = openAiMetrics;
	}

	public String complete(String systemPrompt, String userPrompt) {
		Instant start = Instant.now();
		try {
			String result = callChatCompletionApi(systemPrompt, userPrompt);
			openAiMetrics.recordChatCompletionCall(Duration.between(start, Instant.now()), true);
			return result;
		} catch (AnalysisUnavailableException e) {
			openAiMetrics.recordChatCompletionCall(Duration.between(start, Instant.now()), false);
			log.warn("OpenAI chat completion call failed. Reason: {}", e.getMessage());
			throw e;
		}
	}

	private String callChatCompletionApi(String systemPrompt, String userPrompt) {
		if (baseUrl == null || baseUrl.isBlank()) {
			throw new AnalysisUnavailableException("Chat completion API base URL is not configured");
		}

		HttpResponse<String> response;
		try {
			Map<String, Object> requestBody = Map.of(
					"model", model,
					"messages", List.of(
							Map.of("role", "system", "content", systemPrompt),
							Map.of("role", "user", "content", userPrompt)
					),
					"response_format", Map.of("type", "json_object")
			);
			String requestJson = objectMapper.writeValueAsString(requestBody);

			HttpRequest request = HttpRequest.newBuilder()
					.uri(URI.create(baseUrl + "/chat/completions"))
					.header("Content-Type", "application/json")
					.header("Authorization", "Bearer " + apiKey)
					.timeout(Duration.ofSeconds(30))
					.POST(HttpRequest.BodyPublishers.ofString(requestJson))
					.build();

			response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
		} catch (IOException e) {
			throw new AnalysisUnavailableException("Failed to call chat completion API", e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new AnalysisUnavailableException("Chat completion request was interrupted", e);
		}

		if (response.statusCode() != 200) {
			throw new AnalysisUnavailableException("Chat completion API returned status " + response.statusCode());
		}

		return extractContent(response.body());
	}

	private String extractContent(String responseBody) {
		JsonNode contentNode;
		try {
			contentNode = objectMapper.readTree(responseBody).at("/choices/0/message/content");
		} catch (IOException e) {
			throw new AnalysisUnavailableException("Chat completion API returned an unparseable response", e);
		}

		if (!contentNode.isTextual() || contentNode.asText().isBlank()) {
			throw new AnalysisUnavailableException("Chat completion API response did not contain a message");
		}
		return contentNode.asText();
	}

}

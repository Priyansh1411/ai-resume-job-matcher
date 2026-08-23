package com.resumematcher.backend.matching;

public interface EmbeddingClient {

	/**
	 * Returns a vector embedding for the given text.
	 *
	 * @throws EmbeddingException if the embedding provider is unreachable, misconfigured,
	 *                            or returns an unusable response.
	 */
	float[] embed(String text);

}

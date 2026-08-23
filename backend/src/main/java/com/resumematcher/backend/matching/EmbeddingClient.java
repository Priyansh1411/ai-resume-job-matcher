package com.resumematcher.backend.matching;

import java.util.ArrayList;
import java.util.List;

public interface EmbeddingClient {

	/**
	 * Returns a vector embedding for the given text.
	 *
	 * @throws EmbeddingException if the embedding provider is unreachable, misconfigured,
	 *                            or returns an unusable response.
	 */
	float[] embed(String text);

	/**
	 * Returns a vector embedding for each text, in the same order, ideally as a single
	 * provider call. Implementations that can't batch may rely on this default, which
	 * costs one {@link #embed(String)} call per text instead.
	 *
	 * @throws EmbeddingException if the embedding provider is unreachable, misconfigured,
	 *                            or returns an unusable response.
	 */
	default List<float[]> embed(List<String> texts) {
		List<float[]> embeddings = new ArrayList<>(texts.size());
		for (String text : texts) {
			embeddings.add(embed(text));
		}
		return embeddings;
	}

}

package com.resumematcher.backend.matching;

public final class CosineSimilarity {

	private CosineSimilarity() {
	}

	public static double compute(float[] a, float[] b) {
		if (a.length != b.length) {
			throw new IllegalArgumentException("Vectors must be the same length");
		}

		double dotProduct = 0;
		double normA = 0;
		double normB = 0;

		for (int i = 0; i < a.length; i++) {
			dotProduct += a[i] * b[i];
			normA += a[i] * a[i];
			normB += b[i] * b[i];
		}

		if (normA == 0 || normB == 0) {
			return 0;
		}

		return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
	}

}

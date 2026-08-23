package com.resumematcher.backend.matching;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;

class CosineSimilarityTest {

	@Test
	void returnsOneForIdenticalVectors() {
		float[] a = { 1f, 2f, 3f };
		float[] b = { 1f, 2f, 3f };

		assertThat(CosineSimilarity.compute(a, b)).isEqualTo(1.0, Offset.offset(0.0001));
	}

	@Test
	void returnsZeroForOrthogonalVectors() {
		float[] a = { 1f, 0f };
		float[] b = { 0f, 1f };

		assertThat(CosineSimilarity.compute(a, b)).isEqualTo(0.0, Offset.offset(0.0001));
	}

	@Test
	void returnsNegativeOneForOppositeVectors() {
		float[] a = { 1f, 0f };
		float[] b = { -1f, 0f };

		assertThat(CosineSimilarity.compute(a, b)).isEqualTo(-1.0, Offset.offset(0.0001));
	}

	@Test
	void returnsZeroWhenEitherVectorIsAllZeros() {
		float[] a = { 0f, 0f };
		float[] b = { 1f, 2f };

		assertThat(CosineSimilarity.compute(a, b)).isZero();
	}

	@Test
	void throwsWhenVectorLengthsDiffer() {
		float[] a = { 1f, 2f };
		float[] b = { 1f, 2f, 3f };

		assertThatThrownBy(() -> CosineSimilarity.compute(a, b))
				.isInstanceOf(IllegalArgumentException.class);
	}

}

package com.resumematcher.backend.matching;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.Set;

import com.resumematcher.backend.profile.SkillDictionary;
import com.resumematcher.backend.profile.SkillKeywordMatcher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EmbeddingResumeJobMatcherTest {

	@Mock
	private EmbeddingClient embeddingClient;

	private final KeywordResumeJobMatcher keywordResumeJobMatcher = new KeywordResumeJobMatcher(
			new JobDescriptionRequirementParser(new SkillKeywordMatcher(new SkillDictionary())));

	@Test
	void scoresUsingCosineSimilarityOfResumeAndJobDescriptionEmbeddings() {
		when(embeddingClient.embed("resume narrative")).thenReturn(new float[] { 1f, 0f });
		when(embeddingClient.embed("job description text")).thenReturn(new float[] { 1f, 0f });

		EmbeddingResumeJobMatcher matcher = new EmbeddingResumeJobMatcher(embeddingClient, keywordResumeJobMatcher);

		MatchResult result = matcher.match(Set.of("java"), "resume narrative", "job description text");

		// Identical vectors => cosine similarity 1.0 => 100%.
		assertThat(result.matchScorePercentage()).isEqualTo(100);
	}

	@Test
	void scoresLowerWhenEmbeddingsAreDissimilar() {
		when(embeddingClient.embed("resume narrative")).thenReturn(new float[] { 1f, 0f });
		when(embeddingClient.embed("job description text")).thenReturn(new float[] { 0f, 1f });

		EmbeddingResumeJobMatcher matcher = new EmbeddingResumeJobMatcher(embeddingClient, keywordResumeJobMatcher);

		MatchResult result = matcher.match(Set.of("java"), "resume narrative", "job description text");

		// Orthogonal vectors => cosine similarity 0.0 => 0%.
		assertThat(result.matchScorePercentage()).isZero();
	}

	@Test
	void preservesTheKeywordBasedSkillBreakdownRegardlessOfSemanticScore() {
		when(embeddingClient.embed("resume narrative")).thenReturn(new float[] { 1f, 0f });
		when(embeddingClient.embed("job description text needs Java and Docker skills")).thenReturn(new float[] { 0f, 1f });

		EmbeddingResumeJobMatcher embeddingMatcher =
				new EmbeddingResumeJobMatcher(embeddingClient, keywordResumeJobMatcher);

		Set<String> resumeSkills = Set.of("java");
		String jobDescriptionText = "job description text needs Java and Docker skills";

		MatchResult embeddingResult = embeddingMatcher.match(resumeSkills, "resume narrative", jobDescriptionText);
		MatchResult keywordResult =
				keywordResumeJobMatcher.match(resumeSkills, "resume narrative", jobDescriptionText);

		// The skill breakdown must be identical to the pure keyword matcher's -
		// only matchScorePercentage is allowed to differ.
		assertThat(embeddingResult.matchedSkills()).isEqualTo(keywordResult.matchedSkills());
		assertThat(embeddingResult.missingSkills()).isEqualTo(keywordResult.missingSkills());
		assertThat(embeddingResult.matchedRequiredSkills()).isEqualTo(keywordResult.matchedRequiredSkills());
		assertThat(embeddingResult.missingRequiredSkills()).isEqualTo(keywordResult.missingRequiredSkills());
		assertThat(embeddingResult.matchedPreferredSkills()).isEqualTo(keywordResult.matchedPreferredSkills());
		assertThat(embeddingResult.missingPreferredSkills()).isEqualTo(keywordResult.missingPreferredSkills());
		assertThat(embeddingResult.matchScorePercentage()).isNotEqualTo(keywordResult.matchScorePercentage());
	}

	@Test
	void propagatesEmbeddingExceptionWhenTheClientFails() {
		when(embeddingClient.embed("resume narrative")).thenThrow(new EmbeddingException("provider unreachable"));

		EmbeddingResumeJobMatcher matcher = new EmbeddingResumeJobMatcher(embeddingClient, keywordResumeJobMatcher);

		assertThatThrownBy(() -> matcher.match(Set.of("java"), "resume narrative", "job description text"))
				.isInstanceOf(EmbeddingException.class);
	}

}

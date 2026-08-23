package com.resumematcher.backend.matching;

import java.util.List;
import java.util.Set;

public class EmbeddingResumeJobMatcher implements ResumeJobMatcher {

	private final EmbeddingClient embeddingClient;
	private final KeywordResumeJobMatcher keywordResumeJobMatcher;

	public EmbeddingResumeJobMatcher(EmbeddingClient embeddingClient,
			KeywordResumeJobMatcher keywordResumeJobMatcher) {
		this.embeddingClient = embeddingClient;
		this.keywordResumeJobMatcher = keywordResumeJobMatcher;
	}

	@Override
	public MatchResult match(Set<String> resumeSkills, String resumeText, String jobDescriptionText) {
		// Skill breakdown always comes from the deterministic keyword matcher;
		// semantic scoring only ever replaces matchScorePercentage below.
		MatchResult keywordResult = keywordResumeJobMatcher.match(resumeSkills, resumeText, jobDescriptionText);

		double similarity;
		try {
			List<float[]> embeddings = embeddingClient.embed(List.of(resumeText, jobDescriptionText));
			similarity = CosineSimilarity.compute(embeddings.get(0), embeddings.get(1));
		} catch (IllegalArgumentException e) {
			// The provider returned embeddings of mismatched dimensionality -
			// treat this the same as any other expected provider failure.
			throw new EmbeddingException("Embedding vectors were not comparable", e);
		}

		int matchScorePercentage = (int) Math.round(Math.max(0, Math.min(1, similarity)) * 100);

		return new MatchResult(
				matchScorePercentage,
				keywordResult.matchedSkills(),
				keywordResult.missingSkills(),
				keywordResult.matchedRequiredSkills(),
				keywordResult.missingRequiredSkills(),
				keywordResult.matchedPreferredSkills(),
				keywordResult.missingPreferredSkills()
		);
	}

}

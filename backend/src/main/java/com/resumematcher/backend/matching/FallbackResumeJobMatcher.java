package com.resumematcher.backend.matching;

import java.util.Set;

import com.resumematcher.backend.observability.OpenAiMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FallbackResumeJobMatcher implements ResumeJobMatcher {

	private static final Logger log = LoggerFactory.getLogger(FallbackResumeJobMatcher.class);

	private final ResumeJobMatcher primaryMatcher;
	private final ResumeJobMatcher fallbackMatcher;
	private final OpenAiMetrics openAiMetrics;

	public FallbackResumeJobMatcher(ResumeJobMatcher primaryMatcher, ResumeJobMatcher fallbackMatcher,
			OpenAiMetrics openAiMetrics) {
		this.primaryMatcher = primaryMatcher;
		this.fallbackMatcher = fallbackMatcher;
		this.openAiMetrics = openAiMetrics;
	}

	@Override
	public MatchResult match(Set<String> resumeSkills, String resumeText, String jobDescriptionText) {
		openAiMetrics.recordSemanticMatchAttempt();
		try {
			return primaryMatcher.match(resumeSkills, resumeText, jobDescriptionText);
		} catch (EmbeddingException e) {
			// Only expected provider/config failures fall back; any other
			// exception is a real bug and must propagate, not be hidden.
			String reason = e.getCause() != null ? e.getCause().getClass().getSimpleName() : "unspecified";
			log.warn("Semantic matching failed, falling back to keyword matching. Reason: {}", e.getMessage());
			openAiMetrics.recordSemanticFallback(reason);
			return fallbackMatcher.match(resumeSkills, resumeText, jobDescriptionText);
		}
	}

}

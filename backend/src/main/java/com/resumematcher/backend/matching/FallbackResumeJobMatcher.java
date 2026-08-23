package com.resumematcher.backend.matching;

import java.util.Set;

public class FallbackResumeJobMatcher implements ResumeJobMatcher {

	private final ResumeJobMatcher primaryMatcher;
	private final ResumeJobMatcher fallbackMatcher;

	public FallbackResumeJobMatcher(ResumeJobMatcher primaryMatcher, ResumeJobMatcher fallbackMatcher) {
		this.primaryMatcher = primaryMatcher;
		this.fallbackMatcher = fallbackMatcher;
	}

	@Override
	public MatchResult match(Set<String> resumeSkills, String resumeText, String jobDescriptionText) {
		try {
			return primaryMatcher.match(resumeSkills, resumeText, jobDescriptionText);
		} catch (EmbeddingException e) {
			// Only expected provider/config failures fall back; any other
			// exception is a real bug and must propagate, not be hidden.
			return fallbackMatcher.match(resumeSkills, resumeText, jobDescriptionText);
		}
	}

}

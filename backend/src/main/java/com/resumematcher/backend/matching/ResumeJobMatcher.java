package com.resumematcher.backend.matching;

import java.util.Set;

public interface ResumeJobMatcher {

	MatchResult match(Set<String> resumeSkills, String resumeText, String jobDescriptionText);

}

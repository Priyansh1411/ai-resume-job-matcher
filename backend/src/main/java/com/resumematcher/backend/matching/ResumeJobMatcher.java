package com.resumematcher.backend.matching;

import java.util.Set;

public interface ResumeJobMatcher {

	MatchResult match(Set<String> resumeSkills, String jobDescriptionText);

}

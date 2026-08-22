package com.resumematcher.backend.profile;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

@Component
public class SkillKeywordMatcher {

	private static final Set<String> KNOWN_SKILLS = Set.of(
			"java", "python", "javascript", "typescript", "c++", "c#", "go", "rust", "kotlin", "swift",
			"spring", "spring boot", "react", "angular", "vue", "node.js", "django", "flask",
			"mysql", "postgresql", "mongodb", "redis", "oracle",
			"docker", "kubernetes", "aws", "azure", "gcp", "terraform", "jenkins", "git", "ci/cd",
			"rest", "graphql", "microservices", "html", "css", "sql",
			"machine learning", "data analysis", "project management", "agile", "scrum",
			"communication", "leadership", "problem solving", "teamwork"
	);

	public Set<String> findSkills(String text) {
		if (text == null) {
			return Set.of();
		}

		String lowerCaseText = text.toLowerCase();
		Set<String> matchedSkills = new LinkedHashSet<>();

		for (String skill : KNOWN_SKILLS) {
			Pattern skillPattern = Pattern.compile("\\b" + Pattern.quote(skill) + "\\b", Pattern.CASE_INSENSITIVE);
			if (skillPattern.matcher(lowerCaseText).find()) {
				matchedSkills.add(skill);
			}
		}

		return matchedSkills;
	}

}

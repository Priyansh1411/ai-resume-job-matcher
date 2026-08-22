package com.resumematcher.backend.profile;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

@Component
public class RuleBasedResumeProfileExtractor implements ResumeProfileExtractor {

	private static final Pattern EMAIL_PATTERN =
			Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");

	private static final Pattern PHONE_PATTERN =
			Pattern.compile("(\\+\\d{1,3}[-.\\s]?)?(\\(\\d{3}\\)|\\d{3})[-.\\s]?\\d{3}[-.\\s]?\\d{4}");

	private static final int MAX_NAME_LINE_LENGTH = 80;

	private static final Set<String> KNOWN_SKILLS = Set.of(
			"java", "python", "javascript", "typescript", "c++", "c#", "go", "rust", "kotlin", "swift",
			"spring", "spring boot", "react", "angular", "vue", "node.js", "django", "flask",
			"mysql", "postgresql", "mongodb", "redis", "oracle",
			"docker", "kubernetes", "aws", "azure", "gcp", "terraform", "jenkins", "git", "ci/cd",
			"rest", "graphql", "microservices", "html", "css", "sql",
			"machine learning", "data analysis", "project management", "agile", "scrum",
			"communication", "leadership", "problem solving", "teamwork"
	);

	@Override
	public ExtractedProfile extract(String resumeText) {
		String email = firstMatch(EMAIL_PATTERN, resumeText);
		String phone = firstMatch(PHONE_PATTERN, resumeText);
		String fullName = guessFullName(resumeText);
		Set<String> skills = findSkills(resumeText);

		return new ExtractedProfile(fullName, email, phone, skills);
	}

	private String firstMatch(Pattern pattern, String text) {
		Matcher matcher = pattern.matcher(text);
		return matcher.find() ? matcher.group().trim() : null;
	}

	private String guessFullName(String text) {
		for (String line : text.split("\\R")) {
			String trimmedLine = line.trim();
			if (trimmedLine.isEmpty()) {
				continue;
			}
			if (trimmedLine.length() > MAX_NAME_LINE_LENGTH || trimmedLine.contains("@")) {
				return null;
			}
			return trimmedLine;
		}
		return null;
	}

	private Set<String> findSkills(String text) {
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

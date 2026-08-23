package com.resumematcher.backend.profile;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

@Component
public class SkillKeywordMatcher {

	private final Map<String, List<Pattern>> aliasPatternsByCanonicalSkill;

	public SkillKeywordMatcher(SkillDictionary skillDictionary) {
		this.aliasPatternsByCanonicalSkill = compilePatterns(skillDictionary.getAliasesByCanonicalSkill());
	}

	public Set<String> findSkills(String text) {
		if (text == null) {
			return Set.of();
		}

		Set<String> matchedSkills = new LinkedHashSet<>();

		for (Map.Entry<String, List<Pattern>> entry : aliasPatternsByCanonicalSkill.entrySet()) {
			for (Pattern aliasPattern : entry.getValue()) {
				if (aliasPattern.matcher(text).find()) {
					matchedSkills.add(entry.getKey());
					break;
				}
			}
		}

		return matchedSkills;
	}

	private Map<String, List<Pattern>> compilePatterns(Map<String, List<String>> aliasesByCanonicalSkill) {
		Map<String, List<Pattern>> compiled = new LinkedHashMap<>();
		for (Map.Entry<String, List<String>> entry : aliasesByCanonicalSkill.entrySet()) {
			List<Pattern> patterns = entry.getValue().stream()
					.map(this::compileAliasPattern)
					.toList();
			compiled.put(entry.getKey(), patterns);
		}
		return compiled;
	}

	private Pattern compileAliasPattern(String alias) {
		// Uses lookaround instead of \b so aliases ending or starting with a
		// non-word character (e.g. "c++", "c#", ".net", "ci/cd") still match
		// correctly; \b only fires on a word/non-word transition and silently
		// never matches those tokens.
		String pattern = "(?<![A-Za-z0-9])" + Pattern.quote(alias) + "(?![A-Za-z0-9])";
		return Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
	}

}

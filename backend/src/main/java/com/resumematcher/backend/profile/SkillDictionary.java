package com.resumematcher.backend.profile;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
public class SkillDictionary {

	private static final String DICTIONARY_RESOURCE_PATH = "skills/skill-dictionary.json";

	private final Map<String, List<String>> aliasesByCanonicalSkill;

	public SkillDictionary() {
		this.aliasesByCanonicalSkill = loadDictionary();
	}

	public Map<String, List<String>> getAliasesByCanonicalSkill() {
		return aliasesByCanonicalSkill;
	}

	private Map<String, List<String>> loadDictionary() {
		ObjectMapper objectMapper = new ObjectMapper();
		try (InputStream inputStream = new ClassPathResource(DICTIONARY_RESOURCE_PATH).getInputStream()) {
			return objectMapper.readValue(inputStream, new TypeReference<LinkedHashMap<String, List<String>>>() {
			});
		} catch (IOException e) {
			throw new IllegalStateException("Failed to load skill dictionary from " + DICTIONARY_RESOURCE_PATH, e);
		}
	}

}

package com.resumematcher.backend.profile;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class SkillDictionaryTest {

	@Test
	void loadsDictionaryFromBundledResource() {
		SkillDictionary dictionary = new SkillDictionary();

		Map<String, List<String>> aliasesByCanonicalSkill = dictionary.getAliasesByCanonicalSkill();

		assertThat(aliasesByCanonicalSkill).isNotEmpty();
		assertThat(aliasesByCanonicalSkill).containsKey("kubernetes");
		assertThat(aliasesByCanonicalSkill.get("kubernetes")).contains("kubernetes", "k8s");
	}

}

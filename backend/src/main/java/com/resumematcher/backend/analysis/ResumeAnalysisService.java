package com.resumematcher.backend.analysis;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumematcher.backend.dto.ResumeAnalysisResponse;
import com.resumematcher.backend.entity.ProcessingStatus;
import com.resumematcher.backend.entity.Resume;
import com.resumematcher.backend.entity.ResumeSkill;
import com.resumematcher.backend.matching.KeywordResumeJobMatcher;
import com.resumematcher.backend.matching.MatchResult;
import com.resumematcher.backend.matching.ResumeNotFoundException;
import com.resumematcher.backend.matching.ResumeNotReadyException;
import com.resumematcher.backend.repository.ResumeRepository;
import com.resumematcher.backend.repository.ResumeSkillRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ResumeAnalysisService {

	private static final String SYSTEM_PROMPT = """
			You are a career coach analyzing how well a resume fits a job description.
			Respond with ONLY a JSON object with exactly these keys: "strengths", "gaps", "suggestions".
			Each key maps to an array of short, specific strings, at most 5 items each.
			Do not include markdown, prose, or any text outside the JSON object.
			""";

	private final boolean analysisEnabled;
	private final ResumeRepository resumeRepository;
	private final ResumeSkillRepository resumeSkillRepository;
	private final KeywordResumeJobMatcher keywordResumeJobMatcher;
	private final OpenAiChatClient chatClient;
	private final ObjectMapper objectMapper = new ObjectMapper();

	public ResumeAnalysisService(
			@Value("${analysis.enabled:false}") boolean analysisEnabled,
			ResumeRepository resumeRepository,
			ResumeSkillRepository resumeSkillRepository,
			KeywordResumeJobMatcher keywordResumeJobMatcher,
			OpenAiChatClient chatClient) {
		this.analysisEnabled = analysisEnabled;
		this.resumeRepository = resumeRepository;
		this.resumeSkillRepository = resumeSkillRepository;
		this.keywordResumeJobMatcher = keywordResumeJobMatcher;
		this.chatClient = chatClient;
	}

	public ResumeAnalysisResponse analyze(String resumeId, String jobDescriptionText) {
		if (!analysisEnabled) {
			throw new AnalysisUnavailableException("AI analysis is not enabled");
		}

		Resume resume = resumeRepository.findById(resumeId)
				.orElseThrow(() -> new ResumeNotFoundException("No resume found with id: " + resumeId));

		if (resume.getProcessingStatus() != ProcessingStatus.COMPLETED) {
			throw new ResumeNotReadyException("Resume has not finished processing yet");
		}

		List<ResumeSkill> resumeSkills = resumeSkillRepository.findByResumeId(resumeId);
		Set<String> resumeSkillNames = resumeSkills.stream()
				.map(ResumeSkill::getSkillName)
				.collect(Collectors.toSet());

		// Grounds the LLM prompt in the already-computed, deterministic skill
		// breakdown so it has less room to invent skills that aren't really there.
		MatchResult skillContext =
				keywordResumeJobMatcher.match(resumeSkillNames, resume.getExtractedText(), jobDescriptionText);

		String userPrompt = """
				Job description:
				%s

				Resume:
				%s

				Already-detected matched required skills: %s
				Already-detected missing required skills: %s
				Already-detected matched preferred skills: %s
				Already-detected missing preferred skills: %s

				Using this context, identify the candidate's strengths for this role, the gaps or risks,
				and concrete suggestions to improve their fit or their resume for this specific job.
				"""
				.formatted(
						jobDescriptionText,
						resume.getExtractedText(),
						skillContext.matchedRequiredSkills(),
						skillContext.missingRequiredSkills(),
						skillContext.matchedPreferredSkills(),
						skillContext.missingPreferredSkills());

		String content = chatClient.complete(SYSTEM_PROMPT, userPrompt);
		return parseResponse(content);
	}

	private ResumeAnalysisResponse parseResponse(String content) {
		JsonNode root;
		try {
			root = objectMapper.readTree(content);
		} catch (IOException e) {
			throw new AnalysisUnavailableException("Chat completion API returned an unparseable analysis", e);
		}

		return new ResumeAnalysisResponse(
				readStringArray(root, "strengths"),
				readStringArray(root, "gaps"),
				readStringArray(root, "suggestions"));
	}

	private List<String> readStringArray(JsonNode root, String field) {
		JsonNode node = root.path(field);
		if (!node.isArray()) {
			throw new AnalysisUnavailableException(
					"Chat completion API response was missing the \"" + field + "\" field");
		}

		List<String> values = new ArrayList<>();
		node.forEach(item -> values.add(item.asText()));
		return values;
	}

}

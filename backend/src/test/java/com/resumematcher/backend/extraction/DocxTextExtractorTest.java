package com.resumematcher.backend.extraction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;

import com.resumematcher.backend.testsupport.SyntheticDocuments;
import org.junit.jupiter.api.Test;

class DocxTextExtractorTest {

	private final DocxTextExtractor docxTextExtractor = new DocxTextExtractor();

	@Test
	void extractsKnownTextFromGeneratedDocx() throws Exception {
		byte[] docxBytes = SyntheticDocuments.createSampleDocx("Synthetic resume text for extraction test");

		String extractedText = docxTextExtractor.extract(docxBytes);

		assertThat(extractedText).contains("Synthetic resume text for extraction test");
	}

	@Test
	void throwsTextExtractionExceptionForCorruptDocx() {
		byte[] corruptBytes = "this is not a real docx file".getBytes(StandardCharsets.UTF_8);

		assertThatThrownBy(() -> docxTextExtractor.extract(corruptBytes))
				.isInstanceOf(TextExtractionException.class);
	}

}

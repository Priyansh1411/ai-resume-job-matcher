package com.resumematcher.backend.extraction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;

import com.resumematcher.backend.testsupport.SyntheticDocuments;
import org.junit.jupiter.api.Test;

class PdfTextExtractorTest {

	private final PdfTextExtractor pdfTextExtractor = new PdfTextExtractor();

	@Test
	void extractsKnownTextFromGeneratedPdf() throws Exception {
		byte[] pdfBytes = SyntheticDocuments.createSamplePdf("Synthetic resume text for extraction test");

		String extractedText = pdfTextExtractor.extract(pdfBytes);

		assertThat(extractedText).contains("Synthetic resume text for extraction test");
	}

	@Test
	void throwsTextExtractionExceptionForCorruptPdf() {
		byte[] corruptBytes = "this is not a real pdf file".getBytes(StandardCharsets.UTF_8);

		assertThatThrownBy(() -> pdfTextExtractor.extract(corruptBytes))
				.isInstanceOf(TextExtractionException.class);
	}

}

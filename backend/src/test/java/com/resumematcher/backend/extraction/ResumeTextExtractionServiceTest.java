package com.resumematcher.backend.extraction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ResumeTextExtractionServiceTest {

	@Mock
	private PdfTextExtractor pdfTextExtractor;

	@Mock
	private DocxTextExtractor docxTextExtractor;

	@Test
	void normalizesLineEndingsAndTrimsWhitespace() {
		when(pdfTextExtractor.extract(any())).thenReturn("  Line one\r\nLine two\r\n  ");

		ResumeTextExtractionService service =
				new ResumeTextExtractionService(pdfTextExtractor, docxTextExtractor);

		String result = service.extractText(new byte[] { 1, 2, 3 }, "application/pdf");

		assertThat(result).isEqualTo("Line one\nLine two");
	}

	@Test
	void rejectsBlankExtractedTextAsFailure() {
		when(pdfTextExtractor.extract(any())).thenReturn("   ");

		ResumeTextExtractionService service =
				new ResumeTextExtractionService(pdfTextExtractor, docxTextExtractor);

		assertThatThrownBy(() -> service.extractText(new byte[] { 1, 2, 3 }, "application/pdf"))
				.isInstanceOf(TextExtractionException.class);
	}

	@Test
	void rejectsUnsupportedContentType() {
		ResumeTextExtractionService service =
				new ResumeTextExtractionService(pdfTextExtractor, docxTextExtractor);

		assertThatThrownBy(() -> service.extractText(new byte[] { 1, 2, 3 }, "text/plain"))
				.isInstanceOf(TextExtractionException.class);
	}

}

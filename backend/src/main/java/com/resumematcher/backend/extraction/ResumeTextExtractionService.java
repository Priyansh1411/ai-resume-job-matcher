package com.resumematcher.backend.extraction;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ResumeTextExtractionService {

	private static final String PDF_CONTENT_TYPE = "application/pdf";
	private static final String DOCX_CONTENT_TYPE =
			"application/vnd.openxmlformats-officedocument.wordprocessingml.document";

	private final PdfTextExtractor pdfTextExtractor;
	private final DocxTextExtractor docxTextExtractor;

	public ResumeTextExtractionService(PdfTextExtractor pdfTextExtractor, DocxTextExtractor docxTextExtractor) {
		this.pdfTextExtractor = pdfTextExtractor;
		this.docxTextExtractor = docxTextExtractor;
	}

	public String extractText(byte[] fileBytes, String contentType) {
		TextExtractor extractor = resolveExtractor(contentType);

		String rawText;
		try {
			rawText = extractor.extract(fileBytes);
		} catch (TextExtractionException e) {
			throw e;
		} catch (Exception e) {
			throw new TextExtractionException("Unable to extract text from the uploaded document", e);
		}

		String normalizedText = normalize(rawText);

		if (!StringUtils.hasText(normalizedText)) {
			throw new TextExtractionException("No readable text could be extracted from the document");
		}

		return normalizedText;
	}

	private TextExtractor resolveExtractor(String contentType) {
		if (PDF_CONTENT_TYPE.equals(contentType)) {
			return pdfTextExtractor;
		}
		if (DOCX_CONTENT_TYPE.equals(contentType)) {
			return docxTextExtractor;
		}
		throw new TextExtractionException("Unsupported content type for text extraction: " + contentType);
	}

	private String normalize(String text) {
		if (text == null) {
			return null;
		}
		return text.replace("\r\n", "\n").replace("\r", "\n").trim();
	}

}

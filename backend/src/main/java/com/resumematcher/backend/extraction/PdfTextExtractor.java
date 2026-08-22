package com.resumematcher.backend.extraction;

import java.io.IOException;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

@Component
public class PdfTextExtractor implements TextExtractor {

	@Override
	public String extract(byte[] fileBytes) {
		try (PDDocument document = Loader.loadPDF(fileBytes)) {
			if (document.isEncrypted()) {
				throw new TextExtractionException("PDF document is encrypted and cannot be read");
			}
			PDFTextStripper stripper = new PDFTextStripper();
			return stripper.getText(document);
		} catch (InvalidPasswordException e) {
			throw new TextExtractionException("PDF document requires a password", e);
		} catch (IOException e) {
			throw new TextExtractionException("Unable to read PDF document", e);
		}
	}

}

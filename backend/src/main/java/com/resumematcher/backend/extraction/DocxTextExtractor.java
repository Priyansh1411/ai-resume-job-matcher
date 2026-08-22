package com.resumematcher.backend.extraction;

import java.io.ByteArrayInputStream;

import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Component;

@Component
public class DocxTextExtractor implements TextExtractor {

	@Override
	public String extract(byte[] fileBytes) {
		try (ByteArrayInputStream inputStream = new ByteArrayInputStream(fileBytes);
				XWPFDocument document = new XWPFDocument(inputStream);
				XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
			return extractor.getText();
		} catch (Exception e) {
			throw new TextExtractionException("Unable to read DOCX document", e);
		}
	}

}

package com.resumematcher.backend.testsupport;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;

public final class SyntheticDocuments {

	private SyntheticDocuments() {
	}

	public static byte[] createSamplePdf(String text) throws IOException {
		try (PDDocument document = new PDDocument()) {
			PDPage page = new PDPage();
			document.addPage(page);
			try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
				contentStream.beginText();
				contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
				contentStream.newLineAtOffset(50, 700);
				for (String line : text.split("\n")) {
					contentStream.showText(line);
					contentStream.newLineAtOffset(0, -14);
				}
				contentStream.endText();
			}
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			document.save(outputStream);
			return outputStream.toByteArray();
		}
	}

	public static byte[] createSampleDocx(String text) throws IOException {
		try (XWPFDocument document = new XWPFDocument()) {
			XWPFParagraph paragraph = document.createParagraph();
			XWPFRun run = paragraph.createRun();
			run.setText(text);
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			document.write(outputStream);
			return outputStream.toByteArray();
		}
	}

}

package com.resumematcher.backend.dto;

import com.resumematcher.backend.entity.ProcessingStatus;

public record ResumeUploadResponse(
		String id,
		String originalFilename,
		String contentType,
		Long fileSizeBytes,
		ProcessingStatus processingStatus
) {
}

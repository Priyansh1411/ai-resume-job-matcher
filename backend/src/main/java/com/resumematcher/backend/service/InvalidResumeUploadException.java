package com.resumematcher.backend.service;

public class InvalidResumeUploadException extends RuntimeException {

	public InvalidResumeUploadException(String message) {
		super(message);
	}

}

package com.resumematcher.backend.profile;

public class ProfileExtractionException extends RuntimeException {

	public ProfileExtractionException(String message) {
		super(message);
	}

	public ProfileExtractionException(String message, Throwable cause) {
		super(message, cause);
	}

}

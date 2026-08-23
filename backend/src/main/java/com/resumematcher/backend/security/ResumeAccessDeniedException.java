package com.resumematcher.backend.security;

public class ResumeAccessDeniedException extends RuntimeException {

	public ResumeAccessDeniedException(String message) {
		super(message);
	}

}

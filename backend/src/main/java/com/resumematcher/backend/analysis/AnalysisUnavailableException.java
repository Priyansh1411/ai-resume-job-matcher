package com.resumematcher.backend.analysis;

public class AnalysisUnavailableException extends RuntimeException {

	public AnalysisUnavailableException(String message) {
		super(message);
	}

	public AnalysisUnavailableException(String message, Throwable cause) {
		super(message, cause);
	}

}

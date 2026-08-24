package com.resumematcher.backend.observability;

import java.time.Duration;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

/**
 * Single point of instrumentation for resume-processing latency, mirroring
 * {@link OpenAiMetrics}'s role for OpenAI calls: timers only, so the services
 * that wrap these calls stay focused on their own work.
 */
@Component
public class ResumeProcessingMetrics {

	private final MeterRegistry meterRegistry;

	public ResumeProcessingMetrics(MeterRegistry meterRegistry) {
		this.meterRegistry = meterRegistry;
	}

	public void recordUploadDuration(Duration duration) {
		Timer.builder("resume.upload.duration")
				.register(meterRegistry)
				.record(duration);
	}

	public void recordExtractionDuration(Duration duration) {
		Timer.builder("resume.extraction.duration")
				.register(meterRegistry)
				.record(duration);
	}

	public void recordMatchingDuration(Duration duration) {
		Timer.builder("resume.matching.duration")
				.register(meterRegistry)
				.record(duration);
	}

}

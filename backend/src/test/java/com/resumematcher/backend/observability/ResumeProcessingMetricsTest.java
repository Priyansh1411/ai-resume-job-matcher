package com.resumematcher.backend.observability;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

class ResumeProcessingMetricsTest {

	private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
	private final ResumeProcessingMetrics resumeProcessingMetrics = new ResumeProcessingMetrics(meterRegistry);

	@Test
	void recordsUploadDuration() {
		resumeProcessingMetrics.recordUploadDuration(Duration.ofMillis(150));

		assertThat(meterRegistry.get("resume.upload.duration").timer().count()).isEqualTo(1L);
		assertThat(meterRegistry.get("resume.upload.duration").timer().totalTime(TimeUnit.MILLISECONDS))
				.isEqualTo(150.0);
	}

	@Test
	void recordsExtractionDuration() {
		resumeProcessingMetrics.recordExtractionDuration(Duration.ofMillis(75));

		assertThat(meterRegistry.get("resume.extraction.duration").timer().count()).isEqualTo(1L);
		assertThat(meterRegistry.get("resume.extraction.duration").timer().totalTime(TimeUnit.MILLISECONDS))
				.isEqualTo(75.0);
	}

	@Test
	void recordsMatchingDuration() {
		resumeProcessingMetrics.recordMatchingDuration(Duration.ofMillis(220));

		assertThat(meterRegistry.get("resume.matching.duration").timer().count()).isEqualTo(1L);
		assertThat(meterRegistry.get("resume.matching.duration").timer().totalTime(TimeUnit.MILLISECONDS))
				.isEqualTo(220.0);
	}

	@Test
	void accumulatesMultipleRecordingsForTheSameTimer() {
		resumeProcessingMetrics.recordUploadDuration(Duration.ofMillis(100));
		resumeProcessingMetrics.recordUploadDuration(Duration.ofMillis(200));

		assertThat(meterRegistry.get("resume.upload.duration").timer().count()).isEqualTo(2L);
	}

}

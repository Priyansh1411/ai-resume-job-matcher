package com.resumematcher.backend.controller;

import java.util.Map;

import com.resumematcher.backend.service.DatabaseHealthService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DatabaseHealthController {

	private final DatabaseHealthService databaseHealthService;

	public DatabaseHealthController(DatabaseHealthService databaseHealthService) {
		this.databaseHealthService = databaseHealthService;
	}

	@GetMapping("/api/database/health")
	public Map<String, String> databaseHealth() {
		if (!databaseHealthService.isDatabaseUp()) {
			throw new IllegalStateException("Database health check failed");
		}
		return Map.of(
				"status", "UP",
				"database", "MySQL"
		);
	}

}

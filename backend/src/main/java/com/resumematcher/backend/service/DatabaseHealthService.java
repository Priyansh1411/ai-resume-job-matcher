package com.resumematcher.backend.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class DatabaseHealthService {

	private final JdbcTemplate jdbcTemplate;

	public DatabaseHealthService(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public boolean isDatabaseUp() {
		Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
		return result != null && result == 1;
	}

}

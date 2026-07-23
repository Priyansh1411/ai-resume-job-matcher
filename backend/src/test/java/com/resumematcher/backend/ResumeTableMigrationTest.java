package com.resumematcher.backend;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
class ResumeTableMigrationTest {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void resumesTableExistsAndAcceptsCountQuery() {
		Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM resumes", Integer.class);

		assertThat(count).isNotNull();
		assertThat(count).isGreaterThanOrEqualTo(0);
	}

}

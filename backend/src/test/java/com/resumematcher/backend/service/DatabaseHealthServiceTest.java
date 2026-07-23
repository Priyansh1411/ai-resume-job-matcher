package com.resumematcher.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class DatabaseHealthServiceTest {

	@Mock
	private JdbcTemplate jdbcTemplate;

	@Test
	void isDatabaseUpReturnsTrueWhenSelectOneSucceeds() {
		when(jdbcTemplate.queryForObject("SELECT 1", Integer.class)).thenReturn(1);

		DatabaseHealthService service = new DatabaseHealthService(jdbcTemplate);

		assertThat(service.isDatabaseUp()).isTrue();
	}

	@Test
	void isDatabaseUpReturnsFalseWhenResultIsNotOne() {
		when(jdbcTemplate.queryForObject("SELECT 1", Integer.class)).thenReturn(0);

		DatabaseHealthService service = new DatabaseHealthService(jdbcTemplate);

		assertThat(service.isDatabaseUp()).isFalse();
	}

}

package com.resumematcher.backend.testsupport;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

public abstract class AbstractMySqlIntegrationTest {

	static final MySQLContainer MYSQL_CONTAINER =
			new MySQLContainer(DockerImageName.parse("mysql:8.4"))
					.withDatabaseName("resume_matcher_test")
					.withUsername("test")
					.withPassword("test");

	static {
		MYSQL_CONTAINER.start();
	}

	@DynamicPropertySource
	static void registerDatasourceProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", MYSQL_CONTAINER::getJdbcUrl);
		registry.add("spring.datasource.username", MYSQL_CONTAINER::getUsername);
		registry.add("spring.datasource.password", MYSQL_CONTAINER::getPassword);
	}

}

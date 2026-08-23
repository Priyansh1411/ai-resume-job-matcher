package com.resumematcher.backend.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Optional;

import com.resumematcher.backend.entity.User;
import com.resumematcher.backend.testsupport.AbstractMySqlIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class UserRepositoryTest extends AbstractMySqlIntegrationTest {

	@Autowired
	private UserRepository userRepository;

	@Test
	void savesAndFindsAUserByEmail() {
		User user = new User();
		user.setEmail("jane@example.com");
		user.setPasswordHash("hashed-password");

		User saved = userRepository.save(user);

		assertThat(saved.getId()).isNotNull();

		Optional<User> found = userRepository.findByEmail("jane@example.com");

		assertThat(found).isPresent();
		assertThat(found.get().getId()).isEqualTo(saved.getId());
	}

	@Test
	void returnsEmptyWhenNoUserHasThatEmail() {
		Optional<User> found = userRepository.findByEmail("nobody@example.com");

		assertThat(found).isEmpty();
	}

	@Test
	void rejectsASecondUserWithTheSameEmailAtTheDatabaseLevel() {
		User first = new User();
		first.setEmail("jane@example.com");
		first.setPasswordHash("hashed-password-1");
		userRepository.saveAndFlush(first);

		User second = new User();
		second.setEmail("jane@example.com");
		second.setPasswordHash("hashed-password-2");

		assertThatThrownBy(() -> userRepository.saveAndFlush(second))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

}

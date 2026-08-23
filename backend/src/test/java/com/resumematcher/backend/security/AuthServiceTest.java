package com.resumematcher.backend.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import com.resumematcher.backend.dto.AuthResponse;
import com.resumematcher.backend.entity.User;
import com.resumematcher.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

	@Mock
	private UserRepository userRepository;

	private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
	private final JwtService jwtService = new JwtService("test-only-jwt-signing-secret-must-be-at-least-32-bytes", 24);

	private AuthService authService;

	@BeforeEach
	void setUp() {
		// Constructed here, not as a field initializer: @Mock fields are injected by
		// MockitoExtension after the test instance is created, so building authService
		// as a field initializer would capture a still-null userRepository.
		authService = new AuthService(userRepository, passwordEncoder, jwtService);
	}

	@Test
	void registersANewUserWithAHashedPasswordAndReturnsAToken() {
		when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.empty());
		when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
			User user = invocation.getArgument(0);
			user.setId("user-1");
			return user;
		});

		AuthResponse response = authService.register("jane@example.com", "correct-horse");

		assertThat(response.token()).isNotBlank();
		assertThat(jwtService.validateAndGetUserId(response.token())).contains("user-1");

		ArgumentCaptor<User> savedUser = ArgumentCaptor.forClass(User.class);
		verify(userRepository).save(savedUser.capture());
		assertThat(savedUser.getValue().getEmail()).isEqualTo("jane@example.com");
		assertThat(savedUser.getValue().getPasswordHash()).isNotEqualTo("correct-horse");
		assertThat(passwordEncoder.matches("correct-horse", savedUser.getValue().getPasswordHash())).isTrue();
	}

	@Test
	void rejectsRegistrationWhenTheEmailIsAlreadyTaken() {
		User existing = new User();
		existing.setEmail("jane@example.com");
		when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(existing));

		assertThatThrownBy(() -> authService.register("jane@example.com", "correct-horse"))
				.isInstanceOf(DuplicateEmailException.class);
	}

	@Test
	void logsInWithCorrectCredentialsAndReturnsAToken() {
		User user = new User();
		user.setId("user-1");
		user.setEmail("jane@example.com");
		user.setPasswordHash(passwordEncoder.encode("correct-horse"));
		when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(user));

		AuthResponse response = authService.login("jane@example.com", "correct-horse");

		assertThat(jwtService.validateAndGetUserId(response.token())).contains("user-1");
	}

	@Test
	void rejectsLoginWithTheWrongPassword() {
		User user = new User();
		user.setId("user-1");
		user.setEmail("jane@example.com");
		user.setPasswordHash(passwordEncoder.encode("correct-horse"));
		when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(user));

		assertThatThrownBy(() -> authService.login("jane@example.com", "wrong-password"))
				.isInstanceOf(InvalidCredentialsException.class);
	}

	@Test
	void rejectsLoginWithAnUnknownEmailUsingTheSameErrorAsAWrongPassword() {
		when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> authService.login("nobody@example.com", "anything"))
				.isInstanceOf(InvalidCredentialsException.class)
				.hasMessage("Invalid email or password");
	}

}

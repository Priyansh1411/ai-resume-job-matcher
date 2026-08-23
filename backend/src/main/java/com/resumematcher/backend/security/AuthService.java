package com.resumematcher.backend.security;

import com.resumematcher.backend.dto.AuthResponse;
import com.resumematcher.backend.entity.User;
import com.resumematcher.backend.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;

	public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
	}

	public AuthResponse register(String email, String password) {
		if (userRepository.findByEmail(email).isPresent()) {
			throw new DuplicateEmailException("An account with this email already exists");
		}

		User user = new User();
		user.setEmail(email);
		user.setPasswordHash(passwordEncoder.encode(password));
		User saved = userRepository.save(user);

		return new AuthResponse(jwtService.generateToken(saved.getId()));
	}

	public AuthResponse login(String email, String password) {
		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

		// Same error message and code path as the "user not found" case above -
		// distinguishing them in the response would let a caller enumerate which
		// emails have accounts.
		if (!passwordEncoder.matches(password, user.getPasswordHash())) {
			throw new InvalidCredentialsException("Invalid email or password");
		}

		return new AuthResponse(jwtService.generateToken(user.getId()));
	}

}

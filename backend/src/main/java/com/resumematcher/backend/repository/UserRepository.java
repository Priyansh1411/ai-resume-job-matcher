package com.resumematcher.backend.repository;

import java.util.Optional;

import com.resumematcher.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, String> {

	Optional<User> findByEmail(String email);

}

package com.study.server;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

	private final JdbcTemplate jdbcTemplate;

	public UserController(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@PostMapping
	public ResponseEntity<?> create(@RequestBody UserRequest request) {
		if (request == null || isBlank(request.name()) || isBlank(request.email())) {
			return ResponseEntity.badRequest().body(Map.of("message", "name and email are required"));
		}

		String name = request.name().trim();
		String email = request.email().trim();
		LocalDateTime createdAt = LocalDateTime.now();
		GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();

		try {
			jdbcTemplate.update(connection -> {
				PreparedStatement statement = connection.prepareStatement(
						"INSERT INTO users (name, email, created_at) VALUES (?, ?, ?)",
						Statement.RETURN_GENERATED_KEYS);
				statement.setString(1, name);
				statement.setString(2, email);
				statement.setTimestamp(3, Timestamp.valueOf(createdAt));
				return statement;
			}, keyHolder);
		} catch (DuplicateKeyException exception) {
			return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "email already exists"));
		}

		Number key = keyHolder.getKey();
		if (key == null) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(Map.of("message", "failed to create user"));
		}

		return ResponseEntity.status(HttpStatus.CREATED).body(new UserResponse(key.longValue(), name, email, createdAt));
	}

	private static boolean isBlank(String value) {
		return value == null || value.isBlank();
	}

}

record UserRequest(String name, String email) {
}

record UserResponse(long id, String name, String email, LocalDateTime createdAt) {
}

package com.study.server;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

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
@RequestMapping("/v1/users")
public class DeviceUserController {

	private final JdbcTemplate jdbcTemplate;

	public DeviceUserController(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@PostMapping
	public ResponseEntity<?> create(@RequestBody DeviceUserRequest request) {
		if (request == null || isBlank(request.device_id())) {
			return ResponseEntity.badRequest().body(Map.of("message", "device_id is required"));
		}

		String deviceId = request.device_id().trim();
		try {
			UUID.fromString(deviceId);
		} catch (IllegalArgumentException exception) {
			return ResponseEntity.badRequest().body(Map.of("message", "device_id must be a UUID"));
		}

		String userKey = hashDeviceId(deviceId);
		LocalDateTime createdAt = LocalDateTime.now();
		GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();

		try {
			jdbcTemplate.update(connection -> {
				PreparedStatement statement = connection.prepareStatement(
						"INSERT INTO `user` (device_id, user_key, created_at) VALUES (?, ?, ?)",
						Statement.RETURN_GENERATED_KEYS);
				statement.setString(1, deviceId);
				statement.setString(2, userKey);
				statement.setTimestamp(3, Timestamp.valueOf(createdAt));
				return statement;
			}, keyHolder);
		} catch (DuplicateKeyException exception) {
			DeviceUserResponse existing = jdbcTemplate.queryForObject(
					"SELECT id, device_id, user_key, created_at FROM `user` WHERE device_id = ?",
					(rs, rowNum) -> new DeviceUserResponse(
							rs.getLong("id"),
							rs.getString("device_id"),
							rs.getString("user_key"),
							rs.getTimestamp("created_at").toLocalDateTime()),
					deviceId);
			return ResponseEntity.ok(existing);
		}

		Number key = keyHolder.getKey();
		if (key == null) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(Map.of("message", "failed to create user"));
		}

		return ResponseEntity.status(HttpStatus.CREATED)
				.body(new DeviceUserResponse(key.longValue(), deviceId, userKey, createdAt));
	}

	private static String hashDeviceId(String deviceId) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(deviceId.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(hash);
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 not available", exception);
		}
	}

	private static boolean isBlank(String value) {
		return value == null || value.isBlank();
	}

}

record DeviceUserRequest(String device_id) {
}

record DeviceUserResponse(long id, String device_id, String user_key, LocalDateTime createdAt) {
}

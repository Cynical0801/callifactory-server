package com.study.server;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

	private final JdbcTemplate jdbcTemplate;

	public HealthController(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@GetMapping("/api/health")
	public Map<String, Object> health() {
		Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("status", "ok");
		body.put("db", result);
		body.put("time", OffsetDateTime.now().toString());
		return body;
	}

}

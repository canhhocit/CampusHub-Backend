package com.campushub;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

@SpringBootApplication
@EnableScheduling
@EnableAsync
public class CampusHubApplication {

	private static final Logger log = LoggerFactory.getLogger(CampusHubApplication.class);

	public static void main(String[] args) {
		loadDotEnv();
		SpringApplication.run(CampusHubApplication.class, args);
	}

	private static void loadDotEnv() {
		File envFile = new File(".env");
		if (!envFile.exists()) {
			envFile = new File("sourcecode/CampusHub-Backend/.env");
		}
		if (!envFile.exists()) {
			return;
		}

		try {
			List<String> lines = Files.readAllLines(envFile.toPath());
			for (String line : lines) {
				line = line.trim();
				if (line.isEmpty() || line.startsWith("#") || !line.contains("=")) {
					continue;
				}
				int idx = line.indexOf('=');
				String key = line.substring(0, idx).trim();
				String value = line.substring(idx + 1).trim();

				if (value.startsWith("\"") && value.endsWith("\"") && value.length() >= 2) {
					value = value.substring(1, value.length() - 1);
				} else if (value.startsWith("'") && value.endsWith("'") && value.length() >= 2) {
					value = value.substring(1, value.length() - 1);
				}

				if (System.getProperty(key) == null && System.getenv(key) == null) {
					System.setProperty(key, value);
				}
			}
			log.info("Loaded environment variables from {}", envFile.getAbsolutePath());
		} catch (IOException e) {
			log.warn("Could not read .env file: {}", e.getMessage());
		}
	}
}

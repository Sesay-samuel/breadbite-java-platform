
package com.breadbitt.utility;

import java.security.SecureRandom;

import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class SecurityUtility {

	private static final String PASSWORD_CHARACTERS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";

	private static final SecureRandom SECURE_RANDOM = new SecureRandom();

	@Bean
	public static BCryptPasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder(12);
	}

	public static String randomPassword() {
		StringBuilder password = new StringBuilder();

		for (int i = 0; i < 18; i++) {
			int index = SECURE_RANDOM.nextInt(PASSWORD_CHARACTERS.length());
			password.append(PASSWORD_CHARACTERS.charAt(index));
		}

		return password.toString();
	}
}

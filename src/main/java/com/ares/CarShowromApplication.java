package com.ares;

import com.ares.model.User;
import com.ares.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class CarShowromApplication {

	public static void main(String[] args) {
		SpringApplication.run(CarShowromApplication.class, args);
	}

	@Bean
	public CommandLineRunner seedDefaultUser(UserRepository userRepository) {
		return args -> {
			if (!userRepository.existsByUsername("admin")) {
				User admin = new User();
				admin.setUsername("admin");
				admin.setPassword("123");
				admin.setEmail("manhmxtp000751@gmail.com");
				admin.setPhone("0988888888");
				admin.setGender("Nam");
				admin.setVerifiedType("GOOGLE");
				userRepository.save(admin);
				System.out.println("===> [DEMO USER SEEDED] Tài khoản mặc định: admin / 123");
			}
		};
	}
}

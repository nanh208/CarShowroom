package com.ares;

import com.ares.model.User;
import com.ares.model.Auction;
import com.ares.repository.AuctionRepository;
import com.ares.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.time.LocalDateTime;

@SpringBootApplication
public class CarShowromApplication {

	public static void main(String[] args) {
		SpringApplication.run(CarShowromApplication.class, args);
	}

	@Bean
	public CommandLineRunner seedDefaultUser(UserRepository userRepository, AuctionRepository auctionRepository) {
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

			if (auctionRepository.count() == 0) {
				LocalDateTime now = LocalDateTime.now();
				auctionRepository.save(new Auction(
						"Phiên đấu giá xe hiếm", "Toyota 2000GT 1967",
						"/images/cars/2000gt.jpg", 2_400_000_000L, 25_000_000L,
						now.minusHours(2), now.plusDays(2)));
				auctionRepository.save(new Auction(
						"Phiên đấu giá sắp diễn ra", "Porsche Carrera GT",
						"/images/cars/carreragt.jpg", 8_900_000_000L, 50_000_000L,
						now.plusDays(3), now.plusDays(6)));
			}
		};
	}
}

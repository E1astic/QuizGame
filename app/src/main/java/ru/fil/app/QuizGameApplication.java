package ru.fil.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "ru.fil")
@EnableJpaRepositories(basePackages = "ru.fil")
@EntityScan(basePackages = "ru.fil")
public class QuizGameApplication {

	public static void main(String[] args) {
		SpringApplication.run(QuizGameApplication.class, args);
//		ApplicationContext context = SpringApplication.run(QuizGameApplication.class, args);
//		PasswordEncoder encoder = (PasswordEncoder) context.getBean("passwordEncoder");
//		System.out.println(encoder.encode("moder"));
	}

}

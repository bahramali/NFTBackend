package se.hydroleaf;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class NftBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(NftBackendApplication.class, args);
	}

}

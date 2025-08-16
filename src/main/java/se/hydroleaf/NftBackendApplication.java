package se.hydroleaf;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableScheduling
@EnableCaching
public class NftBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(NftBackendApplication.class, args);
	}

}

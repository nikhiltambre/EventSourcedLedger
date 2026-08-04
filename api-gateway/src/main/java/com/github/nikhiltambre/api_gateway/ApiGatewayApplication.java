package com.github.nikhiltambre.api_gateway;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ApiGatewayApplication implements CommandLineRunner {

	public static void main(String[] args) {
		SpringApplication.run(ApiGatewayApplication.class, args);
	}

    @Override
    public void run(String... args) throws Exception {
        System.out.println("==================================================");
        System.out.println("🚀 SUCCESS: Started API GATEWAY!");
        System.out.println("==================================================");

    }

}

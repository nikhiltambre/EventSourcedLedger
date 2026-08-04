package com.query_service.WalletQueryService;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class WalletQueryServiceApplication implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(WalletQueryServiceApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("==================================================");
        System.out.println("🚀 SUCCESS: Started QUERY SERVICE!");
        System.out.println("==================================================");

    }

}

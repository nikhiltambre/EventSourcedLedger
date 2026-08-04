package com.command_service.WalletCommandService;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class WalletCommandServiceApplication implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(WalletCommandServiceApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("==================================================");
        System.out.println("🚀 SUCCESS: Started COMMAND SERVICE!");
        System.out.println("==================================================");

    }

}

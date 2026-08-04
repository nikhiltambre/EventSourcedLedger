package com.eureka_server.EurekaServer;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

@SpringBootApplication
@EnableEurekaServer
public class EurekaServerApplication implements CommandLineRunner {
    public static void main(String[] args) {
        SpringApplication.run(EurekaServerApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("==================================================");
        System.out.println("🚀 SUCCESS: Started EUREKA SERVER!");
        System.out.println("==================================================");

    }

}

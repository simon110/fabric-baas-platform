package com.anhui.fabricbaasttp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.anhui"})
public class FabricBaasTtpApplication {

    public static void main(String[] args) {
        SpringApplication.run(FabricBaasTtpApplication.class, args);
    }

}

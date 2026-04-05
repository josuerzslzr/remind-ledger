package com.remindledger;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class RemindLedgerApplication {

    /**
     * Application entry point that boots the Spring application.
     *
     * @param args command-line arguments forwarded to SpringApplication
     */
    public static void main(String[] args) {
        SpringApplication.run(RemindLedgerApplication.class, args);
    }
}

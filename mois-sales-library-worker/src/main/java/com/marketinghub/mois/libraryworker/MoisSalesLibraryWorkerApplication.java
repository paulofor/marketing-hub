package com.marketinghub.mois.libraryworker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MoisSalesLibraryWorkerApplication {
    public static void main(String[] args) { SpringApplication.run(MoisSalesLibraryWorkerApplication.class, args); }
}

package com.studymatch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@org.springframework.scheduling.annotation.EnableScheduling
public class StudyMatchApplication {

    public static void main(String[] args) {
        SpringApplication.run(StudyMatchApplication.class, args);
        System.out.println("\n=======================================================");
        System.out.println("🚀 Backend de StudyMatch iniciado correctamente.");
        System.out.println("=======================================================\n");
    }
}

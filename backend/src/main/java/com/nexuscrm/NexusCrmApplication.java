package com.nexuscrm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class NexusCrmApplication {

  public static void main(String[] args) {
    SpringApplication.run(NexusCrmApplication.class, args);
  }
}

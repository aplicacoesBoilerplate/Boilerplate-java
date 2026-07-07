package com.java.boilerplate;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
@ConfigurationPropertiesScan
public class CBoilerplateApplication {
    public static void main(String[] pArgs) {
        SpringApplication.run(CBoilerplateApplication.class, pArgs);
    }
}

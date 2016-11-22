package org.rzats.jsonschema;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The main Spring Boot application for the API.
 */
@SpringBootApplication
public class JsonValidatorApplication {
    /**
     * The entry point of the application.
     *
     * @param args Console arguments.
     */
    public static void main(String[] args) {
        SpringApplication.run(JsonValidatorApplication.class);
    }
}

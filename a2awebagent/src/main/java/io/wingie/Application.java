package io.wingie;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
// TODO: Add @EnableAgent annotation once tools4ai-annotations dependency is available

@SpringBootApplication
// @EnableAgent
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}

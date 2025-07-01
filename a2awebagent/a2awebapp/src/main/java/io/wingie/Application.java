package io.wingie;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.EnableScheduling;

import io.wingie.a2acore.annotation.EnableA2ACore;

@PropertySource("classpath:tools4ai.properties")
@SpringBootApplication
@EnableScheduling
@EnableA2ACore
@ComponentScan(basePackages = {
    "io.wingie",
    "io.wingie.a2acore"
})
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}

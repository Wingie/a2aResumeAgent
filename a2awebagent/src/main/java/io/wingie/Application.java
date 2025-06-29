package io.wingie;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
// TODO: Add @EnableAgent annotation once tools4ai-annotations dependency is available

import io.github.vishalmysore.tools4ai.EnableAgent;

@SpringBootApplication
@EnableAgent
@ComponentScan(excludeFilters = {
    @ComponentScan.Filter(
        type = FilterType.REGEX,
        pattern = "io\\.github\\.vishalmysore\\.tools4ai\\.MainEntryPoint"),
    @ComponentScan.Filter(
        type = FilterType.REGEX,
        pattern = "io\\.github\\.vishalmysore\\.tools4ai\\.A2ACardController")
})
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}

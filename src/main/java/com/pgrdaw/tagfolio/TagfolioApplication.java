package com.pgrdaw.tagfolio;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * The main entry point for the Tagfolio application.
 *
 * @author Pablo Gimeno Ramallo &lt;pgrdaw@gmail.com&gt;
 * @since 2026-01-01
 */
@SpringBootApplication
@EnableJpaAuditing
public class TagfolioApplication {
    /**
     * The main method, which serves as the entry point for the application.
     *
     * @param args Command-line arguments.
     */
    public static void main(String[] args) {
        SpringApplication.run(TagfolioApplication.class, args);
    }
}

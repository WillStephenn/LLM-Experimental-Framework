package com.locallab;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main entry point for the LocalLab application.
 *
 * <p>LocalLab is a local-first LLM experimental framework for benchmarking and comparing language
 * models via Ollama. The core workflow follows: Task Template → Experiment → N models × M configs ×
 * X iterations → Analytics.
 */
@SpringBootApplication
public class LocalLabApplication {

    /**
     * Application entry point.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(LocalLabApplication.class, args);
    }
}

package com.springboot.datagenerator.runner;

import com.springboot.datagenerator.orchestrator.GeneratorOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataGeneratorRunner implements ApplicationRunner {

    private final GeneratorOrchestrator orchestrator;
    private final ApplicationContext context;

    @Override
    public void run(ApplicationArguments args) {
        int exitCode = execute();
        System.exit(SpringApplication.exit(context, () -> exitCode));
    }

    private int execute() {
        log.info("Data generator started");
        long start = System.currentTimeMillis();
        try {
            orchestrator.run();
            log.info("Data generation complete in {}ms", System.currentTimeMillis() - start);
            return 0;
        } catch (Exception e) {
            log.error("Data generation failed", e);
            return 1;
        }
    }
}

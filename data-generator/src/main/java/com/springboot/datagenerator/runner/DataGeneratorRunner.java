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
        log.info("Data generator started");
        long start = System.currentTimeMillis();
        orchestrator.run();
        log.info("Data generation complete in {}ms", System.currentTimeMillis() - start);

        int exitCode = SpringApplication.exit(context, () -> 0);
        System.exit(exitCode);
    }
}

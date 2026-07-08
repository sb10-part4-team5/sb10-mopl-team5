package com.springboot.datagenerator.generator;

import com.springboot.datagenerator.config.GeneratorProperties;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import net.datafaker.Faker;
import org.springframework.jdbc.core.JdbcTemplate;

@RequiredArgsConstructor
public abstract class BaseGenerator {

    protected final GeneratorProperties properties;
    protected static final ThreadLocal<Faker> faker = ThreadLocal.withInitial(Faker::new);
    protected final JdbcTemplate template;
    protected final Executor executor;

    protected final Instant now = Instant.now();
    protected final Instant weekAgo = now.minus(7, ChronoUnit.DAYS);
    protected final Instant twoWeeksAgo = weekAgo.minus(7, ChronoUnit.DAYS);

    @FunctionalInterface
    protected interface ChunkProcessor {
        void process(int offset, int chunk);
    }

    protected <T> List<T> parallel(int total, Function<Integer, List<T>> chunkGenerator) {
        int chunkSize = properties.dbBatchSize();
        List<CompletableFuture<List<T>>> futures = new ArrayList<>();
        for (int offset = 0; offset < total; offset += chunkSize) {
            int chunk = Math.min(chunkSize, total - offset);
            futures.add(CompletableFuture.supplyAsync(() -> chunkGenerator.apply(chunk), executor));
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        return futures.stream().flatMap(f -> f.join().stream()).toList();
    }

    protected void parallelInsert(int total, ChunkProcessor processor) {
        int chunkSize = properties.dbBatchSize();
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (int offset = 0; offset < total; offset += chunkSize) {
            int chunk = Math.min(chunkSize, total - offset);
            int finalOffset = offset;
            futures.add(CompletableFuture.runAsync(() -> processor.process(finalOffset, chunk), executor));
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }

    protected Instant randomBetween(Instant from, Instant to) {
        long delta = to.getEpochSecond() - from.getEpochSecond();
        if (delta <= 0) return from;
        return from.plusSeconds(ThreadLocalRandom.current().nextLong(delta));
    }

    protected String insertSql(String table, String... fields) {
        String cols = String.join(", ", fields);
        String placeholders = String.join(", ", Collections.nCopies(fields.length, "?"));
        return "INSERT INTO %s (%s) VALUES (%s)".formatted(table, cols, placeholders);
    }

    protected UUID randomFrom(List<UUID> ids) {
        return ids.get(ThreadLocalRandom.current().nextInt(ids.size()));
    }

    protected Set<Integer> uniqueRandomInts(int bound, int count) {
        count = Math.min(count, bound);
        if (count == bound) {
            Set<Integer> all = new HashSet<>();
            for (int i = 0; i < bound; i++) all.add(i);
            return all;
        }
        Set<Integer> result = new HashSet<>();
        while (result.size() < count) {
            result.add(ThreadLocalRandom.current().nextInt(bound));
        }
        return result;
    }
}

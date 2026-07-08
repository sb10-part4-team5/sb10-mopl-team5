package com.springboot.datagenerator.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.Min;

@Validated
@ConfigurationProperties(prefix = "generator")
public record GeneratorProperties(
    @Min(1) int user,
    @Min(1) int content,
    @Min(1) int tag,
    @Min(1) int tagPerContent,
    @Min(0) int reviewPerUser,
    @Min(0) int playlistPerUser,
    @Min(1) int itemPerPlaylist,
    @Min(0) int subscriptionPerUser,
    @Min(0) int followPerUser,
    @Min(0) int notificationPerUser,
    @Min(1) int dbBatchSize
) {}

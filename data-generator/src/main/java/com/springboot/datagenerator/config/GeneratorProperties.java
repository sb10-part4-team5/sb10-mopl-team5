package com.springboot.datagenerator.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "generator")
public record GeneratorProperties(
    int user,
    int content,
    int tag,
    int tagPerContent,
    int reviewPerUser,
    int playlistPerUser,
    int itemPerPlaylist,
    int subscriptionPerUser,
    int followPerUser,
    int notificationPerUser,
    int dbBatchSize
) {}

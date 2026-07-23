package com.codeit.team5.mopl.global.infra.opensearch.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

/**
 * basePackages를 지정하지 않으면(= Boot의 ElasticsearchRepositoriesAutoConfiguration을 쓰면)
 * com.codeit.team5.mopl 전체를 스캔해 JPA 리포지토리까지 "ES 리포지토리인가" 판별을 시도한다.
 * 그래서 자동설정은 MoplApplication에서 제외하고, 여기서 범위를 좁혀 직접 켠다.
 */
@Configuration
@EnableElasticsearchRepositories(basePackages = "com.codeit.team5.mopl.content.repository.opensearch")
public class OpenSearchConfig {
}

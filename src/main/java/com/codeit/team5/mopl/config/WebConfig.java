package com.codeit.team5.mopl.config;

import com.codeit.team5.mopl.content.entity.ContentSortByType;
import com.codeit.team5.mopl.content.entity.ContentType;
import com.codeit.team5.mopl.global.exception.InvalidSortDirectionException;
import com.codeit.team5.mopl.review.contant.ReviewSortBy;
import com.codeit.team5.mopl.user.constant.UserSortBy;
import com.codeit.team5.mopl.watcher.constant.SortByType;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(String.class, Sort.Direction.class, value -> {
            if (value.equalsIgnoreCase("ASCENDING") || value.equalsIgnoreCase("ASC")) {
                return Sort.Direction.ASC;
            }
            if (value.equalsIgnoreCase("DESCENDING") || value.equalsIgnoreCase("DESC")) {
                return Sort.Direction.DESC;
            }
            throw new InvalidSortDirectionException(value);
        });

        registry.addConverter(String.class, UserSortBy.class, UserSortBy::from);
        registry.addConverter(String.class, ReviewSortBy.class, ReviewSortBy::from);
        registry.addConverter(String.class, ContentSortByType.class, ContentSortByType::from);
        registry.addConverter(String.class, ContentType.class, ContentType::from);
        registry.addConverter(String.class, SortByType.class, SortByType::from);
    }
}

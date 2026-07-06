package com.codeit.team5.mopl.tag.util;

import com.codeit.team5.mopl.tag.entity.Tag;
import com.codeit.team5.mopl.tag.repository.TagRepository;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class TagResolver {

    private TagResolver() {}

    /**
     * 태그 이름 목록을 이름 기준으로 정규화한다 (trim, 빈 값 제거, 소문자 변환, 중복 제거).
     * 개수 제한이나 빈 목록에 대한 검증은 하지 않으므로, 필요하다면 호출부에서 별도로 검증한다.
     */
    public static List<String> normalizeNames(List<String> rawTagNames) {
        return rawTagNames.stream()
                .map(String::trim)
                .filter(name -> !name.isEmpty())
                .map(String::toLowerCase)
                .distinct()
                .toList();
    }

    /**
     * 태그 이름 목록을 Tag 엔티티로 해석한다. 이미 존재하는 태그는 재사용하고,
     * 존재하지 않는 태그만 새로 생성하여 저장한다.
     *
     * @return 태그 이름 → Tag 엔티티 맵
     */
    public static Map<String, Tag> resolve(List<String> tagNames, TagRepository tagRepository) {
        Map<String, Tag> existingTags = tagRepository.findByNameIn(tagNames).stream()
                .collect(Collectors.toMap(Tag::getName, Function.identity()));

        List<Tag> newTags = tagNames.stream()
                .filter(name -> !existingTags.containsKey(name))
                .map(Tag::create)
                .toList();

        if (!newTags.isEmpty()) {
            tagRepository.saveAll(newTags).forEach(tag -> existingTags.put(tag.getName(), tag));
        }

        return existingTags;
    }
}

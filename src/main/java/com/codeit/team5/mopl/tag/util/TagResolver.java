package com.codeit.team5.mopl.tag.util;

import com.codeit.team5.mopl.tag.entity.Tag;
import com.codeit.team5.mopl.tag.repository.TagRepository;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class TagResolver {

    private TagResolver() {}

    /**
     * 태그 이름 하나를 정규화한다 (trim, 소문자 변환). 정규화 후 빈 문자열이면 null을 반환한다.
     */
    public static String normalize(String rawName) {
        String trimmed = rawName.trim();
        return trimmed.isEmpty() ? null : trimmed.toLowerCase();
    }

    /**
     * 태그 이름 목록을 이름 기준으로 정규화한다 (trim, 빈 값 제거, 소문자 변환, 중복 제거).
     * 개수 제한이나 빈 목록에 대한 검증은 하지 않으므로, 필요하다면 호출부에서 별도로 검증한다.
     */
    public static List<String> normalizeNames(List<String> rawTagNames) {
        return rawTagNames.stream()
                .map(TagResolver::normalize)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    /**
     * 태그 이름 목록을 Tag 엔티티로 해석한다. 이미 존재하는 태그는 재사용하고,
     * 존재하지 않는 태그만 새로 생성하여 저장한다.
     *
     * <p>입력 목록에 정규화되지 않은 값(대소문자 차이, 앞뒤 공백 등)이 섞여 있으면
     * 서로 다른 이름으로 취급되어 별도 태그가 생성될 수 있으므로, 호출 전 {@link #normalizeNames}
     * 등으로 정규화하는 것을 권장한다. 다만 완전히 동일한 문자열이 중복으로 들어오는 경우는
     * 이 메서드가 내부적으로 제거하므로(동일 이름 신규 태그가 두 번 저장되어 유니크 제약을
     * 위반하는 상황 방지), 호출부에서 별도로 방어할 필요는 없다.</p>
     *
     * @return 태그 이름 → Tag 엔티티 맵
     */
    public static Map<String, Tag> resolve(List<String> tagNames, TagRepository tagRepository) {
        List<String> uniqueNames = tagNames.stream().distinct().toList();

        Map<String, Tag> existingTags = tagRepository.findByNameIn(uniqueNames).stream()
                .collect(Collectors.toMap(Tag::getName, Function.identity()));

        List<Tag> newTags = uniqueNames.stream()
                .filter(name -> !existingTags.containsKey(name))
                .map(Tag::create)
                .toList();

        if (!newTags.isEmpty()) {
            tagRepository.saveAll(newTags).forEach(tag -> existingTags.put(tag.getName(), tag));
        }

        return existingTags;
    }
}

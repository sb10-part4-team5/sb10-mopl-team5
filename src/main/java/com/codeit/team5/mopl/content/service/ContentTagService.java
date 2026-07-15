package com.codeit.team5.mopl.content.service;

import com.codeit.team5.mopl.content.entity.Content;
import com.codeit.team5.mopl.content.entity.ContentTag;
import com.codeit.team5.mopl.tag.entity.Tag;
import com.codeit.team5.mopl.tag.repository.TagRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * {@link ContentService}가 관리자 콘텐츠 CRUD에서 사용하는 콘텐츠-태그 연결 로직을 전담하는 서비스.
 *
 * <p>배치 수집({@link com.codeit.team5.mopl.content.batch.writer.ContentItemWriter})은 도메인과 검증 규칙이 달라
 * 이 클래스를 공유하지 않고 자체적으로 태그를 처리한다.</p>
 */
@Service
@RequiredArgsConstructor
public class ContentTagService {

    private final TagRepository tagRepository;

    /**
     * 태그 이름 목록을 정규화한다 (trim, 빈 값 제거, 소문자 변환, 중복 제거).
     */
    public List<String> normalizeNames(List<String> rawTagNames) {
        return rawTagNames.stream()
                .map(ContentTagService::normalize)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    /**
     * 콘텐츠에 태그를 새로 연결한다. 이미 존재하는 태그는 재사용하고, 존재하지 않는 태그만 새로 생성한다.
     *
     * @param tagNames 정규화된 태그 이름 목록이어야 한다 ({@link #normalizeNames} 결과 등)
     */
    public void attachTags(Content content, List<String> tagNames) {
        Map<String, Tag> resolvedTags = tagRepository.findOrCreateAllByName(tagNames);
        tagNames.forEach(name -> content.addTag(ContentTag.create(content, resolvedTags.get(name))));
    }

    /**
     * 콘텐츠의 태그를 요청된 이름 목록과 일치하도록 갱신한다 — 유지될 태그는 그대로 두고, 빠진 태그는 제거하며, 새로 추가된 태그만 연결한다.
     *
     * @param requestedNames 정규화된 태그 이름 목록이어야 한다 ({@link #normalizeNames} 결과 등)
     */
    public void updateTags(Content content, List<String> requestedNames) {
        Set<String> currentNames = content.getContentTags().stream()
                .map(ct -> ct.getTag().getName())
                .collect(Collectors.toSet());

        Set<String> requestedSet = new HashSet<>(requestedNames);
        content.getContentTags().removeIf(ct -> !requestedSet.contains(ct.getTag().getName()));

        List<String> toAdd = requestedNames.stream()
                .filter(name -> !currentNames.contains(name))
                .toList();

        if (!toAdd.isEmpty()) {
            attachTags(content, toAdd);
        }
    }

    private static String normalize(String rawName) {
        String trimmed = rawName.trim();
        return trimmed.isEmpty() ? null : trimmed.toLowerCase(Locale.ROOT);
    }
}

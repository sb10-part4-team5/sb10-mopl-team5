package com.codeit.team5.mopl.content.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.codeit.team5.mopl.content.entity.Content;
import com.codeit.team5.mopl.content.entity.ContentTag;
import com.codeit.team5.mopl.content.entity.ContentType;
import com.codeit.team5.mopl.tag.entity.Tag;
import com.codeit.team5.mopl.tag.repository.TagRepository;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ContentTagServiceTest {

    @Mock
    private TagRepository tagRepository;

    @InjectMocks
    private ContentTagService contentTagService;

    // --- normalizeNames ---

    @Test
    @DisplayName("앞뒤 공백을 제거하고 소문자로 변환한다")
    void normalizeNames_trimsAndLowercases() {
        // when
        List<String> result = contentTagService.normalizeNames(List.of("  Action  ", "DRAMA"));

        // then
        assertThat(result).containsExactly("action", "drama");
    }

    @Test
    @DisplayName("빈 문자열이나 공백만 있는 값은 제거한다")
    void normalizeNames_removesBlankNames() {
        // when
        List<String> result = contentTagService.normalizeNames(List.of("액션", "   ", ""));

        // then
        assertThat(result).containsExactly("액션");
    }

    @Test
    @DisplayName("정규화 후 동일한 값은 중복 제거한다")
    void normalizeNames_deduplicatesAfterNormalization() {
        // when
        List<String> result = contentTagService.normalizeNames(List.of("Action", " action ", "ACTION"));

        // then
        assertThat(result).containsExactly("action");
    }

    @Test
    @DisplayName("빈 목록을 넣으면 예외 없이 빈 목록을 반환한다")
    void normalizeNames_emptyInput_returnsEmptyList() {
        // when
        List<String> result = contentTagService.normalizeNames(List.of());

        // then
        assertThat(result).isEmpty();
    }

    // --- attachTags ---

    @Test
    @DisplayName("리포지토리가 돌려준 태그를 콘텐츠에 그대로 연결한다")
    void attachTags_attachesTagsResolvedByRepository() {
        // given
        Content content = Content.createByAdmin(ContentType.MOVIE, "테스트 영화", null);
        Tag actionTag = tagWithId("액션");
        Tag dramaTag = tagWithId("드라마");
        given(tagRepository.findOrCreateAllByName(List.of("액션", "드라마")))
                .willReturn(Map.of("액션", actionTag, "드라마", dramaTag));

        // when
        contentTagService.attachTags(content, List.of("액션", "드라마"));

        // then
        assertThat(tagNamesOf(content)).containsExactlyInAnyOrder("액션", "드라마");
    }

    @Test
    @DisplayName("태그 이름 목록이 비어 있으면 아무 태그도 연결하지 않는다")
    void attachTags_emptyNames_attachesNothing() {
        // given
        Content content = Content.createByAdmin(ContentType.MOVIE, "테스트 영화", null);
        given(tagRepository.findOrCreateAllByName(List.of())).willReturn(Map.of());

        // when
        contentTagService.attachTags(content, List.of());

        // then
        assertThat(content.getContentTags()).isEmpty();
    }

    // --- updateTags ---

    @Test
    @DisplayName("수정 시 유지되는 태그는 delete/insert 없이 그대로 유지되고, 빠진 태그는 제거되며, 추가된 태그만 조회한다")
    void updateTags_retainsExisting_removesMissing_attachesNewOnly() {
        // given
        Content content = Content.createByAdmin(ContentType.MOVIE, "기존 제목", null);
        ReflectionTestUtils.setField(content, "id", UUID.randomUUID());

        Tag actionTag = tagWithId("액션");
        Tag sfTag = tagWithId("sf");
        content.addTag(ContentTag.create(content, actionTag));  // 유지될 태그
        content.addTag(ContentTag.create(content, sfTag));      // 제거될 태그

        Tag comedyTag = tagWithId("코미디");
        given(tagRepository.findOrCreateAllByName(List.of("코미디"))).willReturn(Map.of("코미디", comedyTag));

        // when: 액션 유지 + 코미디 추가 (sf 제거)
        contentTagService.updateTags(content, List.of("액션", "코미디"));

        // then: 추가할 태그만 리포지토리에 조회/생성 요청한다
        verify(tagRepository).findOrCreateAllByName(List.of("코미디"));
        assertThat(tagNamesOf(content)).containsExactlyInAnyOrder("액션", "코미디");
    }

    @Test
    @DisplayName("요청된 태그가 하나도 없으면 기존 태그를 모두 제거하고 리포지토리를 호출하지 않는다")
    void updateTags_emptyRequest_removesAllExistingTags() {
        // given
        Content content = Content.createByAdmin(ContentType.MOVIE, "기존 제목", null);
        content.addTag(ContentTag.create(content, tagWithId("액션")));

        // when
        contentTagService.updateTags(content, List.of());

        // then
        assertThat(content.getContentTags()).isEmpty();
        verify(tagRepository, never()).findOrCreateAllByName(anyList());
    }

    private Set<String> tagNamesOf(Content content) {
        return content.getContentTags().stream()
                .map(ct -> ct.getTag().getName())
                .collect(Collectors.toSet());
    }

    private Tag tagWithId(String name) {
        Tag tag = Tag.create(name);
        ReflectionTestUtils.setField(tag, "id", UUID.randomUUID());
        return tag;
    }
}

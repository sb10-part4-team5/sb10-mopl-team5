package com.codeit.team5.mopl.tag.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.codeit.team5.mopl.tag.entity.Tag;
import com.codeit.team5.mopl.tag.repository.TagRepository;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TagResolverTest {

    @Mock
    private TagRepository tagRepository;

    // --- normalizeNames ---

    @Test
    @DisplayName("앞뒤 공백을 제거하고 소문자로 변환한다")
    void normalizeNames_trimsAndLowercases() {
        // when
        List<String> result = TagResolver.normalizeNames(List.of("  Action  ", "DRAMA"));

        // then
        assertThat(result).containsExactly("action", "drama");
    }

    @Test
    @DisplayName("빈 문자열이나 공백만 있는 값은 제거한다")
    void normalizeNames_removesBlankNames() {
        // when
        List<String> result = TagResolver.normalizeNames(List.of("액션", "   ", ""));

        // then
        assertThat(result).containsExactly("액션");
    }

    @Test
    @DisplayName("정규화 후 동일한 값은 중복 제거한다")
    void normalizeNames_deduplicatesAfterNormalization() {
        // when
        List<String> result = TagResolver.normalizeNames(List.of("Action", " action ", "ACTION"));

        // then
        assertThat(result).containsExactly("action");
    }

    @Test
    @DisplayName("빈 목록을 넣으면 예외 없이 빈 목록을 반환한다")
    void normalizeNames_emptyInput_returnsEmptyList() {
        // when
        List<String> result = TagResolver.normalizeNames(List.of());

        // then
        assertThat(result).isEmpty();
    }

    // --- resolve ---

    @Test
    @DisplayName("모두 존재하는 태그면 새로 생성하지 않고 그대로 반환한다")
    void resolve_allExisting_reusesWithoutSaving() {
        // given
        Tag actionTag = Tag.create("액션");
        Tag dramaTag = Tag.create("드라마");
        given(tagRepository.findByNameIn(List.of("액션", "드라마")))
                .willReturn(List.of(actionTag, dramaTag));

        // when
        Map<String, Tag> result = TagResolver.resolve(List.of("액션", "드라마"), tagRepository);

        // then
        assertThat(result).containsEntry("액션", actionTag).containsEntry("드라마", dramaTag);
        verify(tagRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("존재하지 않는 태그는 새로 생성하여 저장한다")
    void resolve_allNew_createsAndSavesAll() {
        // given
        given(tagRepository.findByNameIn(List.of("액션", "드라마"))).willReturn(List.of());
        given(tagRepository.saveAll(anyList())).willAnswer(invocation -> invocation.getArgument(0));

        // when
        Map<String, Tag> result = TagResolver.resolve(List.of("액션", "드라마"), tagRepository);

        // then
        ArgumentCaptor<List<Tag>> captor = ArgumentCaptor.forClass(List.class);
        verify(tagRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).extracting(Tag::getName).containsExactlyInAnyOrder("액션", "드라마");
        assertThat(result.keySet()).containsExactlyInAnyOrder("액션", "드라마");
    }

    @Test
    @DisplayName("기존 태그와 신규 태그가 섞여 있으면 신규 태그만 저장한다")
    void resolve_mixedExistingAndNew_savesOnlyNewOnes() {
        // given
        Tag existingTag = Tag.create("액션");
        given(tagRepository.findByNameIn(List.of("액션", "드라마"))).willReturn(List.of(existingTag));
        given(tagRepository.saveAll(anyList())).willAnswer(invocation -> invocation.getArgument(0));

        // when
        Map<String, Tag> result = TagResolver.resolve(List.of("액션", "드라마"), tagRepository);

        // then
        ArgumentCaptor<List<Tag>> captor = ArgumentCaptor.forClass(List.class);
        verify(tagRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).extracting(Tag::getName).containsExactly("드라마");
        assertThat(result).containsKeys("액션", "드라마");
        assertThat(result.get("액션")).isSameAs(existingTag);
    }

    @Test
    @DisplayName("태그 이름 목록이 비어 있으면 저장 없이 빈 맵을 반환한다")
    void resolve_emptyNames_returnsEmptyMap() {
        // given
        given(tagRepository.findByNameIn(List.of())).willReturn(List.of());

        // when
        Map<String, Tag> result = TagResolver.resolve(List.of(), tagRepository);

        // then
        assertThat(result).isEmpty();
        verify(tagRepository, never()).saveAll(anyList());
    }
}

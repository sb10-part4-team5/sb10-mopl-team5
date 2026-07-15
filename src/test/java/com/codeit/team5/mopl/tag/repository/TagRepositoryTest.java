package com.codeit.team5.mopl.tag.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.codeit.team5.mopl.global.support.base.BaseRepositoryTest;
import com.codeit.team5.mopl.tag.entity.Tag;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class TagRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private TagRepository tagRepository;

    @BeforeEach
    void setUp() {
        clear();
    }

    @Test
    @DisplayName("존재하지 않는 이름은 모두 새로 삽입한다")
    void insertIfAbsent_allMissing_insertsAll() {
        // when
        tagRepository.insertIfAbsent(List.of("액션", "드라마"));

        // then
        List<Tag> tags = tagRepository.findByNameIn(List.of("액션", "드라마"));
        assertThat(tags).extracting(Tag::getName).containsExactlyInAnyOrder("액션", "드라마");
    }

    @Test
    @DisplayName("이미 존재하는 이름이면 예외 없이 건너뛰고 기존 행을 그대로 둔다")
    void insertIfAbsent_existingName_skipsWithoutException() {
        // given
        Tag existing = persistAndFlush(Tag.create("액션"));

        // when
        tagRepository.insertIfAbsent(List.of("액션"));

        // then: 예외 없이 무시되고, 기존 행의 id가 그대로 유지된다
        Optional<Tag> found = tagRepository.findByName("액션");
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(existing.getId());
    }

    @Test
    @DisplayName("존재하는 이름과 없는 이름이 섞여 있으면 없는 이름만 삽입된다")
    void insertIfAbsent_mixedExistingAndMissing_insertsOnlyMissing() {
        // given
        persistAndFlush(Tag.create("액션"));

        // when
        tagRepository.insertIfAbsent(List.of("액션", "드라마"));

        // then
        List<Tag> tags = tagRepository.findByNameIn(List.of("액션", "드라마"));
        assertThat(tags).hasSize(2);
        assertThat(tags).extracting(Tag::getName).containsExactlyInAnyOrder("액션", "드라마");
    }

    @Test
    @DisplayName("여러 건을 한 번에 삽입해도 모두 반영된다")
    void insertIfAbsent_bulkInsert_allPersisted() {
        // when
        tagRepository.insertIfAbsent(List.of("코미디", "sf", "로맨스"));

        // then
        List<Tag> tags = tagRepository.findByNameIn(List.of("코미디", "sf", "로맨스"));
        assertThat(tags).hasSize(3);
    }
}

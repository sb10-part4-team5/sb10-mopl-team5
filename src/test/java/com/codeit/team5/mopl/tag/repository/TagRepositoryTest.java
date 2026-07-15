package com.codeit.team5.mopl.tag.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.codeit.team5.mopl.global.support.base.BaseRepositoryTest;
import com.codeit.team5.mopl.tag.entity.Tag;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
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
        // given
        UUID[] ids = {UUID.randomUUID(), UUID.randomUUID()};
        String[] names = {"액션", "드라마"};

        // when
        tagRepository.insertIfAbsent(ids, names);

        // then
        List<Tag> tags = tagRepository.findByNameIn(List.of("액션", "드라마"));
        assertThat(tags).extracting(Tag::getName).containsExactlyInAnyOrder("액션", "드라마");
    }

    @Test
    @DisplayName("이미 존재하는 이름이면 예외 없이 건너뛰고 기존 행을 그대로 둔다")
    void insertIfAbsent_existingName_skipsWithoutException() {
        // given
        Tag existing = persistAndFlush(Tag.create("액션"));

        // when: 같은 이름을 다른 id로 삽입 시도
        tagRepository.insertIfAbsent(new UUID[]{UUID.randomUUID()}, new String[]{"액션"});

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

        UUID[] ids = {UUID.randomUUID(), UUID.randomUUID()};
        String[] names = {"액션", "드라마"};

        // when
        tagRepository.insertIfAbsent(ids, names);

        // then
        List<Tag> tags = tagRepository.findByNameIn(List.of("액션", "드라마"));
        assertThat(tags).hasSize(2);
        assertThat(tags).extracting(Tag::getName).containsExactlyInAnyOrder("액션", "드라마");
    }

    @Test
    @DisplayName("여러 건을 한 번에 삽입해도 모두 반영된다")
    void insertIfAbsent_bulkInsert_allPersisted() {
        // given
        UUID[] ids = {UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()};
        String[] names = {"코미디", "sf", "로맨스"};

        // when
        tagRepository.insertIfAbsent(ids, names);

        // then
        List<Tag> tags = tagRepository.findByNameIn(List.of("코미디", "sf", "로맨스"));
        assertThat(tags).hasSize(3);
    }

    @Test
    @DisplayName("존재하는 이름과 없는 이름이 섞여 있으면 없는 이름만 생성해서 이름→태그 맵으로 돌려준다")
    void findOrCreateAllByName_mixedExistingAndMissing_returnsAllAsMap() {
        // given
        Tag existing = persistAndFlush(Tag.create("액션"));

        // when
        Map<String, Tag> result = tagRepository.findOrCreateAllByName(List.of("액션", "드라마"));

        // then
        assertThat(result).containsOnlyKeys("액션", "드라마");
        assertThat(result.get("액션").getId()).isEqualTo(existing.getId());
        assertThat(result.get("드라마").getId()).isNotNull();
        assertThat(tagRepository.findByName("드라마")).isPresent();
    }

    @Test
    @DisplayName("모두 존재하는 이름이면 새로 생성하지 않고 그대로 맵으로 돌려준다")
    void findOrCreateAllByName_allExisting_returnsWithoutInserting() {
        // given
        Tag actionTag = persistAndFlush(Tag.create("액션"));
        Tag dramaTag = persistAndFlush(Tag.create("드라마"));
        long countBefore = tagRepository.count();

        // when
        Map<String, Tag> result = tagRepository.findOrCreateAllByName(List.of("액션", "드라마"));

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get("액션").getId()).isEqualTo(actionTag.getId());
        assertThat(result.get("드라마").getId()).isEqualTo(dramaTag.getId());
        assertThat(tagRepository.count()).isEqualTo(countBefore);
    }

    @Test
    @DisplayName("중복된 이름이 섞여 있어도 태그는 한 번만 생성된다")
    void findOrCreateAllByName_duplicateNames_createsOnlyOnce() {
        // when
        Map<String, Tag> result = tagRepository.findOrCreateAllByName(List.of("코미디", "코미디"));

        // then
        assertThat(result).hasSize(1);
        List<Tag> tags = tagRepository.findByNameIn(List.of("코미디"));
        assertThat(tags).hasSize(1);
    }
}

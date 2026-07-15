package com.codeit.team5.mopl.tag.repository;

import com.codeit.team5.mopl.tag.entity.Tag;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TagRepository extends JpaRepository<Tag, UUID> {

    Optional<Tag> findByName(String name);

    List<Tag> findByNameIn(List<String> names);

    /**
     * 이름이 겹치는 태그는 건너뛰고 없는 태그만 삽입한다 (동시 요청 간 유니크 제약 충돌을 예외 없이 처리).
     */
    @Modifying
    @Query(value = """
            INSERT INTO tags (id, name)
            SELECT * FROM unnest(:ids, :names)
            ON CONFLICT (name) DO NOTHING
            """, nativeQuery = true)
    void insertIfAbsent(@Param("ids") UUID[] ids, @Param("names") String[] names);

    /**
     * 이름 목록에 대해 이미 존재하는 태그는 재사용하고, 없는 태그는 원자적으로 생성해서 이름→태그 맵으로 돌려준다.
     *
     * @param tagNames 정규화(trim/lowercase/dedup)된 태그 이름 목록이어야 한다
     */
    default Map<String, Tag> findOrCreateAllByName(List<String> tagNames) {
        List<String> uniqueNames = tagNames.stream().distinct().toList();

        Map<String, Tag> existingTags = findByNameIn(uniqueNames).stream()
                .collect(Collectors.toMap(Tag::getName, Function.identity()));

        List<String> missingNames = uniqueNames.stream()
                .filter(name -> !existingTags.containsKey(name))
                .toList();

        if (!missingNames.isEmpty()) {
            missingNames.forEach(Tag::create); // 이름 유효성만 검증 (엔티티 자체는 사용하지 않음)

            // Tag.id는 GenerationType.UUID라 persist 시점에 채워지므로, 네이티브 insert에 쓸 값은 직접 생성한다.
            UUID[] ids = missingNames.stream().map(name -> UUID.randomUUID()).toArray(UUID[]::new);
            String[] names = missingNames.toArray(String[]::new);
            insertIfAbsent(ids, names);

            // 동시 요청이 먼저 삽입했을 수 있으므로 삽입을 시도한 이름 기준으로 다시 조회한다.
            findByNameIn(missingNames).forEach(tag -> existingTags.put(tag.getName(), tag));
        }

        return existingTags;
    }
}

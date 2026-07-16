package com.codeit.team5.mopl.tag.repository;

import com.codeit.team5.mopl.tag.entity.Tag;
import com.codeit.team5.mopl.tag.repository.custom.TagRepositoryCustom;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TagRepository extends JpaRepository<Tag, UUID>, TagRepositoryCustom {

    Optional<Tag> findByName(String name);

    List<Tag> findByNameIn(List<String> names);
}

package com.codeit.team5.mopl.tag.repository.custom;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class TagRepositoryImpl implements TagRepositoryCustom {

    private final EntityManager em;

    @Override
    public void insertIfAbsent(List<String> names) {
        if (names.isEmpty()) {
            return;
        }

        String values = names.stream()
                .map(name -> "(gen_random_uuid(), ?)")
                .collect(Collectors.joining(","));

        Query query = em.createNativeQuery(
                "INSERT INTO tags (id, name) VALUES " + values + " ON CONFLICT (name) DO NOTHING");
        for (int i = 0; i < names.size(); i++) {
            query.setParameter(i + 1, names.get(i));
        }
        query.executeUpdate();
    }
}

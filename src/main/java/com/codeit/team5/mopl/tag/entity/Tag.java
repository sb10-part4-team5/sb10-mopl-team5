package com.codeit.team5.mopl.tag.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import com.codeit.team5.mopl.tag.exception.InvalidTagNameException;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "tags")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    public static Tag create(String name) {
        if (name == null || name.isBlank()) {
            throw new InvalidTagNameException("태그 이름은 필수입니다.");
        }
        if (name.length() > 50) {
            throw new InvalidTagNameException("태그 이름은 50자를 초과할 수 없습니다.");
        }
        Tag tag = new Tag();
        tag.name = name;
        return tag;
    }
}

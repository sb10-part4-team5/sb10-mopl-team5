package com.codeit.team5.mopl.content.document;

import com.codeit.team5.mopl.content.entity.ContentType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Getter
@Builder
@Document(indexName = "contents")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ContentDocument {

    @Id
    private String id;

    @Field(type = FieldType.Keyword)
    private ContentType type;

    @Field(type = FieldType.Text, analyzer = "nori")
    private String title;

    @Field(type = FieldType.Text, analyzer = "nori")
    private String description;

    // 응답에만 싣고 검색/필터/정렬에 쓰지 않아 색인하지 않는다.
    @Field(type = FieldType.Keyword, index = false)
    private String thumbnailUrl;

    @Field(type = FieldType.Keyword)
    private List<String> tags;

    @Field(type = FieldType.Date, format = DateFormat.date_time)
    private Instant createdAt;

    @Field(type = FieldType.Long)
    private long watcherCount;

    @Field(type = FieldType.Double)
    private double averageRating;

    @Field(type = FieldType.Integer)
    private int reviewCount;

    public static String toDocumentId(UUID contentId) {
        return contentId.toString();
    }
}

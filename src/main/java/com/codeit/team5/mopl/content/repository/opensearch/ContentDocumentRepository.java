package com.codeit.team5.mopl.content.repository.opensearch;

import com.codeit.team5.mopl.content.document.ContentDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface ContentDocumentRepository extends ElasticsearchRepository<ContentDocument, String> {
}

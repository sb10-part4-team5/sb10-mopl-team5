package com.codeit.team5.mopl.content.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import com.codeit.team5.mopl.TestcontainersConfiguration;
import com.codeit.team5.mopl.binarycontent.entity.BinaryContent;
import com.codeit.team5.mopl.binarycontent.entity.BinaryContentUploadStatus;
import com.codeit.team5.mopl.binarycontent.repository.BinaryContentRepository;
import com.codeit.team5.mopl.binarycontent.storage.BinaryContentStorage;
import com.codeit.team5.mopl.content.dto.request.ContentCreateRequest;
import com.codeit.team5.mopl.content.dto.request.ContentUpdateRequest;
import com.codeit.team5.mopl.content.dto.response.ContentResponse;
import com.codeit.team5.mopl.content.entity.Content;
import com.codeit.team5.mopl.content.entity.ContentType;
import com.codeit.team5.mopl.content.exception.EmptyTagException;
import com.codeit.team5.mopl.content.repository.ContentRepository;
import com.codeit.team5.mopl.global.dto.FileRequest;
import com.codeit.team5.mopl.tag.repository.TagRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * ContentFacade 통합 테스트.
 *
 * <p>ContentController 통합 테스트에서 다루지 않은 <b>썸네일 업로드 및 실패 시 롤백</b> 경로를 검증한다.
 * ContentFacade → UploadWithRollback → BinaryContentService → ContentService 전체 흐름을 실제 빈으로
 * 주입하고, 외부 S3 스토리지({@link BinaryContentStorage})만 목으로 대체한다.</p>
 *
 * <p>롤백(업로드 후 후속 작업 실패 시 스토리지 객체 삭제)은 서비스의 트랜잭션 경계와 비동기 이벤트에
 * 의존하므로, 테스트 트랜잭션이 이를 삼키지 않도록 클래스에 {@code @Transactional}을 두지 않고
 * {@link AfterEach}로 정리한다.</p>
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class ContentFacadeIntegrationTest {

    private static final String UPLOADED_URL = "http://localhost/thumbnails/uploaded.jpg";

    @Autowired
    private ContentFacade contentFacade;

    @Autowired
    private ContentRepository contentRepository;

    @Autowired
    private BinaryContentRepository binaryContentRepository;

    @Autowired
    private TagRepository tagRepository;

    // 실제 S3 호출을 막기 위해 스토리지 계층만 목으로 대체한다.
    @MockitoBean
    private BinaryContentStorage binaryContentStorage;

    @AfterEach
    void cleanUp() {
        contentRepository.deleteAll();
        binaryContentRepository.deleteAll();
        tagRepository.deleteAll();
    }

    private ContentCreateRequest createRequest(List<String> tags) {
        return new ContentCreateRequest(ContentType.MOVIE, "테스트 영화", "설명", tags);
    }

    private FileRequest image(String filename) {
        return new FileRequest(new byte[]{1, 2, 3}, filename);
    }

    @Test
    @DisplayName("썸네일과 함께 생성하면 스토리지에 업로드되고 COMPLETED 썸네일이 콘텐츠에 연결된다")
    void create_withThumbnail_uploadsAndAttaches() {
        // given
        given(binaryContentStorage.toUrl(anyString())).willReturn(UPLOADED_URL);

        // when
        ContentResponse response = contentFacade.create(createRequest(List.of("액션")), image("poster.jpg"));

        // then
        assertThat(response.thumbnailUrl()).isEqualTo(UPLOADED_URL);
        verify(binaryContentStorage).store(anyString(), any(), any());

        Content saved = contentRepository.findWithStatsAndTagsById(response.id()).orElseThrow();
        assertThat(saved.getThumbnail()).isNotNull();
        assertThat(saved.getThumbnail().getUploadStatus()).isEqualTo(BinaryContentUploadStatus.COMPLETED);
        assertThat(saved.getThumbnail().getUrl()).isEqualTo(UPLOADED_URL);
    }

    @Test
    @DisplayName("썸네일 없이 생성하면 스토리지를 호출하지 않고 thumbnailUrl이 null이다")
    void create_withoutThumbnail_noStorageInteraction() {
        // when
        ContentResponse response = contentFacade.create(createRequest(List.of("액션")), null);

        // then
        assertThat(response.thumbnailUrl()).isNull();
        verify(binaryContentStorage, never()).store(anyString(), any(), any());

        Content saved = contentRepository.findWithStatsAndTagsById(response.id()).orElseThrow();
        assertThat(saved.getThumbnail()).isNull();
    }

    @Test
    @DisplayName("수정 시 새 썸네일을 올리면 기존 썸네일은 DELETED로 마킹되고 새 썸네일이 연결된다")
    void update_withNewThumbnail_replacesOld() {
        // given
        given(binaryContentStorage.toUrl(anyString())).willReturn(UPLOADED_URL);
        ContentResponse created = contentFacade.create(createRequest(List.of("액션")), image("old.jpg"));
        ContentUpdateRequest updateRequest = new ContentUpdateRequest("수정된 영화", "수정된 설명", List.of("액션"));

        // when
        contentFacade.update(created.id(), updateRequest, image("new.jpg"));

        // then
        Content updated = contentRepository.findWithStatsAndTagsById(created.id()).orElseThrow();
        assertThat(updated.getThumbnail().getUploadStatus()).isEqualTo(BinaryContentUploadStatus.COMPLETED);
        // 기존 + 신규 = 2개의 BinaryContent, 그중 정확히 1개가 DELETED로 마킹되어야 한다
        List<BinaryContent> all = binaryContentRepository.findAll();
        assertThat(all).hasSize(2);
        assertThat(all).filteredOn(bc -> bc.getUploadStatus() == BinaryContentUploadStatus.DELETED).hasSize(1);
        assertThat(all).filteredOn(bc -> bc.getUploadStatus() == BinaryContentUploadStatus.COMPLETED).hasSize(1);
    }

    @Test
    @DisplayName("업로드 후 후속 작업이 실패하면 콘텐츠가 저장되지 않고 업로드된 스토리지 객체가 롤백 삭제된다")
    void create_persistFailsAfterUpload_rollsBackStorage() {
        // given - 업로드는 성공하지만 태그가 비어 콘텐츠 생성 단계에서 EmptyTagException이 발생한다
        given(binaryContentStorage.toUrl(anyString())).willReturn(UPLOADED_URL);

        // when & then
        assertThatThrownBy(() -> contentFacade.create(createRequest(List.of()), image("poster.jpg")))
                .isInstanceOf(EmptyTagException.class);

        // 업로드는 시도되었고
        verify(binaryContentStorage).store(anyString(), any(), any());
        // 후속 작업 실패로 비동기 롤백 삭제가 호출되어야 한다
        verify(binaryContentStorage, timeout(3000)).delete(anyString());
        // 콘텐츠와 BinaryContent는 트랜잭션 롤백으로 저장되지 않아야 한다
        assertThat(contentRepository.count()).isZero();
        assertThat(binaryContentRepository.count()).isZero();
    }

    @Test
    @DisplayName("존재하지 않는 콘텐츠를 썸네일과 함께 수정하면 예외가 발생하고 업로드가 롤백된다")
    void update_notFound_rollsBackStorage() {
        // given
        given(binaryContentStorage.toUrl(anyString())).willReturn(UPLOADED_URL);
        ContentUpdateRequest updateRequest = new ContentUpdateRequest("수정", null, List.of("액션"));

        // when & then
        assertThatThrownBy(() -> contentFacade.update(UUID.randomUUID(), updateRequest, image("poster.jpg")))
                .isInstanceOf(RuntimeException.class);

        verify(binaryContentStorage).store(anyString(), any(), any());
        verify(binaryContentStorage, timeout(3000)).delete(anyString());
        assertThat(binaryContentRepository.count()).isZero();
    }
}

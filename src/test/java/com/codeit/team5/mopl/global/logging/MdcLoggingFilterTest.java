package com.codeit.team5.mopl.global.logging;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@DisplayName("MdcLoggingFilter 단위 테스트")
class MdcLoggingFilterTest {

    private final MdcLoggingFilter filter = new MdcLoggingFilter();

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Nested
    @DisplayName("requestId 처리")
    class RequestId {

        @Test
        @DisplayName("X-Request-Id 헤더가 없으면 새 UUID를 생성하고 응답 헤더에 담기 성공")
        void generatesNewIdWhenHeaderMissing() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();
            CapturingFilterChain chain = new CapturingFilterChain();

            filter.doFilter(request, response, chain);

            assertThat(chain.called).isTrue();
            assertThat(chain.requestId).isNotBlank();
            assertThat(isUuid(chain.requestId)).isTrue();
            // MDC 값과 응답 헤더 값이 동일해야 한다
            assertThat(response.getHeader(MdcKey.REQUEST_ID_HEADER)).isEqualTo(chain.requestId);
        }

        @Test
        @DisplayName("유효한 X-Request-Id 헤더가 들어오면 재사용 성공")
        void reusesValidIncomingId() throws Exception {
            String incoming = "abc-123-DEF";
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader(MdcKey.REQUEST_ID_HEADER, incoming);
            MockHttpServletResponse response = new MockHttpServletResponse();
            CapturingFilterChain chain = new CapturingFilterChain();

            filter.doFilter(request, response, chain);

            assertThat(chain.requestId).isEqualTo(incoming);
            assertThat(response.getHeader(MdcKey.REQUEST_ID_HEADER)).isEqualTo(incoming);
        }

        @Test
        @DisplayName("안전하지 않은 문자(로그 인젝션)가 포함된 ID는 무시하고 새로 새로 생성 성공")
        void regeneratesWhenIncomingIdHasUnsafeChars() throws Exception {
            String malicious = "bad\nINJECTED-LOG";
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader(MdcKey.REQUEST_ID_HEADER, malicious);
            MockHttpServletResponse response = new MockHttpServletResponse();
            CapturingFilterChain chain = new CapturingFilterChain();

            filter.doFilter(request, response, chain);

            assertThat(chain.requestId).isNotEqualTo(malicious);
            assertThat(isUuid(chain.requestId)).isTrue();
        }

        @Test
        @DisplayName("최대 길이(64자)를 초과한 ID는 무시하고 새로 생성 성공")
        void regeneratesWhenIncomingIdTooLong() throws Exception {
            String tooLong = "a".repeat(65);
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader(MdcKey.REQUEST_ID_HEADER, tooLong);
            MockHttpServletResponse response = new MockHttpServletResponse();
            CapturingFilterChain chain = new CapturingFilterChain();

            filter.doFilter(request, response, chain);

            assertThat(chain.requestId).isNotEqualTo(tooLong);
            assertThat(isUuid(chain.requestId)).isTrue();
        }
    }

    @Nested
    @DisplayName("clientIp 처리")
    class ClientIp {

        @Test
        @DisplayName("remoteAddr을 clientIp로 사용 성공")
        void usesRemoteAddr() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRemoteAddr("192.168.0.1");
            MockHttpServletResponse response = new MockHttpServletResponse();
            CapturingFilterChain chain = new CapturingFilterChain();

            filter.doFilter(request, response, chain);

            assertThat(chain.clientIp).isEqualTo("192.168.0.1");
        }

        @Test
        @DisplayName("clientIp는 응답 헤더로 노출하지 않기 성공 (로그/MDC 전용)")
        void doesNotExposeClientIpInResponseHeader() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRemoteAddr("192.168.0.1");
            MockHttpServletResponse response = new MockHttpServletResponse();
            CapturingFilterChain chain = new CapturingFilterChain();

            filter.doFilter(request, response, chain);

            assertThat(response.getHeaderNames()).containsExactly(MdcKey.REQUEST_ID_HEADER);
        }
    }

    @Nested
    @DisplayName("MDC 정리")
    class MdcCleanup {

        @Test
        @DisplayName("요청 처리 도중에는 MDC에 값이 채우기 성공")
        void mdcPopulatedDuringRequest() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRemoteAddr("192.168.0.1");
            MockHttpServletResponse response = new MockHttpServletResponse();
            CapturingFilterChain chain = new CapturingFilterChain();

            filter.doFilter(request, response, chain);

            assertThat(chain.requestId).isNotBlank();
            assertThat(chain.clientIp).isEqualTo("192.168.0.1");
        }

        @Test
        @DisplayName("요청 처리 후 MDC가 비워짐 성공 (스레드 재사용 시 오염 방지)")
        void mdcClearedAfterRequest() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRemoteAddr("192.168.0.1");
            MockHttpServletResponse response = new MockHttpServletResponse();
            CapturingFilterChain chain = new CapturingFilterChain();

            filter.doFilter(request, response, chain);

            assertThat(MDC.get(MdcKey.REQUEST_ID)).isNull();
            assertThat(MDC.get(MdcKey.CLIENT_IP)).isNull();
        }
    }

    private static boolean isUuid(String value) {
        try {
            UUID.fromString(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 필터 체인이 호출되는 시점(= MDC가 채워진 시점)의 MDC 값을 캡처하는 테스트용 FilterChain.
     * MDC 값은 필터 종료 후 clear되므로, 체인 실행 중에 확인
     */
    private static class CapturingFilterChain implements FilterChain {

        private boolean called;
        private String requestId;
        private String clientIp;

        @Override
        public void doFilter(ServletRequest request, ServletResponse response)
            throws IOException, ServletException {
            this.called = true;
            this.requestId = MDC.get(MdcKey.REQUEST_ID);
            this.clientIp = MDC.get(MdcKey.CLIENT_IP);
        }
    }
}

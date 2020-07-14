package uk.gov.hmcts.reform.fpl.request;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.servlet.http.HttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
class RequestDataTest {
    private static final String AUTH_TOKEN = "Bearer token";
    private static final String CACHED_AUTH_TOKEN = "Cached bearer token";
    private static final String USER_ID = "example.gov.uk";
    private static final String CACHED_USER_ID = "cached.gov.uk";

    @Mock
    private HttpServletRequest httpServletRequest;

    @InjectMocks
    private CacheAwareRequestData requestData;

    @Nested
    class NotCachedRequestData {

        @Test
        void shouldReturnAuthorisationHeaderFromRequest() {
            when(httpServletRequest.getHeader("authorization")).thenReturn(AUTH_TOKEN);
            assertThat(requestData.authorisation()).isEqualTo(AUTH_TOKEN);
        }

        @Test
        void shouldReturnUserIdFromRequest() {
            when(httpServletRequest.getHeader("user-id")).thenReturn(USER_ID);
            assertThat(requestData.userId()).isEqualTo(USER_ID);
        }
    }

    @Nested
    class CachedRequestData {

        final SimpleRequestData cachedRequestData = new SimpleRequestData(CACHED_AUTH_TOKEN, CACHED_USER_ID);

        @BeforeEach
        void setup() {
            RequestDataCache.add(cachedRequestData);
        }

        @AfterEach
        void cleanUp() {
            RequestDataCache.remove();
        }

        @Test
        void shouldReturnAuthorisationHeaderFromCache() {
            assertThat(requestData.authorisation()).isEqualTo(CACHED_AUTH_TOKEN);
            verifyNoInteractions(httpServletRequest);
        }

        @Test
        void shouldReturnUserIdFromCache() {
            assertThat(requestData.userId()).isEqualTo(CACHED_USER_ID);
            verifyNoInteractions(httpServletRequest);
        }
    }
}

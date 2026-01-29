package com.locallab.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.servlet.config.annotation.CorsRegistration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

/**
 * Unit tests for {@link WebConfig}.
 *
 * <p>Tests CORS configuration for API endpoints and H2 console access. Verifies that CORS settings
 * are correctly loaded from {@link CorsProperties} and applied to the appropriate URL patterns.
 *
 * @author William Stephen
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("WebConfig")
class WebConfigTest {

    @Mock private CorsProperties corsProperties;

    @Mock private CorsRegistry corsRegistry;

    @Mock private CorsRegistration apiCorsRegistration;

    @Mock private CorsRegistration h2CorsRegistration;

    @Captor private ArgumentCaptor<String[]> originsCaptor;

    @Captor private ArgumentCaptor<String[]> methodsCaptor;

    @Captor private ArgumentCaptor<String[]> headersCaptor;

    private WebConfig webConfig;

    @BeforeEach
    void setUp() {
        webConfig = new WebConfig(corsProperties);
    }

    @Nested
    @DisplayName("addCorsMappings")
    class AddCorsMappingsTests {

        @BeforeEach
        void setUpMocks() {
            lenient().when(corsProperties.getAllowedOrigins()).thenReturn("http://localhost:5173");
            lenient()
                    .when(corsProperties.getAllowedMethods())
                    .thenReturn("GET,POST,PUT,DELETE,PATCH,OPTIONS");
            lenient().when(corsProperties.getAllowedHeaders()).thenReturn("*");
            lenient().when(corsProperties.isAllowCredentials()).thenReturn(true);
            lenient().when(corsProperties.getMaxAge()).thenReturn(3600L);

            lenient().when(corsRegistry.addMapping("/api/**")).thenReturn(apiCorsRegistration);
            lenient()
                    .when(corsRegistry.addMapping("/h2-console/**"))
                    .thenReturn(h2CorsRegistration);

            lenient()
                    .when(apiCorsRegistration.allowedOrigins(any(String[].class)))
                    .thenReturn(apiCorsRegistration);
            lenient()
                    .when(apiCorsRegistration.allowedMethods(any(String[].class)))
                    .thenReturn(apiCorsRegistration);
            lenient()
                    .when(apiCorsRegistration.allowedHeaders(any(String[].class)))
                    .thenReturn(apiCorsRegistration);
            lenient()
                    .when(apiCorsRegistration.allowCredentials(anyBoolean()))
                    .thenReturn(apiCorsRegistration);
            lenient().when(apiCorsRegistration.maxAge(anyLong())).thenReturn(apiCorsRegistration);

            lenient()
                    .when(h2CorsRegistration.allowedOrigins(any(String[].class)))
                    .thenReturn(h2CorsRegistration);
            lenient()
                    .when(h2CorsRegistration.allowedMethods(any(String[].class)))
                    .thenReturn(h2CorsRegistration);
            lenient()
                    .when(h2CorsRegistration.allowedHeaders(any(String[].class)))
                    .thenReturn(h2CorsRegistration);
            lenient()
                    .when(h2CorsRegistration.allowCredentials(anyBoolean()))
                    .thenReturn(h2CorsRegistration);
        }

        @Test
        @DisplayName("should configure CORS for API endpoints")
        void shouldConfigureCorsForApiEndpoints() {
            webConfig.addCorsMappings(corsRegistry);

            verify(corsRegistry).addMapping("/api/**");
            verify(apiCorsRegistration).allowCredentials(true);
            verify(apiCorsRegistration).maxAge(3600L);
        }

        @Test
        @DisplayName("should configure CORS for H2 console")
        void shouldConfigureCorsForH2Console() {
            webConfig.addCorsMappings(corsRegistry);

            verify(corsRegistry).addMapping("/h2-console/**");
            verify(h2CorsRegistration).allowCredentials(true);
        }

        @Test
        @DisplayName("should parse comma-separated allowed origins")
        void shouldParseCommaSeparatedAllowedOrigins() {
            when(corsProperties.getAllowedOrigins())
                    .thenReturn("http://localhost:5173,http://localhost:3000");
            when(apiCorsRegistration.allowedOrigins(originsCaptor.capture()))
                    .thenReturn(apiCorsRegistration);

            webConfig.addCorsMappings(corsRegistry);

            String[] capturedOrigins = originsCaptor.getValue();
            assertThat(capturedOrigins)
                    .containsExactly("http://localhost:5173", "http://localhost:3000");
        }

        @Test
        @DisplayName("should parse comma-separated allowed methods")
        void shouldParseCommaSeparatedAllowedMethods() {
            when(apiCorsRegistration.allowedMethods(methodsCaptor.capture()))
                    .thenReturn(apiCorsRegistration);

            webConfig.addCorsMappings(corsRegistry);

            String[] capturedMethods = methodsCaptor.getValue();
            assertThat(capturedMethods)
                    .containsExactly("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS");
        }

        @Test
        @DisplayName("should parse comma-separated allowed headers")
        void shouldParseCommaSeparatedAllowedHeaders() {
            when(corsProperties.getAllowedHeaders()).thenReturn("Content-Type,Authorization");
            when(apiCorsRegistration.allowedHeaders(headersCaptor.capture()))
                    .thenReturn(apiCorsRegistration);

            webConfig.addCorsMappings(corsRegistry);

            String[] capturedHeaders = headersCaptor.getValue();
            assertThat(capturedHeaders).containsExactly("Content-Type", "Authorization");
        }

        @Test
        @DisplayName("should handle wildcard for allowed headers")
        void shouldHandleWildcardForAllowedHeaders() {
            when(apiCorsRegistration.allowedHeaders(headersCaptor.capture()))
                    .thenReturn(apiCorsRegistration);

            webConfig.addCorsMappings(corsRegistry);

            String[] capturedHeaders = headersCaptor.getValue();
            assertThat(capturedHeaders).containsExactly("*");
        }

        @Test
        @DisplayName("should use credentials setting from properties")
        void shouldUseCredentialsSettingFromProperties() {
            when(corsProperties.isAllowCredentials()).thenReturn(false);

            webConfig.addCorsMappings(corsRegistry);

            verify(apiCorsRegistration).allowCredentials(false);
            verify(h2CorsRegistration).allowCredentials(false);
        }

        @Test
        @DisplayName("should use max age setting from properties")
        void shouldUseMaxAgeSettingFromProperties() {
            when(corsProperties.getMaxAge()).thenReturn(7200L);

            webConfig.addCorsMappings(corsRegistry);

            verify(apiCorsRegistration).maxAge(7200L);
        }
    }

    @Nested
    @DisplayName("constructor")
    class ConstructorTests {

        @Test
        @DisplayName("should accept CorsProperties dependency")
        void shouldAcceptCorsPropertiesDependency() {
            WebConfig config = new WebConfig(corsProperties);

            assertThat(config).isNotNull();
        }
    }
}

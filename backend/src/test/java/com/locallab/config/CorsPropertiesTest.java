package com.locallab.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link CorsProperties}.
 *
 * <p>Tests all getter and setter methods for CORS configuration properties including allowed
 * origins, methods, headers, credentials support, and preflight cache duration.
 *
 * @author William Stephen
 */
@DisplayName("CorsProperties")
class CorsPropertiesTest {

    private CorsProperties corsProperties;

    @BeforeEach
    void setUp() {
        corsProperties = new CorsProperties();
    }

    @Nested
    @DisplayName("default values")
    class DefaultValuesTests {

        @Test
        @DisplayName("should have default allowed origins for localhost:5173")
        void shouldHaveDefaultAllowedOrigins() {
            assertThat(corsProperties.getAllowedOrigins()).isEqualTo("http://localhost:5173");
        }

        @Test
        @DisplayName("should have default allowed methods")
        void shouldHaveDefaultAllowedMethods() {
            assertThat(corsProperties.getAllowedMethods())
                    .isEqualTo("GET,POST,PUT,DELETE,PATCH,OPTIONS");
        }

        @Test
        @DisplayName("should have default allowed headers as wildcard")
        void shouldHaveDefaultAllowedHeaders() {
            assertThat(corsProperties.getAllowedHeaders()).isEqualTo("*");
        }

        @Test
        @DisplayName("should have credentials enabled by default")
        void shouldHaveCredentialsEnabledByDefault() {
            assertThat(corsProperties.isAllowCredentials()).isTrue();
        }

        @Test
        @DisplayName("should have default max age of 3600 seconds")
        void shouldHaveDefaultMaxAge() {
            assertThat(corsProperties.getMaxAge()).isEqualTo(3600L);
        }
    }

    @Nested
    @DisplayName("setters and getters")
    class SettersAndGettersTests {

        @Test
        @DisplayName("should set and get allowed origins")
        void shouldSetAndGetAllowedOrigins() {
            corsProperties.setAllowedOrigins("http://example.com,http://localhost:3000");

            assertThat(corsProperties.getAllowedOrigins())
                    .isEqualTo("http://example.com,http://localhost:3000");
        }

        @Test
        @DisplayName("should set and get allowed methods")
        void shouldSetAndGetAllowedMethods() {
            corsProperties.setAllowedMethods("GET,POST");

            assertThat(corsProperties.getAllowedMethods()).isEqualTo("GET,POST");
        }

        @Test
        @DisplayName("should set and get allowed headers")
        void shouldSetAndGetAllowedHeaders() {
            corsProperties.setAllowedHeaders("Content-Type,Authorization");

            assertThat(corsProperties.getAllowedHeaders()).isEqualTo("Content-Type,Authorization");
        }

        @Test
        @DisplayName("should set and get allow credentials")
        void shouldSetAndGetAllowCredentials() {
            corsProperties.setAllowCredentials(false);

            assertThat(corsProperties.isAllowCredentials()).isFalse();
        }

        @Test
        @DisplayName("should set and get max age")
        void shouldSetAndGetMaxAge() {
            corsProperties.setMaxAge(7200L);

            assertThat(corsProperties.getMaxAge()).isEqualTo(7200L);
        }
    }

    @Nested
    @DisplayName("wildcard origin configuration")
    class WildcardOriginTests {

        @Test
        @DisplayName("should allow wildcard for development profile")
        void shouldAllowWildcardForDevelopmentProfile() {
            corsProperties.setAllowedOrigins("*");

            assertThat(corsProperties.getAllowedOrigins()).isEqualTo("*");
        }

        @Test
        @DisplayName("should disable credentials when using wildcard origins")
        void shouldDisableCredentialsWhenUsingWildcardOrigins() {
            corsProperties.setAllowedOrigins("*");
            corsProperties.setAllowCredentials(false);

            assertThat(corsProperties.getAllowedOrigins()).isEqualTo("*");
            assertThat(corsProperties.isAllowCredentials()).isFalse();
        }
    }

    @Nested
    @DisplayName("production configuration")
    class ProductionConfigurationTests {

        @Test
        @DisplayName("should support restricted origins for production")
        void shouldSupportRestrictedOriginsForProduction() {
            corsProperties.setAllowedOrigins("https://production.example.com");
            corsProperties.setAllowCredentials(true);

            assertThat(corsProperties.getAllowedOrigins())
                    .isEqualTo("https://production.example.com");
            assertThat(corsProperties.isAllowCredentials()).isTrue();
        }

        @Test
        @DisplayName("should support multiple production origins")
        void shouldSupportMultipleProductionOrigins() {
            corsProperties.setAllowedOrigins("https://app.example.com,https://admin.example.com");

            assertThat(corsProperties.getAllowedOrigins())
                    .isEqualTo("https://app.example.com,https://admin.example.com");
        }
    }
}

package com.locallab.model.enums;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link RunStatus} enum. */
class RunStatusTest {

    @Test
    @DisplayName("Should contain all expected status values")
    void shouldContainAllExpectedStatusValues() {
        Set<String> expectedValues = Set.of("PENDING", "RUNNING", "SUCCESS", "FAILED");
        Set<String> actualValues =
                Arrays.stream(RunStatus.values()).map(Enum::name).collect(Collectors.toSet());

        assertEquals(expectedValues, actualValues);
    }
}

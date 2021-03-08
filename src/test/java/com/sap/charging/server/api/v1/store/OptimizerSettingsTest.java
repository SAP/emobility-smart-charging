package com.sap.charging.server.api.v1.store;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.ValueInstantiationException;

class OptimizerSettingsTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    public void setup() {
        this.objectMapper = new ObjectMapper();
    }

    private static Stream<Arguments> deserializeOptimizerSettingsValues() {
        return Stream.of(
            arguments("Null value", "{\"weightObjectiveFairShare\": null}", false),
            arguments("Negative value", "{\"weightObjectiveFairShare\": -1}", true),
            arguments("Valid JSON object (zero weight)", "{\"weightObjectiveFairShare\": 0}", false),
            arguments("Valid JSON object (positive weight)", "{\"weightObjectiveFairShare\": 1}", false));
    }

    @ParameterizedTest
    @MethodSource("deserializeOptimizerSettingsValues")
    @DisplayName("Build strategy based on incoming API request")
    void deserializeOptimizerSettings(String name, String json, boolean throwsException) {
        if (throwsException) {
            assertThrows(ValueInstantiationException.class, () -> objectMapper.readValue(json, OptimizerSettings.class));
        } else {
            assertDoesNotThrow(() -> objectMapper.readValue(json, OptimizerSettings.class));
        }
    }

}

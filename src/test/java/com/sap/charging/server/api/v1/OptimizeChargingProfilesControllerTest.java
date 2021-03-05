package com.sap.charging.server.api.v1;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.sap.charging.realTime.StrategyAlgorithmic;
import com.sap.charging.realTime.util.TimeslotSortingCriteria;
import com.sap.charging.server.api.v1.store.OptimizerSettings;


@WebMvcTest(OptimizeChargingProfilesController.class)
@ExtendWith(SpringExtension.class)
class OptimizeChargingProfilesControllerTest {

	private OptimizeChargingProfilesController classUnderTest; 

	@BeforeEach
	void setup() {
		this.classUnderTest = new OptimizeChargingProfilesController();
	}
	
	private static Stream<Arguments> buildStrategyValues() {
        return Stream.of(
            arguments("Default settings", OptimizerSettings.getDefaultOptimizerSettings(), TimeslotSortingCriteria.INDEX),
            arguments("Empty settings with no weights", new OptimizerSettings(0, 0, 0, 0), TimeslotSortingCriteria.INDEX),
            arguments("Settings with highest objective: fair share", new OptimizerSettings(1, 0, 0, 0), TimeslotSortingCriteria.INDEX),
            arguments("Settings with highest objective: peak shaving", new OptimizerSettings(1, 10, 0, 0), TimeslotSortingCriteria.PEAK_DEMAND), 
            arguments("Settings with highest objective: energy costs", new OptimizerSettings(1, 10, 100, 0), TimeslotSortingCriteria.PRICE));
    }
	
	@ParameterizedTest
	@MethodSource("buildStrategyValues")
    @DisplayName("Build strategy based on incoming API request")
	void buildStrategy(String name, OptimizerSettings settings, TimeslotSortingCriteria expectedSortingCritera) {
		StrategyAlgorithmic strategy = (StrategyAlgorithmic) this.classUnderTest.buildStrategy(settings);
		assertEquals(expectedSortingCritera, strategy.getSortingCriteriaByObjective());
	}

}

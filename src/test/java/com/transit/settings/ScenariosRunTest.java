package com.transit.settings;

import com.transit.engine.Simulation;
import com.transit.map.MapGenerator;
import com.transit.model.CityMap;
import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Chaque scénario fourni aux étudiants doit construire sa ville et tourner. */
class ScenariosRunTest {

    private static final int TICKS = 300;

    @Test
    void everyBundledScenarioBuildsItsCityAndRuns() {
        for (String scenario : Scenarios.names()) {
            Settings settings = Scenarios.load(scenario);
            CityMap map = MapGenerator.generate(settings.city(), settings.seed());
            assertTrue(map.busLines().size() >= 1, scenario + ": no bus line");
            Simulation simulation = new Simulation(map, settings);
            for (int t = 0; t < TICKS; t++) {
                simulation.tick();
            }
            assertEquals(TICKS, simulation.snapshot().tick(), scenario);
        }
    }
}

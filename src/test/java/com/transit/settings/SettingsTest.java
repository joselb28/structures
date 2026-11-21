package com.transit.settings;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SettingsTest {

    @Test
    void defaultsAreReadFromTheBundledScenario() {
        Settings settings = Settings.defaults();
        assertEquals(42, settings.seed());
        assertEquals(5, settings.fleet().busCapacity());
        assertEquals(0.4, settings.customers().terminusShare());
    }

    @Test
    void aScenarioOverridesOnlyItsKeys(@TempDir Path dir) throws IOException {
        Path scenario = Files.writeString(dir.resolve("s.properties"), "fleet.busCapacity = 12\n");
        Settings settings = Settings.load(scenario);
        assertEquals(12, settings.fleet().busCapacity());
        assertEquals(Settings.defaults().city(), settings.city());
    }

    @Test
    void outOfRangeValuesAreRefusedWithTheKeyName(@TempDir Path dir) throws IOException {
        Path scenario = Files.writeString(dir.resolve("s.properties"), "customers.terminusShare = 1.5\n");
        var error = assertThrows(IllegalArgumentException.class, () -> Settings.load(scenario));
        assertTrue(error.getMessage().contains("customers.terminusShare"), error.getMessage());
    }

    @Test
    void everyBundledScenarioLoadsAndHasADescription() {
        for (String name : Scenarios.names()) {
            Scenarios.load(name);
            assertFalse(Scenarios.description(name).isBlank(), name);
        }
    }

    @Test
    void theCatalogListsExactlyTheScenarioFiles() throws IOException {
        try (var files = Files.list(Path.of("src/main/resources/scenarios"))) {
            Set<String> onDisk = files.map(f -> f.getFileName().toString().replace(".properties", ""))
                    .collect(Collectors.toSet());
            assertEquals(onDisk, Set.copyOf(Scenarios.BUNDLED), "Scenarios.BUNDLED doit lister chaque fichier de scenarios/");
        }
    }
}

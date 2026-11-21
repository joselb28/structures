package com.transit.map;

import com.transit.settings.Settings;
import com.transit.model.BusLine;
import com.transit.model.CityMap;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MapStoreTest {

    private static final Settings.City CITY = Settings.defaults().city();

    @Test
    void roundTripPreservesTheMap(@TempDir Path dir) {
        Path db = dir.resolve("transit.db");
        CityMap generated = MapGenerator.generate(CITY, 7);

        CityMap firstLoad = MapStore.loadOrGenerate(db, CITY, 7);   // génère + enregistre
        CityMap secondLoad = MapStore.loadOrGenerate(db, CITY, 7);  // lit seulement

        for (CityMap loaded : List.of(firstLoad, secondLoad)) {
            assertEquals(generated.nodes(), loaded.nodes());
            assertEquals(Set.copyOf(generated.edges()), Set.copyOf(loaded.edges()));
            assertEquals(byNumber(generated), byNumber(loaded));
        }
    }

    @Test
    void differentSeedRegeneratesTheDatabase(@TempDir Path dir) {
        Path db = dir.resolve("transit.db");
        MapStore.loadOrGenerate(db, CITY, 1);
        CityMap reloaded = MapStore.loadOrGenerate(db, CITY, 2);
        assertEquals(MapGenerator.generate(CITY, 2).nodes(), reloaded.nodes());
    }

    private static List<BusLine> byNumber(CityMap map) {
        return map.busLines().stream().sorted(Comparator.comparingInt(BusLine::number)).toList();
    }

    @Test
    void changingTheCitySettingsRegeneratesTheDatabase(@TempDir Path dir) {
        Path db = dir.resolve("transit.db");
        MapStore.loadOrGenerate(db, CITY, 1);
        Settings.City smaller = new Settings.City(1600, 1000, CITY.intersectionSpacing(), 3);
        CityMap reloaded = MapStore.loadOrGenerate(db, smaller, 1);
        assertEquals(1600, reloaded.width());
        assertEquals(MapGenerator.generate(smaller, 1).nodes(), reloaded.nodes());
    }
}

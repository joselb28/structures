package com.transit.engine;

import com.transit.model.CityMap;
import com.transit.model.TestMaps;
import com.transit.model.Time;
import com.transit.settings.Settings;
import com.transit.trip.BusRide;
import com.transit.trip.Ride;
import com.transit.trip.TripPlanner;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomerSpawnerTest {

    private static final Settings.Customers CUSTOMERS = Settings.defaults().customers();
    private static final int TICKS = 100_000;

    private final CityMap map = TestMaps.defaultCity();
    private final Set<Integer> terminals = map.terminals();
    private final List<List<Ride>> trips = drawTrips();

    private List<List<Ride>> drawTrips() {
        TripPlanner planner = new TripPlanner(map, CUSTOMERS.transferMinutes());
        CustomerSpawner spawner = new CustomerSpawner(map, planner, new Random(1), CUSTOMERS);
        List<List<Ride>> all = new ArrayList<>();
        for (int t = 0; t < TICKS; t++) {
            all.addAll(spawner.nextTrips());
        }
        return all;
    }

    @Test
    void spawnsAtTheConfiguredRate() {
        double perTick = CUSTOMERS.perMinute() / Time.TICKS_PER_SIM_MINUTE / CUSTOMERS.spawnEveryTicks();
        assertEquals(perTick * TICKS, trips.size(), perTick * TICKS * 0.05);
    }

    @Test
    void nobodyStartsAtALineTerminal() {
        for (List<Ride> trip : trips) {
            assertFalse(terminals.contains(trip.getFirst().from()), "trip starting at a terminal: " + trip);
        }
    }

    @Test
    void theConfiguredShareOfBusTripsEndsAtATerminal() {
        List<List<Ride>> busTrips = trips.stream().filter(t -> t.getFirst() instanceof BusRide).toList();
        long toTerminal = busTrips.stream().filter(t -> terminals.contains(t.getLast().to())).count();
        double share = (double) toTerminal / busTrips.size();
        assertEquals(CUSTOMERS.terminusShare(), share, 0.03, "share of bus trips ending at a terminal");
    }

    @Test
    void theConfiguredShareOfTripsGoesByUber() {
        long byUber = trips.stream().filter(t -> !(t.getFirst() instanceof BusRide)).count();
        assertEquals(CUSTOMERS.offNetworkShare(), (double) byUber / trips.size(), 0.01);
        assertTrue(byUber > 0);
    }
}

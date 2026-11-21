package com.transit.engine;

import com.transit.settings.Settings;
import com.transit.model.TestMaps;
import com.transit.agent.Bus;
import com.transit.agent.CustomerView;
import com.transit.agent.VehicleView;
import com.transit.map.MapGenerator;
import com.transit.model.CityMap;
import com.transit.model.Time;

import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimulationTest {

    private static final Settings SETTINGS = Settings.defaults();

    private final CityMap map = TestMaps.defaultCity();

    @Test
    void spawnsTheDefaultFleet() {
        Simulation simulation = new Simulation(map, SETTINGS);
        assertEquals(map.busLines().size() * SETTINGS.fleet().busesPerLine(), simulation.buses().size());
        assertEquals(SETTINGS.fleet().ubers(), simulation.ubers().size());
        assertEquals(simulation.buses().size() + simulation.ubers().size(), simulation.snapshot().vehicles().size());
    }

    @Test
    void eachTickPublishesANewSnapshot() {
        Simulation simulation = new Simulation(map, SETTINGS);
        Snapshot before = simulation.snapshot();
        simulation.tick();
        Snapshot after = simulation.snapshot();
        assertNotSame(before, after);
        assertEquals(before.tick() + 1, after.tick());
    }

    @Test
    void tenTicksMakeOneSimulatedMinute() {
        Simulation simulation = new Simulation(map, SETTINGS);
        for (int i = 0; i < 3 * Time.TICKS_PER_SIM_MINUTE; i++) {
            simulation.tick();
        }
        assertEquals(3, simulation.snapshot().simulatedMinutes());
    }

    @Test
    void engineThreadRunsAtRoughlyTheTargetRate() throws InterruptedException {
        Simulation simulation = new Simulation(map, SETTINGS);
        simulation.setSpeed(4); // 80 ticks par seconde
        Thread engine = simulation.start();
        TimeUnit.MILLISECONDS.sleep(1_000);
        engine.interrupt();
        engine.join(1_000);
        long ticks = simulation.snapshot().tick();
        assertTrue(ticks >= 65 && ticks <= 90, "ticks in 1 s at x4: " + ticks);
    }

    @Test
    void pausedEngineDoesNotAdvance() throws InterruptedException {
        Simulation simulation = new Simulation(map, SETTINGS);
        simulation.setPaused(true);
        Thread engine = simulation.start();
        TimeUnit.MILLISECONDS.sleep(200);
        engine.interrupt();
        engine.join(1_000);
        assertEquals(0, simulation.snapshot().tick());
    }

    @Test
    void rushHourNeverOverfillsBusesAndAccountsForEveryCustomer() {
        Simulation simulation = new Simulation(map, SETTINGS);
        simulation.spawner().setRushHour(true);
        boolean sawFullBus = false;
        for (int i = 0; i < 5_000; i++) {
            simulation.tick();
            for (VehicleView v : simulation.snapshot().vehicles()) {
                assertTrue(v.passengers() <= v.capacity(), v.id() + " overfilled");
                sawFullBus |= v.kind() == VehicleView.Kind.BUS && v.isFull();
            }
        }
        Snapshot.Stats stats = simulation.snapshot().stats();
        assertTrue(stats.delivered() > 0);
        assertTrue(sawFullBus, "rush hour should fill at least one bus");
        assertEquals(stats.spawned(), stats.waiting() + stats.inTransit() + stats.delivered());
        int onBoard = simulation.snapshot().vehicles().stream().mapToInt(VehicleView::passengers).sum();
        assertEquals(stats.inTransit(), onBoard, "customers in transit = passengers in buses and Ubers");
    }

    @Test
    void customersCrossTheCityWithTransfers() {
        Simulation simulation = new Simulation(map, SETTINGS);
        for (int i = 0; i < 5_000; i++) {
            simulation.tick();
        }
        Snapshot.Stats stats = simulation.snapshot().stats();
        assertTrue(stats.delivered() > 50, "delivered: " + stats.delivered());
        assertTrue(stats.transfers() > 0, "nobody transferred");
    }

    @Test
    void ubersPickUpAndDeliverOffNetworkCustomers() {
        Simulation simulation = new Simulation(map, SETTINGS);
        long busyUberTicks = 0;
        for (int i = 0; i < 5_000; i++) {
            simulation.tick();
            busyUberTicks += simulation.snapshot().vehicles().stream()
                    .filter(v -> v.kind() == VehicleView.Kind.UBER && v.passengers() > 0).count();
        }
        assertTrue(busyUberTicks > 0, "no Uber ever carried a customer");
        assertTrue(simulation.snapshot().waiting().stream().allMatch(c ->
                c.mode() == CustomerView.Mode.BUS ? c.line() > 0 : c.line() == 0));
    }
}

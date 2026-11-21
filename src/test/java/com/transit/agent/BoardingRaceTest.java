package com.transit.agent;

import com.transit.model.CityMap;
import com.transit.model.TestMaps;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Race n° 1 : des clients montent dans le même petit bus et en descendent en même temps, ce qui le
 * garde constamment à la limite de sa capacité.
 * Échoue avec un simple {@code int} « vérifier puis incrémenter », passe avec une boucle CAS.
 */
class BoardingRaceTest {

    private static final int CUSTOMERS = 8;
    private static final int TRIPS_PER_CUSTOMER = 1_000_000;
    private static final int CAPACITY = 3;

    @Test
    void capacityIsNeverExceededAndNoSeatIsLost() throws Exception {
        CityMap map = TestMaps.street();
        Bus bus = new Bus("5A", map.busLines().getFirst(), map, CAPACITY, 0);
        AtomicInteger overcrowdedSightings = new AtomicInteger();
        CountDownLatch startGate = new CountDownLatch(1);

        try (ExecutorService pool = Executors.newFixedThreadPool(CUSTOMERS)) {
            for (int c = 0; c < CUSTOMERS; c++) {
                pool.submit(() -> {
                    startGate.await();
                    for (int trip = 0; trip < TRIPS_PER_CUSTOMER; trip++) {
                        if (bus.tryBoard()) {
                            if (bus.passengers() > CAPACITY) {
                                overcrowdedSightings.incrementAndGet();
                            }
                            bus.alight();
                        }
                    }
                    return null;
                });
            }
            startGate.countDown();
        } // close() attend la fin de tous les clients

        assertEquals(0, overcrowdedSightings.get(), "times a customer saw more passengers than seats");
        assertEquals(0, bus.passengers(), "everyone got off, the bus should be empty");
    }
}

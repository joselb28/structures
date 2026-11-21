package com.transit.metrics;

import com.transit.engine.Simulation;
import com.transit.model.Time;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Race n° 3 : des mises à jour concurrentes des compteurs ne doivent pas se perdre.
 * Échoue avec de simples compteurs {@code long}, passe avec {@code LongAdder}.
 */
class MetricsRaceTest {

    private static final int THREADS = 8;
    private static final int UPDATES_PER_THREAD = 100_000;

    @Test
    void noLostUpdates() throws Exception {
        Metrics metrics = new Metrics();
        CountDownLatch startGate = new CountDownLatch(1);
        try (ExecutorService pool = Executors.newFixedThreadPool(THREADS)) {
            for (int t = 0; t < THREADS; t++) {
                pool.submit(() -> {
                    startGate.await();
                    for (int i = 0; i < UPDATES_PER_THREAD; i++) {
                        metrics.recordBoarding(2);
                        metrics.recordDelivery(3, 500);
                        metrics.recordBusFare(350);
                        metrics.recordUberFare(900);
                    }
                    return null;
                });
            }
            startGate.countDown();
        } // close() attend la fin de toutes les tâches

        long expected = (long) THREADS * UPDATES_PER_THREAD;
        assertEquals(expected, metrics.boarded());
        assertEquals(expected, metrics.delivered());
        assertEquals(expected, metrics.busFares());
        assertEquals(expected * 350, metrics.busRevenueCents());
        assertEquals(expected * 900, metrics.uberRevenueCents());
        assertEquals(500, metrics.averageTripPriceCents(), 1e-9);
        assertEquals(2.0 / Time.TICKS_PER_SIM_MINUTE, metrics.averageWaitMinutes(), 1e-12);
        assertEquals(3.0 / Time.TICKS_PER_SIM_MINUTE, metrics.averageTripMinutes(), 1e-12);
    }
}

package com.transit.agent;

import com.transit.metrics.Metrics;
import com.transit.trip.Fares;
import com.transit.trip.UberRide;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicIntegerArray;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Race n° 2 : plusieurs Uber libres essaient de réserver les mêmes clients en même temps.
 * Chaque Uber parcourt la même liste de demandes : ils se percutent sur chaque client.
 * Échoue avec un « vérifier puis affecter » sur un champ ordinaire, passe avec {@code AtomicReference.compareAndSet}.
 */
class ClaimRaceTest {

    private static final Fares FARES = new Fares(350, 300, 60);

    private static final int UBERS = 8;
    private static final int CUSTOMERS = 200_000;

    @Test
    void eachCustomerIsClaimedByExactlyOneUber() throws Exception {
        Metrics metrics = new Metrics();
        Customer[] customers = new Customer[CUSTOMERS];
        for (int i = 0; i < CUSTOMERS; i++) {
            customers[i] = new Customer(i, List.of(new UberRide(0, 1)), 0, new BusStops(), metrics, FARES);
        }
        AtomicIntegerArray claims = new AtomicIntegerArray(CUSTOMERS);
        CountDownLatch startGate = new CountDownLatch(1);

        try (ExecutorService pool = Executors.newFixedThreadPool(UBERS)) {
            for (int u = 0; u < UBERS; u++) {
                pool.submit(() -> {
                    startGate.await();
                    for (int i = 0; i < CUSTOMERS; i++) {
                        if (customers[i].tryClaim()) {
                            claims.incrementAndGet(i);
                        }
                    }
                    return null;
                });
            }
            startGate.countDown();
        }

        int doubleClaims = 0;
        for (int i = 0; i < CUSTOMERS; i++) {
            if (claims.get(i) != 1) {
                doubleClaims++;
            }
        }
        assertEquals(0, doubleClaims, "customers claimed by zero or several Ubers");
    }
}

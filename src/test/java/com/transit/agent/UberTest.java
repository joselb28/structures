package com.transit.agent;

import com.transit.map.MapGenerator;
import com.transit.metrics.Metrics;
import com.transit.trip.Fares;
import com.transit.model.CityMap;
import com.transit.model.TestMaps;
import com.transit.model.Time;
import com.transit.trip.UberRide;

import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UberTest {

    private static final Fares FARES = new Fares(350, 300, 60);

    @Test
    void wandersAlongEdgesWithoutUTurnsExceptAtDeadEnds() {
        CityMap map = TestMaps.defaultCity();
        Uber uber = new Uber("U", map, 0, new Random(1), new UberRequests());
        int beforePrevious = -1, previous = uber.currentNode();
        for (long tick = 1; tick <= 20_000; tick++) {
            uber.update(tick);
            int now = uber.currentNode();
            if (now != previous) {
                assertTrue(map.areAdjacent(previous, now));
                if (map.neighbors(previous).length > 1) {
                    assertNotEquals(beforePrevious, now, "U-turn at " + previous);
                }
                beforePrevious = previous;
                previous = now;
            }
        }
    }

    @Test
    void turnsAroundAtADeadEnd() {
        CityMap map = TestMaps.street();
        Uber uber = new Uber("U", map, 2, new Random(3), new UberRequests());
        int visitsTo4 = 0;
        int previous = uber.currentNode();
        for (long tick = 1; tick <= 2_000; tick++) {
            uber.update(tick);
            if (uber.currentNode() != previous) {
                if (previous == 4) {
                    assertEquals(2, uber.currentNode());
                    visitsTo4++;
                }
                previous = uber.currentNode();
            }
        }
        assertTrue(visitsTo4 > 0, "never reached the dead end");
    }

    @Test
    void picksUpTheWaitingCustomerAndDropsThemOff() {
        CityMap map = TestMaps.street();
        Metrics metrics = new Metrics();
        Customer customer = new Customer(0, List.of(new UberRide(4, 0)), 0, new BusStops(), metrics, FARES);
        UberRequests requests = new UberRequests();
        Uber uber = new Uber("U", map, 3, new Random(1), requests);

        boolean sawCustomerOnBoard = false;
        for (long tick = 1; tick <= 500 && customer.state() != Customer.State.ARRIVED; tick++) {
            requests.refresh(List.of(customer));
            uber.update(tick);
            sawCustomerOnBoard |= uber.view().passengers() == 1;
        }
        assertEquals(Customer.State.ARRIVED, customer.state());
        assertTrue(sawCustomerOnBoard);
        assertEquals(0, uber.currentNode());
        assertTrue(uber.isFree());
        assertEquals(1, metrics.delivered());
        // pris en charge à 4 (une minute pour monter), puis 4 -> 2 -> 1 -> 0 = trois tronçons :
        // quatre minutes dans la voiture
        assertEquals(FARES.uberRideCents(4L * Time.TICKS_PER_SIM_MINUTE), customer.paidCents());
        assertEquals(customer.paidCents(), metrics.uberRevenueCents());
    }

    @Test
    void aFreeUberIgnoresCustomersAlreadyClaimed() {
        CityMap map = TestMaps.street();
        Customer customer = new Customer(0, List.of(new UberRide(4, 0)), 0, new BusStops(), new Metrics(), FARES);
        UberRequests requests = new UberRequests();
        requests.refresh(List.of(customer));
        Uber first = new Uber("U1", map, 3, new Random(1), requests);
        Uber second = new Uber("U2", map, 3, new Random(2), requests);

        first.update(1);
        second.update(1);
        assertTrue(!first.isFree());
        assertTrue(second.isFree());
    }

    @Test
    void stopsOneMinuteToPickUpTheCustomer() {
        CityMap map = TestMaps.street();
        Customer customer = new Customer(0, List.of(new UberRide(3, 0)), 0, new BusStops(), new Metrics(), FARES);
        UberRequests requests = new UberRequests();
        Uber uber = new Uber("U", map, 3, new Random(1), requests); // déjà sur place

        int ticksAtPickup = 0;
        for (long tick = 1; tick <= 3 * Time.STOP_DWELL_TICKS; tick++) {
            requests.refresh(List.of(customer));
            uber.update(tick);
            if (customer.state() == Customer.State.IN_TRANSIT && uber.view().x() == map.node(3).x()) {
                ticksAtPickup++; // client à bord, voiture encore au point de prise en charge
            }
        }
        assertEquals(Time.STOP_DWELL_TICKS, ticksAtPickup, "une minute simulée d'arrêt pour faire monter le client");
    }
}

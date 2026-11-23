package com.transit.agent;

import com.transit.engine.Simulation;
import com.transit.metrics.Metrics;
import com.transit.trip.Fares;
import com.transit.model.CityMap;
import com.transit.model.TestMaps;
import com.transit.trip.BusRide;
import com.transit.trip.TripPlanner;

import java.util.List;
import java.util.function.BooleanSupplier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomerTest {

    private static final Fares FARES = new Fares(350, 300, 60);

    private static final int TRANSFER_MINUTES = 5;

    private final CityMap map = TestMaps.street();
    private final BusStops stops = new BusStops();
    private final Metrics metrics = new Metrics();
    private long tick;

    @Test
    void ridesFromOriginToDestination() {
        Bus bus = bus(40, false);
        Customer customer = customer(0, 3);

        runUntil(() -> customer.state() == Customer.State.IN_TRANSIT, List.of(bus), List.of(customer));
        assertEquals(1, bus.passengers());
        runUntil(() -> customer.state() == Customer.State.ARRIVED, List.of(bus), List.of(customer));
        assertEquals(3, bus.currentNode());
        assertEquals(0, bus.passengers());
        assertEquals(1, metrics.delivered());
    }

    @Test
    void ignoresABusGoingTheWrongWay() {
        Bus bus = bus(40, false);          // 0 -> 3, puis retour
        Customer customer = customer(2, 0); // veut repartir vers 0

        runUntil(() -> bus.isAtStop() && bus.currentNode() == 2, List.of(bus), List.of(customer));
        step(List.of(bus), List.of(customer));
        assertEquals(Customer.State.WAITING, customer.state(), "boarded a bus heading to 3");

        runUntil(() -> customer.state() == Customer.State.ARRIVED, List.of(bus), List.of(customer));
    }

    @Test
    void cannotBoardAFullBus() {
        Bus bus = bus(1, false);
        Customer first = customer(0, 3);
        Customer second = customer(0, 3);

        runUntil(() -> first.state() != Customer.State.WAITING || second.state() != Customer.State.WAITING,
                List.of(bus), List.of(first, second));
        assertTrue(bus.isFull());
        assertEquals(1, List.of(first, second).stream().filter(c -> c.state() == Customer.State.IN_TRANSIT).count());
    }

    @Test
    void transfersBetweenLinesToReachItsDestination() {
        CityMap cross = TestMaps.cross();
        BusStops crossStops = new BusStops();
        List<Bus> buses = List.of(
                new Bus("5A", cross.busLines().get(0), cross, 40, 0),
                new Bus("7A", cross.busLines().get(1), cross, 40, 0));
        Customer customer = new Customer(0, new TripPlanner(cross, TRANSFER_MINUTES).planByBus(0, 6), 0, crossStops, metrics, FARES);
        assertEquals(List.of(new BusRide(5, 0, 2), new BusRide(7, 2, 6)), customer.trip());

        for (int i = 0; i < 2_000 && customer.state() != Customer.State.ARRIVED; i++) {
            tick++;
            buses.forEach(b -> b.update(tick));
            crossStops.refresh(buses);
            customer.update(tick);
        }
        assertEquals(Customer.State.ARRIVED, customer.state());
        assertEquals(1, metrics.transfers());
        assertEquals(FARES.busTripCents(), customer.paidCents(), "a transfer is included in the bus fare");
        assertEquals(1, metrics.busFares());
        assertEquals(2, metrics.boarded());
        assertTrue(buses.stream().allMatch(b -> b.passengers() == 0));
    }

    @Test
    void someoneGettingOffFreesTheSeatForSomeoneWaitingOnTheSameTick() {
        Bus bus = bus(1, false);                 // une seule place
        Customer rider = customer(0, 2);         // monte à 0, descend à 2
        Customer waiting = customer(2, 3);       // attend à 2 pour aller à 3
        runUntil(() -> rider.state() == Customer.State.IN_TRANSIT, List.of(bus), List.of(rider));

        // Comme le moteur : tout le monde descend, puis tout le monde monte. La cliente qui attend
        // est évaluée en premier exprès : elle ne doit quand même pas rater la place libérée.
        List<Customer> customers = List.of(waiting, rider);
        while (rider.state() != Customer.State.ARRIVED) {
            tick++;
            bus.update(tick);
            stops.refresh(List.of(bus));
            customers.forEach(c -> c.getOffIfArrived(tick));
            customers.forEach(c -> c.boardIfPossible(tick));
        }
        assertEquals(Customer.State.IN_TRANSIT, waiting.state(), "la place libérée doit servir pendant le même tick");
    }

    @Test
    void busAtATerminalIsConsideredTurnedAround() {
        Bus atLastTerminal = bus(40, true); // arrêté à 3
        assertTrue(atLastTerminal.isHeadingTo(0));
        assertFalse(atLastTerminal.isHeadingTo(3));
    }

    // ------------------------------------------------------------------ outils

    private Bus bus(int capacity, boolean startAtLastTerminal) {
        return new Bus("5A", map.busLines().getFirst(), map, capacity, startAtLastTerminal ? 0.5 : 0);
    }

    private Customer customer(int origin, int destination) {
        return new Customer(0, List.of(new BusRide(5, origin, destination)), tick, stops, metrics, FARES);
    }

    /** Mêmes phases que {@link Simulation#tick()}, sur un seul thread. */
    private void step(List<Bus> buses, List<Customer> customers) {
        tick++;
        buses.forEach(b -> b.update(tick));
        stops.refresh(buses);
        customers.forEach(c -> c.update(tick));
    }

    private void runUntil(BooleanSupplier condition, List<Bus> buses, List<Customer> customers) {
        for (int i = 0; i < 1_000 && !condition.getAsBoolean(); i++) {
            step(buses, customers);
        }
        assertTrue(condition.getAsBoolean(), "condition not reached in 1000 ticks");
    }
}

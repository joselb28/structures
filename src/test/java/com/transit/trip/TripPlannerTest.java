package com.transit.trip;

import com.transit.map.MapGenerator;
import com.transit.model.BusLine;
import com.transit.model.CityMap;
import com.transit.model.TestMaps;

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TripPlannerTest {

    private static final int TRANSFER_MINUTES = 5;

    @Test
    void directTripIsASingleRide() {
        TripPlanner planner = new TripPlanner(TestMaps.cross(), TRANSFER_MINUTES);
        assertEquals(List.of(new BusRide(5, 0, 3)), planner.planByBus(0, 3));
        assertEquals(List.of(new BusRide(5, 3, 0)), planner.planByBus(3, 0));
    }

    @Test
    void chainsTwoLinesThroughTheSharedStop() {
        TripPlanner planner = new TripPlanner(TestMaps.cross(), TRANSFER_MINUTES);
        assertEquals(List.of(new BusRide(5, 0, 2), new BusRide(7, 2, 6)), planner.planByBus(0, 6));
        assertEquals(List.of(new BusRide(7, 6, 2), new BusRide(5, 2, 0)), planner.planByBus(6, 0));
    }

    @Test
    void noItineraryForTheSameStopOrANonStop() {
        TripPlanner planner = new TripPlanner(TestMaps.cross(), TRANSFER_MINUTES);
        assertEquals(List.of(), planner.planByBus(0, 0));
        assertEquals(List.of(), planner.planByBus(0, 1)); // 1 est un point de route
    }

    @Test
    void everyPairOfStopsIsConnectedByAWellFormedItinerary() {
        CityMap map = TestMaps.defaultCity();
        TripPlanner planner = new TripPlanner(map, TRANSFER_MINUTES);
        List<Integer> stops = map.intersectionIds().stream().filter(map::isStop).toList();
        int maxRides = 0;

        for (int origin : stops) {
            for (int destination : stops) {
                if (origin == destination) {
                    continue;
                }
                List<BusRide> rides = planner.planByBus(origin, destination);
                assertTrue(!rides.isEmpty(), origin + " -> " + destination + " unreachable");
                assertEquals(origin, rides.getFirst().from());
                assertEquals(destination, rides.getLast().to());
                for (int i = 0; i < rides.size(); i++) {
                    BusRide ride = rides.get(i);
                    assertNotEquals(ride.from(), ride.to());
                    assertTrue(map.linesAt(ride.from()).contains(ride.line()), ride + ": line does not stop at boarding stop");
                    assertTrue(map.linesAt(ride.to()).contains(ride.line()), ride + ": line does not stop at alighting stop");
                    if (i > 0) {
                        assertEquals(rides.get(i - 1).to(), ride.from(), "rides must chain");
                        assertTrue(map.isTransferStop(ride.from()));
                    }
                }
                maxRides = Math.max(maxRides, rides.size());
            }
        }
        assertTrue(maxRides >= 3, "the default city should need several transfers for some trips");
    }

    @Test
    void prefersADirectRideWhenATransferWouldNotSaveEnoughTime() {
        CityMap map = TestMaps.defaultCity();
        TripPlanner planner = new TripPlanner(map, TRANSFER_MINUTES);
        BusLine line = map.busLines().getFirst();
        List<Integer> lineStops = line.nodeIds().stream().filter(map::isStop).toList();
        int a = lineStops.get(0), b = lineStops.get(1);
        assertEquals(List.of(new BusRide(line.number(), a, b)), planner.planByBus(a, b));
    }
}

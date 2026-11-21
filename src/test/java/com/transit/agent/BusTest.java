package com.transit.agent;

import com.transit.engine.Simulation;
import com.transit.model.CityMap;
import com.transit.model.TestMaps;
import com.transit.model.Time;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BusTest {

    private final CityMap map = TestMaps.street();

    @Test
    void travelsItsLineBackAndForth() {
        Bus bus = new Bus("5A", map.busLines().getFirst(), map, 40, 0);
        assertEquals(List.of(0, 1, 2, 3, 2, 1, 0, 1), visitedNodes(bus, 8));
    }

    @Test
    void secondBusStartsAtTheOtherTerminalHeadingBack() {
        Bus bus = new Bus("5B", map.busLines().getFirst(), map, 40, 0.5);
        assertEquals(List.of(3, 2, 1, 0, 1), visitedNodes(bus, 5));
    }

    @Test
    void dwellsOneMinuteAtStopsButNotAtRoadPoints() {
        Bus bus = new Bus("5A", map.busLines().getFirst(), map, 40, 0);
        int dwell = Time.STOP_DWELL_TICKS, edge = Time.TICKS_PER_SIM_MINUTE;
        // arrêt à 0, tronçon 0->1, (pas d'arrêt au point de route 1), tronçon 1->2, arrêt à l'arrêt 2
        long ticksToLeaveStop2 = dwell + edge + edge + dwell;
        long tick = 0;
        while (bus.currentNode() != 2) {
            bus.update(++tick);
        }
        assertEquals(dwell + 2L * edge, tick);
        while (bus.isAtStop()) {
            bus.update(++tick);
        }
        assertEquals(ticksToLeaveStop2 + 1, tick);
    }

    /** Les nœuds distincts visités, dans l'ordre, jusqu'à en avoir {@code count}. */
    static List<Integer> visitedNodes(Bus bus, int count) {
        List<Integer> visited = new ArrayList<>(List.of(bus.currentNode()));
        for (long tick = 1; visited.size() < count; tick++) {
            bus.update(tick);
            if (bus.currentNode() != visited.getLast()) {
                visited.add(bus.currentNode());
            }
        }
        return visited;
    }

    @Test
    void busesOfALineStartEvenlySpreadOverTheRoundTrip() {
        // la ligne 5 de la rue a 4 nœuds : aller-retour 0 1 2 3 2 1 (6 pas)
        var line = map.busLines().getFirst();
        assertEquals(0, new Bus("5A", line, map, 40, 0).currentNode());
        assertEquals(2, new Bus("5B", line, map, 40, 1.0 / 3).currentNode());
        Bus third = new Bus("5C", line, map, 40, 2.0 / 3);
        assertEquals(2, third.currentNode());
        assertEquals(-1, third.direction(), "the third bus is on its way back");
    }
}

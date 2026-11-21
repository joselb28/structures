package com.transit.map;

import com.transit.settings.Settings;
import com.transit.model.BusLine;
import com.transit.model.CityMap;
import com.transit.model.Edge;
import com.transit.model.Node;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.IntFunction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MapGeneratorTest {

    private static final Settings.City CITY = Settings.defaults().city();
    private static final long SEED = Settings.defaults().seed();

    private final CityMap map = MapGenerator.generate(CITY, SEED);

    @Test
    void sameSeedGivesSameCity() {
        CityMap again = MapGenerator.generate(CITY, SEED);
        assertEquals(map.nodes(), again.nodes());
        assertEquals(map.edges(), again.edges());
        assertEquals(map.busLines(), again.busLines());
    }

    @Test
    void nodesStayInsideTheMap() {
        for (Node n : map.nodes()) {
            assertTrue(n.x() >= 0 && n.x() <= map.width() && n.y() >= 0 && n.y() <= map.height(), n.toString());
        }
    }

    @Test
    void intersectionsRespectMinimumSpacing() {
        List<Integer> ids = map.intersectionIds();
        for (int i = 0; i < ids.size(); i++) {
            for (int j = i + 1; j < ids.size(); j++) {
                assertTrue(map.node(ids.get(i)).distanceTo(map.node(ids.get(j))) >= CITY.intersectionSpacing());
            }
        }
    }

    @Test
    void graphIsConnected() {
        assertEquals(map.nodeCount(), reachableFrom(0, id -> map.neighbors(id)).size());
    }

    @Test
    void roadsNeverCrossOutsideIntersections() {
        List<Edge> edges = map.edges();
        for (int i = 0; i < edges.size(); i++) {
            for (int j = i + 1; j < edges.size(); j++) {
                Edge e = edges.get(i), f = edges.get(j);
                boolean shareNode = e.a() == f.a() || e.a() == f.b() || e.b() == f.a() || e.b() == f.b();
                if (!shareNode) {
                    assertFalse(properlyIntersect(e, f), e + " crosses " + f);
                }
            }
        }
    }

    @Test
    void segmentsHaveRoughlyUniformLength() {
        for (Edge e : map.edges()) {
            double length = map.node(e.a()).distanceTo(map.node(e.b()));
            assertTrue(length <= MapGenerator.SEGMENT_LENGTH * 1.5, e + " is " + length + " px long");
        }
    }

    @Test
    void busLinesAreLongAndStartAndEndAtIntersections() {
        assertEquals(CITY.busLines(), map.busLines().size());
        for (BusLine line : map.busLines()) {
            Node first = map.node(line.nodeIds().getFirst());
            Node last = map.node(line.nodeIds().getLast());
            assertEquals(Node.Type.INTERSECTION, first.type());
            assertEquals(Node.Type.INTERSECTION, last.type());
            assertTrue(first.distanceTo(last) >= BusLinePlanner.MIN_SPAN_OF_DIAGONAL * Math.hypot(map.width(), map.height()));
        }
    }

    @Test
    void everyLineIsReachableThroughTransfers() {
        Map<Integer, Set<Integer>> transfers = new HashMap<>();
        for (BusLine line : map.busLines()) {
            transfers.put(line.number(), new HashSet<>());
        }
        for (int id : map.intersectionIds()) {
            for (int a : map.linesAt(id)) {
                transfers.get(a).addAll(map.linesAt(id));
            }
        }
        int firstLine = map.busLines().getFirst().number();
        Set<Integer> reachable = reachableFrom(firstLine,
                line -> transfers.get(line).stream().mapToInt(Integer::intValue).toArray());
        assertEquals(transfers.keySet(), reachable);
    }

    // ------------------------------------------------------------------ outils

    private static Set<Integer> reachableFrom(int start, IntFunction<int[]> next) {
        Set<Integer> seen = new HashSet<>(List.of(start));
        Deque<Integer> todo = new ArrayDeque<>(List.of(start));
        while (!todo.isEmpty()) {
            for (int n : next.apply(todo.poll())) {
                if (seen.add(n)) {
                    todo.add(n);
                }
            }
        }
        return seen;
    }

    private boolean properlyIntersect(Edge e, Edge f) {
        Node p1 = map.node(e.a()), p2 = map.node(e.b()), q1 = map.node(f.a()), q2 = map.node(f.b());
        double d1 = cross(q1, q2, p1), d2 = cross(q1, q2, p2), d3 = cross(p1, p2, q1), d4 = cross(p1, p2, q2);
        return d1 * d2 < 0 && d3 * d4 < 0;
    }

    private static double cross(Node o, Node a, Node b) {
        return (a.x() - o.x()) * (b.y() - o.y()) - (a.y() - o.y()) * (b.x() - o.x());
    }
}

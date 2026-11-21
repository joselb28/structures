package com.transit.graph;

import com.transit.graph.ShortestPath.Step;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShortestPathTest {

    /**
     * <pre>
     *   A --1-- B --1-- C
     *    \             /
     *     -----5------
     * </pre>
     */
    private static final Map<String, List<Step<String>>> GRAPH = Map.of(
            "A", List.of(new Step<>("B", 1), new Step<>("C", 5)),
            "B", List.of(new Step<>("A", 1), new Step<>("C", 1)),
            "C", List.of(new Step<>("B", 1), new Step<>("A", 5)),
            "D", List.of());

    @Test
    void findsTheCheapestRatherThanTheShortestPath() {
        var path = ShortestPath.find(Set.of("A"), "C"::equals, GRAPH::get).orElseThrow();
        assertEquals(List.of("A", "B", "C"), path.nodes());
        assertEquals(2, path.cost());
    }

    @Test
    void sourceThatIsATargetGivesASingleNodePath() {
        var path = ShortestPath.find(Set.of("A"), "A"::equals, GRAPH::get).orElseThrow();
        assertEquals(List.of("A"), path.nodes());
        assertEquals(0, path.cost());
    }

    @Test
    void startsFromTheBestOfSeveralSources() {
        var path = ShortestPath.find(List.of("A", "B"), "C"::equals, GRAPH::get).orElseThrow();
        assertEquals(List.of("B", "C"), path.nodes());
    }

    @Test
    void unreachableTargetGivesNothing() {
        Optional<ShortestPath.Path<String>> path = ShortestPath.find(Set.of("A"), "D"::equals, GRAPH::get);
        assertTrue(path.isEmpty());
    }
}

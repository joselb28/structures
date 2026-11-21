package com.transit.trip;

import com.transit.graph.ShortestPath;
import com.transit.graph.ShortestPath.Step;
import com.transit.model.CityMap;

import java.util.ArrayList;
import java.util.List;

/**
 * Rouler sur le réseau routier (les Uber). Chaque tronçon coûte une minute simulée.
 */
public final class Roads {

    private Roads() {
    }

    // TODO Q14 (★★★) : trouver la route par un parcours en largeur (BFS) au lieu de Dijkstra — voir docs/QUESTIONS.md
    /** @return les nœuds de {@code from} à {@code to}, les deux inclus (le réseau est connexe) */
    public static List<Integer> fastestRoute(CityMap map, int from, int to) {
        return ShortestPath.find(List.of(from), n -> n == to, n -> steps(map, n)).orElseThrow().nodes();
    }

    private static List<Step<Integer>> steps(CityMap map, int node) {
        List<Step<Integer>> steps = new ArrayList<>();
        for (int next : map.neighbors(node)) {
            steps.add(new Step<>(next, 1));
        }
        return steps;
    }
}

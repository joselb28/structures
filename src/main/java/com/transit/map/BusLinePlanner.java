package com.transit.map;

import com.transit.graph.ShortestPath.Step;
import com.transit.graph.ShortestPath;
import com.transit.model.Edge;
import com.transit.settings.Settings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Trace les lignes de bus numérotées sur un {@link RoadLayout}.
 *
 * <p>Chaque ligne est le chemin le moins cher entre deux intersections éloignées, où une route déjà
 * desservie par d'autres lignes coûte plus cher : les lignes s'étalent. Une ligne candidate est rejetée
 * si elle partage trop de routes avec les lignes existantes, ou aucun arrêt (le réseau de bus doit
 * rester relié par des correspondances).
 */
final class BusLinePlanner {

    /** Les deux terminus d'une ligne sont éloignés d'au moins cette fraction de la diagonale de la carte. */
    static final double MIN_SPAN_OF_DIAGONAL = 0.4;
    /** Chaque ligne qui emprunte déjà une route la rend d'autant plus chère pour la ligne suivante. */
    static final double REUSE_PENALTY = 3.0;
    /** Une ligne candidate qui partage plus que cette fraction de ses routes avec d'autres est rejetée. */
    static final double MAX_SHARED_ROADS = 0.4;
    /** Nombre de lignes candidates tirées avant de renoncer à atteindre le nombre de lignes demandé. */
    static final int ATTEMPTS = 200;
    /** Les numéros de ligne sont tirés entre 1 et cette valeur, comme dans un vrai réseau. */
    static final int MAX_LINE_NUMBER = 99;

    private final RoadLayout layout;
    private final Random rng;
    private final double minSpan;
    private final List<List<Integer>> neighbors = new ArrayList<>();
    private final Map<Edge, Integer> roadUsage = new HashMap<>();
    private final Set<Integer> servedStops = new HashSet<>();

    private BusLinePlanner(RoadLayout layout, Random rng, double minSpan) {
        this.layout = layout;
        this.rng = rng;
        this.minSpan = minSpan;
        for (int i = 0; i < layout.intersections().size(); i++) {
            neighbors.add(new ArrayList<>());
        }
        for (Edge road : layout.roads()) {
            neighbors.get(road.a()).add(road.b());
            neighbors.get(road.b()).add(road.a());
        }
    }

    /**
     * @return numéro de ligne -> chemin d'indices d'intersections, dans l'ordre de création. Peut contenir
     *         moins de {@code city.busLines()} lignes quand la ville est trop petite pour toutes les placer.
     */
    static Map<Integer, List<Integer>> plan(RoadLayout layout, Settings.City city, Random rng) {
        double minSpan = MIN_SPAN_OF_DIAGONAL * Math.hypot(city.width(), city.height());
        return new BusLinePlanner(layout, rng, minSpan).planAll(city.busLines());
    }

    /** Tire des lignes candidates jusqu'à en accepter {@code lineCount} ou épuiser les essais. */
    private Map<Integer, List<Integer>> planAll(int lineCount) {
        List<Integer> numbers = new ArrayList<>();
        for (int k = 1; k <= MAX_LINE_NUMBER; k++) {
            numbers.add(k);
        }
        Collections.shuffle(numbers, rng);

        Map<Integer, List<Integer>> lines = new LinkedHashMap<>();
        for (int attempt = 0; attempt < ATTEMPTS && lines.size() < lineCount; attempt++) {
            List<Integer> candidate = drawCandidate();
            if (candidate != null && isAcceptable(candidate, lines.isEmpty())) {
                for (int i = 1; i < candidate.size(); i++) {
                    roadUsage.merge(new Edge(candidate.get(i - 1), candidate.get(i)), 1, Integer::sum);
                }
                servedStops.addAll(candidate);
                lines.put(numbers.get(lines.size()), candidate);
            }
        }
        return lines;
    }

    /**
     * Chemin le moins cher d'une intersection au hasard vers une intersection éloignée au hasard, ou null s'il n'y en a pas.
     */
    private List<Integer> drawCandidate() {
        List<RoadLayout.Point> points = layout.intersections();
        int start = rng.nextInt(points.size());
        List<Integer> farAway = new ArrayList<>();
        for (int i = 0; i < points.size(); i++) {
            if (points.get(start).distanceTo(points.get(i)) >= minSpan) {
                farAway.add(i);
            }
        }
        if (farAway.isEmpty()) {
            return null;
        }
        int end = farAway.get(rng.nextInt(farAway.size()));
        return ShortestPath.find(List.of(start), n -> n == end, this::steps).orElseThrow().nodes();
    }

    /** Les routes qui partent d'une intersection, plus chères pour chaque ligne qui les emprunte déjà. */
    private List<Step<Integer>> steps(int from) {
        List<Step<Integer>> steps = new ArrayList<>();
        for (int to : neighbors.get(from)) {
            double length = layout.intersections().get(from).distanceTo(layout.intersections().get(to));
            int usage = roadUsage.getOrDefault(new Edge(from, to), 0);
            steps.add(new Step<>(to, length * (1 + REUSE_PENALTY * usage)));
        }
        return steps;
    }

    /** Partage un arrêt avec les lignes existantes (sauf si c'est la première) et pas trop de routes. */
    private boolean isAcceptable(List<Integer> candidate, boolean firstLine) {
        boolean connected = firstLine || candidate.stream().anyMatch(servedStops::contains);
        long sharedRoads = 0;
        for (int i = 1; i < candidate.size(); i++) {
            if (roadUsage.containsKey(new Edge(candidate.get(i - 1), candidate.get(i)))) {
                sharedRoads++;
            }
        }
        return connected && sharedRoads <= MAX_SHARED_ROADS * (candidate.size() - 1);
    }
}

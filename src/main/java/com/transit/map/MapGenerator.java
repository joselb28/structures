package com.transit.map;

import com.transit.model.BusLine;
import com.transit.model.CityMap;
import com.transit.model.Edge;
import com.transit.model.Node;
import com.transit.settings.Settings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Générateur de ville procédural et déterministe (même graine, même ville) :
 * <ol>
 *   <li>{@link RoadLayout} : des intersections bien réparties, reliées par des routes planaires et connexes ;</li>
 *   <li>{@link BusLinePlanner} : des lignes de bus numérotées qui partagent des arrêts de correspondance ;</li>
 *   <li>découpage (ici) : chaque route est coupée en tronçons d'environ {@value #SEGMENT_LENGTH} px pour que
 *       « un nœud traversé = une minute simulée » donne une vitesse uniforme.</li>
 * </ol>
 */
public final class MapGenerator {

    /**
     * Longueur d'un tronçon de route, en pixels : la distance parcourue en une minute simulée.
     * Ce n'est pas un réglage de scénario, car elle fixe l'échelle de vitesse de toute la simulation.
     */
    static final double SEGMENT_LENGTH = 40;

    private MapGenerator() {
    }

    public static CityMap generate(Settings.City city, long seed) {
        Random rng = new Random(seed);
        RoadLayout layout = RoadLayout.generate(city, rng);
        Map<Integer, List<Integer>> lines = BusLinePlanner.plan(layout, city, rng);
        return subdivide(city, layout, lines);
    }

    /** Les intersections gardent les identifiants {@code 0..n-1} ; les points de route sont numérotés après. */
    static CityMap subdivide(Settings.City city, RoadLayout layout, Map<Integer, List<Integer>> linePaths) {
        List<Node> nodes = new ArrayList<>();
        for (RoadLayout.Point p : layout.intersections()) {
            nodes.add(new Node(nodes.size(), p.x(), p.y(), Node.Type.INTERSECTION));
        }

        List<Edge> edges = new ArrayList<>();
        Map<Edge, List<Integer>> chains = new HashMap<>(); // route -> identifiants des nœuds de road.a() à road.b()
        for (Edge road : layout.roads()) {
            List<Integer> chain = splitRoad(road, layout, nodes);
            for (int s = 1; s < chain.size(); s++) {
                edges.add(new Edge(chain.get(s - 1), chain.get(s)));
            }
            chains.put(road, chain);
        }

        List<BusLine> busLines = new ArrayList<>();
        linePaths.forEach((number, path) -> busLines.add(new BusLine(number, expand(path, chains))));
        return new CityMap(city.width(), city.height(), nodes, edges, busLines);
    }

    /** Ajoute les points de route de {@code road} à {@code nodes} ; renvoie la chaîne de nœuds de a à b. */
    private static List<Integer> splitRoad(Edge road, RoadLayout layout, List<Node> nodes) {
        RoadLayout.Point p = layout.intersections().get(road.a()), q = layout.intersections().get(road.b());
        int segments = Math.max(1, (int) Math.round(p.distanceTo(q) / SEGMENT_LENGTH));
        List<Integer> chain = new ArrayList<>(List.of(road.a()));
        for (int s = 1; s < segments; s++) {
            double t = (double) s / segments;
            nodes.add(new Node(nodes.size(), p.x() + t * (q.x() - p.x()), p.y() + t * (q.y() - p.y()), Node.Type.ROAD_POINT));
            chain.add(nodes.size() - 1);
        }
        chain.add(road.b());
        return chain;
    }

    /** Transforme un chemin d'intersections en chemin complet de nœuds, points de route compris. */
    private static List<Integer> expand(List<Integer> intersections, Map<Edge, List<Integer>> chains) {
        List<Integer> nodeIds = new ArrayList<>(List.of(intersections.getFirst()));
        for (int i = 1; i < intersections.size(); i++) {
            int from = intersections.get(i - 1), to = intersections.get(i);
            List<Integer> chain = new ArrayList<>(chains.get(new Edge(from, to)));
            if (chain.getFirst() != from) {
                Collections.reverse(chain);
            }
            nodeIds.addAll(chain.subList(1, chain.size()));
        }
        return nodeIds;
    }
}

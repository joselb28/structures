package com.transit.model;

import com.transit.model.BusLine;
import com.transit.model.CityMap;
import com.transit.model.Edge;
import com.transit.model.Node;

import com.transit.map.MapGenerator;
import com.transit.settings.Settings;

import java.util.List;

/** Petites cartes faites à la main pour les tests. */
public final class TestMaps {

    private TestMaps() {
    }

    /**
     * Une rue droite, ligne 5 du nœud 0 au nœud 3, arrêts à 0, 2 et 3 :
     * <pre>
     *   0 ---- 1 ---- 2 ---- 3        (0, 2, 3 : intersections ; 1 : point de route)
     *                 |
     *                 4               (cul-de-sac)
     * </pre>
     */
    public static CityMap street() {
        List<Node> nodes = List.of(
                new Node(0, 0, 0, Node.Type.INTERSECTION),
                new Node(1, 40, 0, Node.Type.ROAD_POINT),
                new Node(2, 80, 0, Node.Type.INTERSECTION),
                new Node(3, 120, 0, Node.Type.INTERSECTION),
                new Node(4, 80, 40, Node.Type.INTERSECTION));
        List<Edge> edges = List.of(new Edge(0, 1), new Edge(1, 2), new Edge(2, 3), new Edge(2, 4));
        return new CityMap(200, 200, nodes, edges, List.of(new BusLine(5, List.of(0, 1, 2, 3))));
    }

    /**
     * Deux lignes qui se croisent à l'arrêt 2 (arrêt de correspondance) :
     * <pre>
     *              5
     *              |
     *   0 -- 1 --- 2 --- 3       ligne 5 : 0 1 2 3   (1 est un point de route)
     *              |
     *              4
     *              |
     *              6             ligne 7 : 5 2 4 6
     * </pre>
     */
    public static CityMap cross() {
        List<Node> nodes = List.of(
                new Node(0, 0, 100, Node.Type.INTERSECTION),
                new Node(1, 40, 100, Node.Type.ROAD_POINT),
                new Node(2, 80, 100, Node.Type.INTERSECTION),
                new Node(3, 120, 100, Node.Type.INTERSECTION),
                new Node(4, 80, 140, Node.Type.INTERSECTION),
                new Node(5, 80, 60, Node.Type.INTERSECTION),
                new Node(6, 80, 180, Node.Type.INTERSECTION));
        List<Edge> edges = List.of(new Edge(0, 1), new Edge(1, 2), new Edge(2, 3),
                new Edge(5, 2), new Edge(2, 4), new Edge(4, 6));
        return new CityMap(200, 200, nodes, edges, List.of(
                new BusLine(5, List.of(0, 1, 2, 3)),
                new BusLine(7, List.of(5, 2, 4, 6))));
    }

    /** La ville du scénario par défaut. */
    public static CityMap defaultCity() {
        Settings settings = Settings.defaults();
        return MapGenerator.generate(settings.city(), settings.seed());
    }
}

package com.transit.map;

import com.transit.model.Edge;
import com.transit.settings.Settings;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Les intersections et les routes qui les relient, avant le découpage en tronçons.
 *
 * @param intersections positions des intersections ; l'indice d'une intersection est son identifiant
 * @param roads         routes non orientées entre indices d'intersections
 */
record RoadLayout(List<Point> intersections, List<Edge> roads) {

    /** Aucune intersection plus près que ça du bord de la carte. */
    static final double MARGIN = 150;
    /** Points tirés au hasard par l'échantillonneur ; assez pour remplir même la ville la plus dense permise. */
    static final int SAMPLING_ATTEMPTS = 30_000;

    record Point(double x, double y) {
        double distanceTo(Point o) {
            return Math.hypot(x - o.x, y - o.y);
        }
    }

    // TODO Q15 (★★★) : second mode de génération : une ville en damier (tableau 2D) — voir docs/QUESTIONS.md
    static RoadLayout generate(Settings.City city, Random rng) {
        List<Point> intersections = sampleIntersections(city, rng);
        return new RoadLayout(intersections, gabrielGraph(intersections));
    }

    /**
     * Échantillonnage de Poisson par « lancer de fléchettes » : on garde un point au hasard seulement s'il est loin de tous les autres.
     */
    static List<Point> sampleIntersections(Settings.City city, Random rng) {
        List<Point> points = new ArrayList<>();
        double minX = MARGIN, maxX = city.width() - MARGIN;
        double minY = MARGIN, maxY = city.height() - MARGIN;
        for (int attempt = 0; attempt < SAMPLING_ATTEMPTS; attempt++) {
            Point candidate = new Point(minX + rng.nextDouble() * (maxX - minX), minY + rng.nextDouble() * (maxY - minY));
            if (points.stream().allMatch(p -> p.distanceTo(candidate) >= city.intersectionSpacing())) {
                points.add(candidate);
            }
        }
        return points;
    }

    /**
     * La route (i, j) existe si et seulement si aucun autre point n'est dans le cercle de diamètre [i, j].
     * Le résultat est planaire et contient l'arbre couvrant minimal, donc il est connexe.
     */
    static List<Edge> gabrielGraph(List<Point> points) {
        List<Edge> roads = new ArrayList<>();
        for (int i = 0; i < points.size(); i++) {
            for (int j = i + 1; j < points.size(); j++) {
                if (circleIsEmpty(points, i, j)) {
                    roads.add(new Edge(i, j));
                }
            }
        }
        return roads;
    }

    /** Test de Gabriel : aucun autre point dans le cercle de diamètre [i, j]. */
    private static boolean circleIsEmpty(List<Point> points, int i, int j) {
        Point p = points.get(i), q = points.get(j);
        Point center = new Point((p.x() + q.x()) / 2, (p.y() + q.y()) / 2);
        double radius = p.distanceTo(q) / 2;
        for (int k = 0; k < points.size(); k++) {
            if (k != i && k != j && points.get(k).distanceTo(center) < radius) {
                return false;
            }
        }
        return true;
    }
}

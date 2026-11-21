package com.transit.model;

/**
 * Un sommet du réseau routier. Son {@code id} est aussi son indice dans {@link CityMap}.
 */
public record Node(int id, double x, double y, Type type) {

    public enum Type {
        /** Extrémité d'une route ; un arrêt de bus si au moins une ligne la dessert. */
        INTERSECTION,
        /** Point intermédiaire le long d'une route (un nœud traversé = une minute simulée). */
        ROAD_POINT
    }

    public double distanceTo(Node other) {
        return Math.hypot(x - other.x, y - other.y);
    }
}

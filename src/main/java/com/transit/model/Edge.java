package com.transit.model;

/**
 * Tronçon non orienté entre deux nœuds voisins, normalisé pour que {@code a < b}.
 */
public record Edge(int a, int b) {

    public Edge {
        if (a == b) {
            throw new IllegalArgumentException("Self-loop on node " + a);
        }
        if (a > b) {
            int tmp = a;
            a = b;
            b = tmp;
        }
    }
}

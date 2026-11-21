package com.transit.model;

import java.util.List;

/**
 * Une ligne de bus numérotée : une liste ordonnée de nœuds voisins, parcourue en aller-retour.
 * Ses deux terminus sont des intersections.
 */
public record BusLine(int number, List<Integer> nodeIds) {

    public BusLine {
        if (nodeIds.size() < 2) {
            throw new IllegalArgumentException("Line " + number + " needs at least two nodes");
        }
        nodeIds = List.copyOf(nodeIds);
    }
}

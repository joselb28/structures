package com.transit.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Le réseau routier en mémoire, immuable : nœuds, listes d'adjacence et lignes de bus.
 *
 * <p>Chargé une fois au démarrage ; lu en même temps par le moteur et par l'interface,
 * sans synchronisation, puisque rien n'y change jamais.
 */
public final class CityMap {

    private final int width;
    private final int height;
    private final List<Node> nodes;
    private final List<Edge> edges;
    private final int[][] neighbors;
    private final List<BusLine> busLines;
    private final Map<Integer, Set<Integer>> linesByNode;

    /**
     * @param width    largeur de l'espace virtuel, en pixels
     * @param height   hauteur de l'espace virtuel, en pixels
     * @param nodes    nœuds tels que {@code nodes.get(i).id() == i}
     * @param edges    tronçons non orientés entre nœuds existants
     * @param busLines lignes dont les nœuds consécutifs sont voisins
     */
    public CityMap(int width, int height, List<Node> nodes, List<Edge> edges, List<BusLine> busLines) {
        for (int i = 0; i < nodes.size(); i++) {
            if (nodes.get(i).id() != i) {
                throw new IllegalArgumentException("Node at index " + i + " has id " + nodes.get(i).id());
            }
        }
        this.width = width;
        this.height = height;
        this.nodes = List.copyOf(nodes);
        this.edges = List.copyOf(edges);
        this.busLines = List.copyOf(busLines);
        this.neighbors = buildAdjacency(nodes.size(), edges);
        this.linesByNode = indexLines();
    }

    /**
     * Transforme la liste des tronçons en un tableau de voisins par nœud, de taille exacte : une première
     * passe compte le degré de chaque nœud, une seconde remplit les tableaux.
     */
    private static int[][] buildAdjacency(int nodeCount, List<Edge> edges) {
        int[] degree = new int[nodeCount];
        for (Edge e : edges) {
            degree[e.a()]++;
            degree[e.b()]++;
        }
        int[][] adjacency = new int[nodeCount][];
        for (int i = 0; i < nodeCount; i++) {
            adjacency[i] = new int[degree[i]];
        }
        int[] fill = new int[nodeCount];
        for (Edge e : edges) {
            adjacency[e.a()][fill[e.a()]++] = e.b();
            adjacency[e.b()][fill[e.b()]++] = e.a();
        }
        return adjacency;
    }

    /**
     * Arrêt -> numéros des lignes qui s'y arrêtent, en vérifiant au passage que chaque ligne ne suit que
     * des tronçons existants.
     */
    private Map<Integer, Set<Integer>> indexLines() {
        Map<Integer, Set<Integer>> index = new HashMap<>();
        for (BusLine line : busLines) {
            List<Integer> ids = line.nodeIds();
            for (int i = 0; i < ids.size(); i++) {
                int id = ids.get(i);
                if (i > 0 && !areAdjacent(ids.get(i - 1), id)) {
                    throw new IllegalArgumentException(
                            "Line " + line.number() + ": nodes " + ids.get(i - 1) + " and " + id + " are not adjacent");
                }
                if (nodes.get(id).type() == Node.Type.INTERSECTION) {
                    index.computeIfAbsent(id, k -> new TreeSet<>()).add(line.number());
                }
            }
        }
        index.replaceAll((k, v) -> Collections.unmodifiableSet(v));
        return Map.copyOf(index);
    }

    // TODO Q5 (★) : degré moyen, intersection la plus connectée, culs-de-sac (listes d'adjacence) — voir docs/QUESTIONS.md
    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public int nodeCount() {
        return nodes.size();
    }

    public Node node(int id) {
        return nodes.get(id);
    }

    public List<Node> nodes() {
        return nodes;
    }

    public List<Edge> edges() {
        return edges;
    }

    /** Les voisins d'un nœud. Le tableau renvoyé est partagé : l'appelant ne doit pas le modifier. */
    public int[] neighbors(int id) {
        return neighbors[id];
    }

    public boolean areAdjacent(int a, int b) {
        for (int n : neighbors[a]) {
            if (n == b) {
                return true;
            }
        }
        return false;
    }

    public List<BusLine> busLines() {
        return busLines;
    }

    /** Les numéros des lignes qui s'arrêtent à ce nœud (seules les intersections sont des arrêts). */
    public Set<Integer> linesAt(int nodeId) {
        return linesByNode.getOrDefault(nodeId, Set.of());
    }

    /** Une intersection où au moins une ligne s'arrête. */
    public boolean isStop(int nodeId) {
        return linesByNode.containsKey(nodeId);
    }

    /** Un arrêt partagé où l'on peut changer de ligne (deux lignes ou plus). */
    public boolean isTransferStop(int nodeId) {
        return linesAt(nodeId).size() >= 2;
    }

    /** Le premier et le dernier arrêt de chaque ligne. */
    public Set<Integer> terminals() {
        Set<Integer> terminals = new TreeSet<>();
        for (BusLine line : busLines) {
            terminals.add(line.nodeIds().getFirst());
            terminals.add(line.nodeIds().getLast());
        }
        return Collections.unmodifiableSet(terminals);
    }

    public List<Integer> intersectionIds() {
        List<Integer> ids = new ArrayList<>();
        for (Node n : nodes) {
            if (n.type() == Node.Type.INTERSECTION) {
                ids.add(n.id());
            }
        }
        return ids;
    }
}

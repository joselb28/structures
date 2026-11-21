package com.transit.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * L'algorithme de Dijkstra sur n'importe quel graphe, décrit par une fonction qui donne les pas
 * possibles depuis un nœud.
 *
 * <p>Un nœud peut être n'importe quoi qui a un {@code equals}/{@code hashCode} correct (un {@code Integer},
 * un record...). Sert à tracer les lignes de bus (sur les intersections), à planifier les trajets
 * multimodaux (sur les états (arrêt, ligne)) et à guider les Uber (sur le réseau routier).
 */
public final class ShortestPath {

    /** Un tronçon sortant : aller à {@code to} pour un coût {@code cost} (doit être &gt;= 0). */
    public record Step<N>(N to, double cost) {
    }

    /** Les nœuds d'une source à une cible (les deux inclus) et le coût total. */
    public record Path<N>(List<N> nodes, double cost) {
    }

    private record Entry<N>(N node, double cost) {
    }

    private ShortestPath() {
    }

    /**
     * @param sources   nœuds de départ, tous à coût 0
     * @param isTarget  la recherche s'arrête au premier nœud (le moins cher) accepté par ce prédicat
     * @param neighbors les pas possibles depuis un nœud
     * @return le chemin le moins cher, ou vide si aucune cible n'est atteignable
     */
    public static <N> Optional<Path<N>> find(Collection<N> sources, Predicate<N> isTarget,
                                             Function<N, ? extends Iterable<Step<N>>> neighbors) {
        Map<N, Double> cost = new HashMap<>();
        Map<N, N> previous = new HashMap<>();
        PriorityQueue<Entry<N>> queue = new PriorityQueue<>((a, b) -> Double.compare(a.cost(), b.cost()));
        for (N source : sources) {
            cost.put(source, 0.0);
            queue.add(new Entry<>(source, 0));
        }

        while (!queue.isEmpty()) {
            Entry<N> head = queue.poll();
            if (head.cost() > cost.get(head.node())) {
                continue; // entrée périmée : un chemin moins cher a été trouvé après son ajout dans la file
            }
            if (isTarget.test(head.node())) {
                return Optional.of(new Path<>(walkBack(head.node(), previous), head.cost()));
            }
            for (Step<N> step : neighbors.apply(head.node())) {
                double c = head.cost() + step.cost();
                if (c < cost.getOrDefault(step.to(), Double.POSITIVE_INFINITY)) {
                    cost.put(step.to(), c);
                    previous.put(step.to(), head.node());
                    queue.add(new Entry<>(step.to(), c));
                }
            }
        }
        return Optional.empty();
    }

    /** Reconstruit le chemin en remontant le prédécesseur de chaque nœud jusqu'à une source. */
    private static <N> List<N> walkBack(N target, Map<N, N> previous) {
        List<N> nodes = new ArrayList<>();
        for (N n = target; n != null; n = previous.get(n)) {
            nodes.add(n);
        }
        Collections.reverse(nodes);
        return nodes;
    }
}

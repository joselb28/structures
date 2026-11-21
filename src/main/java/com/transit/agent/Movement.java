package com.transit.agent;

import com.transit.model.CityMap;
import com.transit.model.Time;

/**
 * Déplacement de nœud en nœud sur le réseau routier, commun à tous les véhicules (composition, pas héritage).
 *
 * <p>Traverser un tronçon prend {@link Time#TICKS_PER_SIM_MINUTE} ticks : un nœud = une minute simulée.
 * La position est interpolée linéairement le long du tronçon, et la position d'avant le dernier
 * {@link #step()} est gardée pour que l'interface puisse interpoler entre deux ticks.
 */
public final class Movement {

    private final CityMap map;
    private int from;
    private int to;
    private int ticksOnEdge;
    private double previousX;
    private double previousY;

    public Movement(CityMap map, int startNode) {
        this.map = map;
        this.from = startNode;
        this.to = startNode;
        this.previousX = map.node(startNode).x();
        this.previousY = map.node(startNode).y();
    }

    /** @return true s'il est arrêté sur un nœud, prêt pour {@link #headTo(int)}. */
    public boolean isIdle() {
        return from == to;
    }

    /** Le nœud où il se trouve, ou le nœud qu'il vient de quitter s'il roule. */
    public int currentNode() {
        return from;
    }

    /** Le nœud vers lequel il roule (égal à {@link #currentNode()} à l'arrêt). */
    public int targetNode() {
        return to;
    }

    /**
     * Commence à traverser le tronçon vers {@code neighbor}.
     *
     * @throws IllegalStateException    s'il roule déjà : un tronçon est toujours traversé jusqu'au bout
     * @throws IllegalArgumentException si {@code neighbor} n'est pas voisin du nœud courant
     */
    public void headTo(int neighbor) {
        if (!isIdle()) {
            throw new IllegalStateException("Already moving from " + from + " to " + to);
        }
        if (!map.areAdjacent(from, neighbor)) {
            throw new IllegalArgumentException(neighbor + " is not adjacent to " + from);
        }
        to = neighbor;
        ticksOnEdge = 0;
    }

    /**
     * Avance d'un tick. À appeler à chaque tick, même à l'arrêt, pour que {@link #previousX()} reste juste.
     *
     * @return true si ce tick a terminé le tronçon (le véhicule est maintenant arrêté sur {@link #currentNode()}).
     */
    public boolean step() {
        previousX = x();
        previousY = y();
        if (isIdle()) {
            return false;
        }
        ticksOnEdge++;
        if (ticksOnEdge < Time.TICKS_PER_SIM_MINUTE) {
            return false;
        }
        from = to;
        ticksOnEdge = 0;
        return true;
    }

    public double x() {
        return lerp(map.node(from).x(), map.node(to).x());
    }

    public double y() {
        return lerp(map.node(from).y(), map.node(to).y());
    }

    public double previousX() {
        return previousX;
    }

    public double previousY() {
        return previousY;
    }

    private double lerp(double a, double b) {
        return a + (b - a) * ticksOnEdge / Time.TICKS_PER_SIM_MINUTE;
    }
}

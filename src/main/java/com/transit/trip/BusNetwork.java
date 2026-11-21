package com.transit.trip;

import com.transit.graph.ShortestPath.Step;
import com.transit.model.BusLine;
import com.transit.model.CityMap;
import com.transit.model.Time;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Le réseau de bus vu comme un graphe, pour chercher des itinéraires.
 *
 * <p>Un nœud est un {@link Boarding} : « à cet arrêt, sur cette ligne ». À partir de là, on peut
 * <ul>
 *   <li><b>rouler</b> jusqu'à l'arrêt précédent ou suivant de la ligne : temps de trajet (une minute par nœud)
 *       plus la minute d'arrêt ;</li>
 *   <li><b>changer de ligne</b> au même arrêt : l'attente estimée par les clients pour le prochain bus
 *       ({@code customers.transferMinutes} dans le scénario).</li>
 * </ul>
 * Immuable une fois construit : on peut le lire depuis n'importe quel thread.
 */
public final class BusNetwork {

    static final int DWELL_MINUTES = Time.STOP_DWELL_TICKS / Time.TICKS_PER_SIM_MINUTE;

    /** À {@code stop}, sur la ligne {@code line}. */
    public record Boarding(int stop, int line) {
    }

    private final CityMap map;
    private final int transferMinutes;
    private final Map<Boarding, List<Step<Boarding>>> steps = new HashMap<>();

    public BusNetwork(CityMap map, int transferMinutes) {
        this.map = map;
        this.transferMinutes = transferMinutes;
        map.busLines().forEach(this::addRides);
        map.intersectionIds().forEach(this::addTransfers);
    }

    /**
     * Relie chaque arrêt de la ligne au suivant, dans les deux sens ; les points de route entre les deux ajoutent des minutes.
     */
    private void addRides(BusLine line) {
        List<Integer> nodes = line.nodeIds();
        int previousIndex = -1;
        for (int i = 0; i < nodes.size(); i++) {
            if (!map.isStop(nodes.get(i))) {
                continue;
            }
            if (previousIndex >= 0) {
                int minutes = (i - previousIndex) + DWELL_MINUTES;
                link(new Boarding(nodes.get(previousIndex), line.number()), new Boarding(nodes.get(i), line.number()), minutes);
                link(new Boarding(nodes.get(i), line.number()), new Boarding(nodes.get(previousIndex), line.number()), minutes);
            }
            previousIndex = i;
        }
    }

    /** À un arrêt desservi par plusieurs lignes, relie chaque ligne à toutes les autres. */
    private void addTransfers(int stop) {
        for (int from : map.linesAt(stop)) {
            for (int to : map.linesAt(stop)) {
                if (from != to) {
                    link(new Boarding(stop, from), new Boarding(stop, to), transferMinutes);
                }
            }
        }
    }

    private void link(Boarding from, Boarding to, double minutes) {
        steps.computeIfAbsent(from, k -> new ArrayList<>()).add(new Step<>(to, minutes));
    }

    /** Où peut commencer un trajet qui part de {@code stop} : une montée par ligne qui le dessert. */
    public List<Boarding> boardingsAt(int stop) {
        return map.linesAt(stop).stream().map(line -> new Boarding(stop, line)).toList();
    }

    public List<Step<Boarding>> steps(Boarding from) {
        return steps.getOrDefault(from, List.of());
    }
}

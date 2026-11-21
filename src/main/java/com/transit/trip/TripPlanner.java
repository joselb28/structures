package com.transit.trip;

import com.transit.graph.ShortestPath;
import com.transit.model.CityMap;
import com.transit.trip.BusNetwork.Boarding;

import java.util.ArrayList;
import java.util.List;

/**
 * Planifie les trajets des clients.
 *
 * <p>En bus : Dijkstra sur le {@link BusNetwork}, puis les arrêts consécutifs sur une même ligne sont
 * regroupés en {@link BusRide} : une par ligne empruntée.
 *
 * <p>Immuable une fois construit : on peut l'appeler depuis n'importe quel thread.
 */
public final class TripPlanner {

    private final CityMap map;
    private final BusNetwork graph;

    /**
     * @param transferMinutes attente estimée à une correspondance, qui fait coûter du temps aux changements de ligne
     */
    public TripPlanner(CityMap map, int transferMinutes) {
        this.map = map;
        this.graph = new BusNetwork(map, transferMinutes);
    }

    // TODO Q7 (★★) : mémoriser les itinéraires déjà calculés (cache HashMap) — voir docs/QUESTIONS.md
    /**
     * @return les courses en bus les plus rapides de {@code origin} à {@code destination} (deux arrêts), ou
     *         une liste vide si c'est le même arrêt, si ce ne sont pas des arrêts, ou s'ils ne sont pas reliés
     */
    public List<BusRide> planByBus(int origin, int destination) {
        if (origin == destination || !map.isStop(origin) || !map.isStop(destination)) {
            return List.of();
        }
        return ShortestPath.find(graph.boardingsAt(origin), b -> b.stop() == destination, graph::steps)
                .map(path -> toRides(path.nodes()))
                .orElse(List.of());
    }

    /**
     * Une nouvelle course commence à chaque changement de ligne (une correspondance se fait toujours à un seul arrêt).
     */
    private static List<BusRide> toRides(List<Boarding> path) {
        List<BusRide> rides = new ArrayList<>();
        Boarding rideStart = path.getFirst();
        for (int i = 1; i < path.size(); i++) {
            Boarding previous = path.get(i - 1), current = path.get(i);
            if (current.line() != previous.line()) {
                rides.add(new BusRide(previous.line(), rideStart.stop(), previous.stop()));
                rideStart = current;
            }
        }
        rides.add(new BusRide(path.getLast().line(), rideStart.stop(), path.getLast().stop()));
        return rides;
    }
}

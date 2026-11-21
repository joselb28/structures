package com.transit.agent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Quels bus sont arrêtés à quel arrêt pendant le tick courant.
 *
 * <p>Reconstruit par le thread du moteur avant la phase parallèle des clients, puis seulement lu :
 * la phase parallèle commence après le retour de {@link #refresh(List)}, ce qui rend les écritures visibles.
 */
public final class BusStops {

    private Map<Integer, List<Bus>> dwelling = Map.of();

    public void refresh(List<Bus> buses) {
        Map<Integer, List<Bus>> byStop = new HashMap<>();
        for (Bus bus : buses) {
            if (bus.isAtStop()) {
                byStop.computeIfAbsent(bus.currentNode(), k -> new ArrayList<>()).add(bus);
            }
        }
        dwelling = byStop;
    }

    public List<Bus> dwellingAt(int nodeId) {
        return dwelling.getOrDefault(nodeId, List.of());
    }
}

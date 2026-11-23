package com.transit.agent;

import com.transit.model.CityMap;
import com.transit.model.Node;
import com.transit.model.Time;
import com.transit.trip.Roads;

import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Random;

/**
 * Agent proactif. Libre, il réserve la demande ouverte la plus proche (ou erre, sans jamais faire
 * demi-tour sauf en cul-de-sac) ; ensuite il va chercher le client, le prend à bord, puis roule
 * jusqu'au dépôt, chaque fois par la route la plus rapide. Il s'arrête une minute simulée à la prise
 * en charge et au dépôt, le temps que le client monte ou descende.
 */
public final class Uber implements Agent {

    private final String id;
    private final CityMap map;
    private final Movement movement;
    private final Random random;
    private final UberRequests requests;

    private final Deque<Integer> route = new ArrayDeque<>();
    private Customer customer;
    private boolean onBoard;
    private int stopTicksLeft;
    private int previousNode = -1;

    /** Un Uber transporte un client (ou un groupe qui voyage ensemble) à la fois. */
    public static final int CAPACITY = 1;

    public Uber(String id, CityMap map, int startNode, Random random, UberRequests requests) {
        this.id = id;
        this.map = map;
        this.movement = new Movement(map, startNode);
        this.random = random;
        this.requests = requests;
    }

    @Override
    public void update(long currentTick) {
        if (customer == null) {
            claimNearestRequest();
        }
        if (stopTicksLeft > 0) {
            stopTicksLeft--; // arrêté le temps que le client monte ou descende
        } else if (movement.isIdle()) {
            if (customer != null && route.isEmpty()) {
                arrive(currentTick);
                stopTicksLeft = Time.STOP_DWELL_TICKS - 1; // ce tick compte déjà dans l'arrêt
            } else if (!route.isEmpty()) {
                movement.headTo(route.poll());
            } else if (customer == null) {
                wanderToRandomNeighbor();
            }
        }
        movement.step();
    }

    /**
     * Essaie les demandes ouvertes de la plus proche à la plus lointaine ; un autre Uber peut gagner n'importe laquelle.
     */
    private void claimNearestRequest() {
        Node here = map.node(movement.targetNode());
        List<Customer> byDistance = requests.open().stream()
                .sorted(Comparator.comparingDouble(c -> here.distanceTo(map.node(c.currentRide().from()))))
                .toList();
        for (Customer candidate : byDistance) {
            if (candidate.tryClaim()) {
                customer = candidate;
                onBoard = false;
                driveTo(candidate.currentRide().from());
                return;
            }
        }
    }

    /** À la prise en charge : prend le client à bord. Au dépôt : le fait descendre et redevient libre. */
    private void arrive(long currentTick) {
        if (!onBoard) {
            customer.pickedUp(currentTick);
            onBoard = true;
            driveTo(customer.currentRide().to());
        } else {
            customer.droppedOff(currentTick);
            customer = null;
            onBoard = false;
        }
    }

    /** Planifie la route à partir de l'endroit où l'Uber sera de nouveau arrêté. */
    private void driveTo(int destination) {
        route.clear();
        List<Integer> nodes = Roads.fastestRoute(map, movement.targetNode(), destination);
        route.addAll(nodes.subList(1, nodes.size()));
    }

    /** N'importe quel voisin sauf celui qu'il vient de quitter, sauf en cul-de-sac. */
    private void wanderToRandomNeighbor() {
        int here = movement.currentNode();
        int[] neighbors = map.neighbors(here);
        int next;
        do {
            next = neighbors[random.nextInt(neighbors.length)];
        } while (next == previousNode && neighbors.length > 1);
        previousNode = here;
        movement.headTo(next);
    }

    public VehicleView view() {
        return new VehicleView(id, VehicleView.Kind.UBER, 0, movement.previousX(), movement.previousY(),
                movement.x(), movement.y(), onBoard ? 1 : 0, CAPACITY, customer == null);
    }

    public String id() {
        return id;
    }

    public int currentNode() {
        return movement.currentNode();
    }

    public boolean isFree() {
        return customer == null;
    }
}

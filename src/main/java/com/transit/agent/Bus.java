package com.transit.agent;

import com.transit.model.BusLine;
import com.transit.model.CityMap;
import com.transit.model.Time;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Agent à itinéraire fixe : fait l'aller-retour sur sa ligne et s'arrête à chaque arrêt.
 *
 * <p>Seul le thread du moteur le fait avancer. Par contre, la montée et la descente sont appelées par
 * des clients évalués <b>en parallèle</b> : le compteur de passagers est un état partagé.
 */
public final class Bus implements Agent {

    private final String id;
    private final BusLine line;
    private final CityMap map;
    private final Movement movement;
    private final int capacity;
    private final AtomicInteger passengers = new AtomicInteger();

    private int index;          // position dans line.nodeIds()
    private int direction;      // +1 vers le dernier terminus, -1 vers le premier
    private int dwellTicksLeft;

    /**
     * @param roundTripOffset où le bus commence son aller-retour, de 0 (premier terminus, à l'aller)
     *                        à 1 (de retour au premier terminus) ; 0,5 = dernier terminus, au retour.
     *                        Répartir les bus d'une ligne sur [0, 1) les garde également espacés.
     */
    public Bus(String id, BusLine line, CityMap map, int capacity, double roundTripOffset) {
        this.id = id;
        this.line = line;
        this.map = map;
        this.capacity = capacity;
        int lastIndex = line.nodeIds().size() - 1;
        int step = (int) Math.round(roundTripOffset * 2 * lastIndex) % (2 * lastIndex); // 0 .. 2*lastIndex-1
        this.index = step <= lastIndex ? step : 2 * lastIndex - step;
        this.direction = step < lastIndex ? 1 : -1;
        this.movement = new Movement(map, line.nodeIds().get(index));
        this.dwellTicksLeft = Time.STOP_DWELL_TICKS;
    }

    @Override
    public void update(long currentTick) {
        if (movement.isIdle()) {
            if (dwellTicksLeft > 0) {
                dwellTicksLeft--;
            } else {
                departToNextNode();
            }
        }
        if (movement.step()) {
            index += direction;
            if (map.isStop(movement.currentNode())) {
                dwellTicksLeft = Time.STOP_DWELL_TICKS;
            }
        }
    }

    /** Part vers le nœud suivant de la ligne, en faisant demi-tour d'abord s'il est à un terminus. */
    private void departToNextNode() {
        List<Integer> nodes = line.nodeIds();
        if (index + direction < 0 || index + direction >= nodes.size()) {
            direction = -direction; // terminus atteint : demi-tour
        }
        movement.headTo(nodes.get(index + direction));
    }

    /**
     * Prend une place s'il en reste. Appelé en même temps par plusieurs clients pendant le même tick.
     *
     * @return false si le bus est plein : le client doit attendre le suivant
     */
    public boolean tryBoard() {
        // Vérifier puis agir doit être une seule étape atomique : on recommence si un autre thread a pris une place entre-temps.
        while (true) {
            int seated = passengers.get();
            if (seated >= capacity) {
                return false;
            }
            if (passengers.compareAndSet(seated, seated + 1)) {
                return true;
            }
        }
    }

    /** Libère une place. Appelé en même temps par les clients qui descendent. */
    public void alight() {
        passengers.decrementAndGet();
    }

    public int passengers() {
        return passengers.get();
    }

    public int capacity() {
        return capacity;
    }

    public boolean isFull() {
        return passengers() >= capacity;
    }

    /**
     * Indique si ce bus, d'où il est, atteindra {@code nodeId} sans faire demi-tour.
     * Un bus arrêté à un terminus est considéré comme ayant déjà fait demi-tour.
     */
    public boolean isHeadingTo(int nodeId) {
        int target = line.nodeIds().indexOf(nodeId);
        if (target < 0) {
            return false;
        }
        int effectiveDirection = direction;
        if (movement.isIdle() && (index + direction < 0 || index + direction >= line.nodeIds().size())) {
            effectiveDirection = -direction;
        }
        return Integer.signum(target - index) == effectiveDirection;
    }

    public VehicleView view() {
        return new VehicleView(id, VehicleView.Kind.BUS, line.number(),
                movement.previousX(), movement.previousY(), movement.x(), movement.y(), passengers(), capacity, !isFull());
    }

    public String id() {
        return id;
    }

    public BusLine line() {
        return line;
    }

    public int currentNode() {
        return movement.currentNode();
    }

    public boolean isAtStop() {
        return movement.isIdle() && map.isStop(movement.currentNode());
    }

    public int direction() {
        return direction;
    }
}

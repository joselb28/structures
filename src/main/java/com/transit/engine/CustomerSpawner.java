package com.transit.engine;

import com.transit.model.CityMap;
import com.transit.model.Time;
import com.transit.settings.Settings;
import com.transit.trip.Ride;
import com.transit.trip.TripPlanner;
import com.transit.trip.UberRide;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Décide combien de clients apparaissent à chaque tick, d'où ils partent et où ils vont, et planifie
 * leur trajet. Chaque proportion et chaque débit vient du scénario (clés {@code customers.*}).
 *
 * <ul>
 *   <li>Personne n'apparaît à un terminus (ça engorgerait les fins de ligne).</li>
 *   <li>{@code offNetworkShare} des clients partent ou vont à une intersection sans arrêt :
 *       sans marche à pied, ils prennent un Uber pour tout le trajet.</li>
 *   <li>Les autres voyagent en bus, d'un arrêt à un arrêt ; {@code terminusShare} d'entre eux vont
 *       à un terminus.</li>
 * </ul>
 * Thread du moteur seulement, sauf {@link #setRushHour(boolean)} (drapeau volatile modifié par l'interface).
 */
public final class CustomerSpawner {

    /**
     * Nombre de tirages (départ, arrivée) essayés avant d'abandonner, quand le planificateur ne trouve pas de trajet.
     */
    private static final int ATTEMPTS = 10;

    private final Settings.Customers settings;
    private final List<Integer> departureStops;
    private final List<Integer> terminals;
    private final List<Integer> offNetwork;
    private final TripPlanner planner;
    private final Random random;
    private volatile boolean rushHour;
    private long ticks;

    public CustomerSpawner(CityMap map, TripPlanner planner, Random random, Settings.Customers settings) {
        this.settings = settings;
        this.terminals = List.copyOf(map.terminals());
        this.departureStops = map.intersectionIds().stream()
                .filter(id -> map.isStop(id) && !map.terminals().contains(id)).toList();
        this.offNetwork = map.intersectionIds().stream().filter(id -> !map.isStop(id)).toList();
        this.planner = planner;
        this.random = random;
    }

    /**
     * À appeler une fois par tick. Les clients n'apparaissent qu'un tick sur {@code spawnEveryTicks} ; les
     * autres ticks sont sautés, donc le débit réel moyen est {@code perMinute / spawnEveryTicks}.
     *
     * @return les trajets des clients qui apparaissent pendant ce tick (souvent aucun)
     */
    public List<List<Ride>> nextTrips() {
        if (++ticks % settings.spawnEveryTicks() != 0) {
            return List.of();
        }
        double perTick = settings.perMinute() * (rushHour ? settings.rushHourFactor() : 1) / Time.TICKS_PER_SIM_MINUTE;
        int count = (int) perTick + (random.nextDouble() < perTick % 1 ? 1 : 0); // ex. 0,2 -> un tick sur cinq
        List<List<Ride>> trips = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            List<Ride> trip = random.nextDouble() < settings.offNetworkShare() ? offNetworkTrip() : busTrip();
            if (!trip.isEmpty()) {
                trips.add(trip);
            }
        }
        return trips;
    }

    /** D'un arrêt ordinaire vers un terminus ({@code terminusShare} du temps) ou vers un autre arrêt. */
    private List<Ride> busTrip() {
        for (int attempt = 0; attempt < ATTEMPTS; attempt++) {
            int origin = pick(departureStops);
            int destination = random.nextDouble() < settings.terminusShare() ? pick(terminals) : pick(departureStops);
            List<Ride> rides = List.copyOf(planner.planByBus(origin, destination));
            if (!rides.isEmpty()) {
                return rides;
            }
        }
        return List.of();
    }

    /**
     * Une extrémité hors réseau, l'autre hors réseau ou à un arrêt ordinaire : un Uber fait tout le trajet.
     * L'extrémité hors réseau est le départ ou l'arrivée, au hasard.
     */
    private List<Ride> offNetworkTrip() {
        if (offNetwork.isEmpty()) {
            return busTrip();
        }
        int outside = pick(offNetwork), other = pick(random.nextBoolean() ? departureStops : offNetwork);
        if (outside == other) {
            return List.of();
        }
        return List.of(random.nextBoolean() ? new UberRide(outside, other) : new UberRide(other, outside));
    }

    private int pick(List<Integer> nodes) {
        return nodes.get(random.nextInt(nodes.size()));
    }

    public double rushHourFactor() {
        return settings.rushHourFactor();
    }

    public boolean isRushHour() {
        return rushHour;
    }

    /** Multiplie le débit d'apparition par {@code customers.rushHourFactor}. */
    public void setRushHour(boolean rushHour) {
        this.rushHour = rushHour;
    }
}

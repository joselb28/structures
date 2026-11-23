package com.transit.engine;

import com.transit.agent.Bus;
import com.transit.agent.BusStops;
import com.transit.agent.Customer;
import com.transit.agent.Uber;
import com.transit.agent.UberRequests;
import com.transit.metrics.Metrics;
import com.transit.model.BusLine;
import com.transit.model.CityMap;
import com.transit.settings.Settings;
import com.transit.trip.Fares;
import com.transit.trip.Ride;
import com.transit.trip.TripPlanner;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.locks.LockSupport;

/**
 * Le moteur de simulation : possède tous les agents et les fait avancer sur un thread dédié,
 * à un rythme fixe ({@code engine.ticksPerSecond} du scénario en ×1).
 *
 * <p>Chaque tick se déroule en quatre phases :
 * <ol>
 *   <li>les <b>bus</b> avancent, un par un, sur le thread du moteur ;</li>
 *   <li>les <b>Uber</b> agissent <b>en parallèle</b> : les libres se disputent les clients en attente (race n° 2) ;</li>
 *   <li>les <b>clients</b> réagissent <b>en parallèle</b> : montées et descentes se disputent les places
 *       des bus et les compteurs (races n° 1 et 3) ;</li>
 *   <li><b>bilan</b>, de retour sur le thread du moteur : les clients arrivés partent, de nouveaux
 *       apparaissent, et un {@link Snapshot} immuable est publié.</li>
 * </ol>
 *
 * <p>Les autres threads (l'EDT de Swing) ne font que lire le dernier snapshot et basculer les
 * commandes volatiles (vitesse, pause, heure de pointe).
 */
public final class Simulation implements Runnable {

    /** Si le moteur prend plus de retard que ça, il arrête d'essayer de rattraper. */
    private static final long MAX_LAG_NANOS = 250_000_000L;

    private final List<Bus> buses = new ArrayList<>();
    private final List<Uber> ubers = new ArrayList<>();
    private final List<Customer> customers = new ArrayList<>();
    private final BusStops busStops = new BusStops();
    private final Metrics metrics = new Metrics();
    private final UberRequests uberRequests = new UberRequests();
    private final CustomerSpawner spawner;

    private final int ticksPerSecond;
    private final Fares fares;
    private long tick;
    private int nextCustomerId;
    private volatile int speed = 1;
    private volatile boolean paused;
    private volatile Snapshot snapshot;

    public Simulation(CityMap map, Settings settings) {
        Random random = new Random(settings.seed());
        this.ticksPerSecond = settings.ticksPerSecond();
        this.fares = Fares.of(settings.fares());
        addBuses(map, settings.fleet());
        List<Integer> intersections = map.intersectionIds();
        for (int i = 1; i <= settings.fleet().ubers(); i++) {
            int start = intersections.get(random.nextInt(intersections.size()));
            ubers.add(new Uber("UBER-" + i, map, start, random, uberRequests));
        }
        TripPlanner planner = new TripPlanner(map, settings.customers().transferMinutes());
        spawner = new CustomerSpawner(map, planner, random, settings.customers());
        publishSnapshot();
    }

    /** Les bus d'une ligne s'appellent 14A, 14B... et sont répartis également sur l'aller-retour. */
    private void addBuses(CityMap map, Settings.Fleet fleet) {
        for (BusLine line : map.busLines()) {
            for (int i = 0; i < fleet.busesPerLine(); i++) {
                String id = line.number() + String.valueOf((char) ('A' + i));
                buses.add(new Bus(id, line, map, fleet.busCapacity(), (double) i / fleet.busesPerLine()));
            }
        }
    }

    /** Démarre le moteur sur son propre thread (démon). */
    public Thread start() {
        Thread thread = new Thread(this, "simulation-engine");
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    @Override
    public void run() {
        long nextTick = System.nanoTime();
        while (!Thread.currentThread().isInterrupted()) {
            if (!paused) {
                tick();
            }
            nextTick += tickPeriodNanos();
            long wait = nextTick - System.nanoTime();
            if (wait > 0) {
                LockSupport.parkNanos(wait);
            } else if (-wait > MAX_LAG_NANOS) {
                nextTick = System.nanoTime(); // trop de retard : on abandonne l'arriéré plutôt que de s'emballer
            }
        }
    }

    /** Fait avancer le monde d'un tick. Thread du moteur seulement (ou tests). */
    public void tick() {
        tick++;

        // 1. Bus (un par un)
        for (Bus bus : buses) {
            bus.update(tick);
        }
        busStops.refresh(buses);

        // 2. Uber (en parallèle) : les Uber libres se disputent les demandes ouvertes
        // TODO Q12 (★★★) : un dispatcher central qui apparie Uber et clients (PriorityQueue de paires) — voir docs/QUESTIONS.md
        uberRequests.refresh(customers);
        ubers.parallelStream().forEach(u -> u.update(tick));

        // 3. Clients (en parallèle), en deux temps : tout le monde descend, puis tout le monde monte.
        //    parallelStream() ne rend la main qu'une fois tous les clients traités.
        // TODO Q13 (★★★) : remplacer parallelStream par un ExecutorService (invokeAll) — voir docs/QUESTIONS.md
        customers.parallelStream().forEach(c -> c.getOffIfArrived(tick));
        customers.parallelStream().forEach(c -> c.boardIfPossible(tick));

        // 4. Bilan (un par un)
        // TODO Q8 (★★) : retirer les clients qui abandonnent, avec une PriorityQueue triée par échéance — voir docs/QUESTIONS.md
        // TODO Q11 (★★★) : vider le journal des événements (ConcurrentLinkedQueue) et garder les 10 derniers — voir docs/QUESTIONS.md
        customers.removeIf(c -> c.state() == Customer.State.ARRIVED);
        for (List<Ride> trip : spawner.nextTrips()) {
            customers.add(new Customer(nextCustomerId++, trip, tick, busStops, metrics, fares));
            metrics.recordSpawn();
        }
        publishSnapshot();
    }

    private void publishSnapshot() {
        snapshot = Snapshot.capture(tick, buses, ubers, customers, metrics, tickPeriodNanos());
    }

    private long tickPeriodNanos() {
        return 1_000_000_000L / ((long) ticksPerSecond * speed);
    }

    public Snapshot snapshot() {
        return snapshot;
    }

    public Fares fares() {
        return fares;
    }

    /** Ticks par seconde en ×1. */
    public int ticksPerSecond() {
        return ticksPerSecond;
    }

    public int speed() {
        return speed;
    }

    /** Facteur d'avance rapide : multiplie les ticks par seconde ; le comportement simulé ne change pas. */
    public void setSpeed(int speed) {
        if (speed < 1) {
            throw new IllegalArgumentException("speed must be >= 1");
        }
        this.speed = speed;
    }

    public boolean isPaused() {
        return paused;
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    public CustomerSpawner spawner() {
        return spawner;
    }

    public Metrics metrics() {
        return metrics;
    }

    public List<Customer> customers() {
        return List.copyOf(customers);
    }

    public List<Bus> buses() {
        return List.copyOf(buses);
    }

    public List<Uber> ubers() {
        return List.copyOf(ubers);
    }
}

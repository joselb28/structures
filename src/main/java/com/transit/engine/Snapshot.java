package com.transit.engine;

import com.transit.agent.Bus;
import com.transit.agent.Customer;
import com.transit.agent.CustomerView;
import com.transit.agent.Uber;
import com.transit.agent.VehicleView;
import com.transit.metrics.Metrics;
import com.transit.model.Time;
import com.transit.trip.BusRide;
import com.transit.trip.UberRide;

import java.util.ArrayList;
import java.util.List;

/**
 * État immuable du monde, publié par le moteur après chaque tick.
 * L'interface ne lit que des snapshots : elle ne voit donc jamais un monde à moitié mis à jour.
 *
 * @param waiting          les clients qui attendent un bus ou un Uber
 * @param publishedAtNanos {@link System#nanoTime()} au moment de la publication
 * @param tickPeriodNanos  durée actuelle d'un tick (plus courte en avance rapide)
 */
public record Snapshot(long tick, List<VehicleView> vehicles, List<CustomerView> waiting,
                       Stats stats, long publishedAtNanos, long tickPeriodNanos) {

    public Snapshot {
        vehicles = List.copyOf(vehicles);
        waiting = List.copyOf(waiting);
    }

    /** Prend la photo du monde à la fin d'un tick. Thread du moteur seulement. */
    static Snapshot capture(long tick, List<Bus> buses, List<Uber> ubers, List<Customer> customers,
                            Metrics metrics, long tickPeriodNanos) {
        List<VehicleView> vehicles = new ArrayList<>(buses.size() + ubers.size());
        buses.forEach(b -> vehicles.add(b.view()));
        ubers.forEach(u -> vehicles.add(u.view()));

        List<CustomerView> waiting = new ArrayList<>();
        long inTransit = 0;
        for (Customer c : customers) {
            if (c.isWaiting()) {
                waiting.add(view(c));
            } else if (c.state() == Customer.State.IN_TRANSIT) {
                inTransit++;
            }
        }
        // TODO Q2 (★) : compter les clients en attente par ligne (Map<Integer, Integer>) — voir docs/QUESTIONS.md
        // TODO Q3 (★) : taux d'occupation moyen des bus et bus le plus chargé — voir docs/QUESTIONS.md
        int fullBuses = (int) buses.stream().filter(Bus::isFull).count();
        Stats stats = new Stats(metrics.spawned(), customers.size() - inTransit, inTransit, metrics.delivered(),
                metrics.transfers(), metrics.averageWaitMinutes(), metrics.averageTripMinutes(), fullBuses,
                metrics.busFares(), metrics.busRevenueCents(), metrics.uberFares(), metrics.uberRevenueCents(),
                metrics.averageTripPriceCents());

        return new Snapshot(tick, vehicles, waiting, stats, System.nanoTime(), tickPeriodNanos);
    }

    private static CustomerView view(Customer c) {
        return switch (c.currentRide()) {
            case BusRide ride -> new CustomerView(c.id(), ride.from(), CustomerView.Mode.BUS, ride.line(), c.waitingSince());
            case UberRide ride -> new CustomerView(c.id(), ride.from(), CustomerView.Mode.UBER, 0, c.waitingSince());
        };
    }

    public long simulatedMinutes() {
        return tick / Time.TICKS_PER_SIM_MINUTE;
    }

    /** Les chiffres du tableau de bord à la fin du tick. Les durées sont en minutes simulées. */
    public record Stats(long spawned, long waiting, long inTransit, long delivered, long transfers,
                        double averageWaitMinutes, double averageTripMinutes, int fullBuses,
                        long busFares, long busRevenueCents, long uberFares, long uberRevenueCents,
                        double averageTripPriceCents) {
    }
}

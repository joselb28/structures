package com.transit.agent;

import com.transit.metrics.Metrics;
import com.transit.trip.BusRide;
import com.transit.trip.Fares;
import com.transit.trip.Ride;
import com.transit.trip.UberRide;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Agent réactif qui suit un trajet composé de courses ({@link Ride}).
 *
 * <ul>
 *   <li>{@link BusRide} : le client attend à l'arrêt un bus de la ligne qui va dans le bon sens,
 *       monte s'il reste une place, et descend au dernier arrêt de la course.</li>
 *   <li>{@link UberRide} : le client attend ; un Uber le réserve ({@link #tryClaim()}), vient le
 *       chercher et le dépose. C'est l'Uber qui fait avancer ces étapes.</li>
 * </ul>
 *
 * <p>Concurrence : les clients sont évalués en parallèle, les Uber aussi, et plusieurs Uber peuvent
 * vouloir réserver le même client pendant le même tick (race n° 2). Les champs ordinaires sont écrits
 * et lus dans des phases différentes ; le fork/join de chaque phase parallèle ordonne ces accès.
 */
public final class Customer implements Agent {

    public enum State {
        WAITING,
        /** Réservé par un Uber qui est en route. */
        ASSIGNED,
        IN_TRANSIT,
        ARRIVED
    }

    private final int id;
    private final List<Ride> trip;
    private final long spawnTick;
    private final BusStops stops;
    private final Metrics metrics;
    private final Fares fares;

    private final AtomicReference<State> state = new AtomicReference<>(State.WAITING);
    private int rideIndex;
    private long waitingSince;
    private long uberPickupTick;
    private boolean busFarePaid;
    private long paidCents;
    private Bus bus;

    /** @param trip non vide, chaque course partant de là où la précédente se termine */
    public Customer(int id, List<? extends Ride> trip, long spawnTick, BusStops stops, Metrics metrics, Fares fares) {
        if (trip.isEmpty()) {
            throw new IllegalArgumentException("Customer " + id + " has nowhere to go");
        }
        this.id = id;
        this.trip = List.<Ride>copyOf(trip);
        this.spawnTick = spawnTick;
        this.waitingSince = spawnTick;
        this.stops = stops;
        this.metrics = metrics;
        this.fares = fares;
    }

   /**
     * Les deux temps d'un tick, dans l'ordre : descendre, puis monter. Le moteur, lui, fait
     * descendre <b>tous</b> les clients avant d'en faire monter un seul (voir {@code Simulation.tick}).
     */
    @Override
    public void update(long currentTick) {
        getOffIfArrived(currentTick);
        boardIfPossible(currentTick);
    }

    /** Premier temps : un client en bus descend s'il est arrivé au dernier arrêt de sa course. */
    public void getOffIfArrived(long currentTick) {
        if (state() == State.IN_TRANSIT && currentRide() instanceof BusRide ride) {
            getOffAtLastStop(ride, currentTick);
        }
    }

    /** Second temps : un client qui attend un bus essaie de monter (les courses Uber sont pilotées par l'Uber). */
    public void boardIfPossible(long currentTick) {
        if (state() == State.WAITING && currentRide() instanceof BusRide ride) {
            tryToBoard(ride, currentTick);
        }
    }

    /**
     * Monte dans le premier bus arrêté à l'arrêt qui dessert la ligne de la course, va dans le bon sens
     * et a encore une place. Plusieurs clients peuvent appeler {@link Bus#tryBoard()} en même temps.
     */
    private void tryToBoard(BusRide ride, long currentTick) {
        for (Bus candidate : stops.dwellingAt(ride.from())) {
            if (candidate.line().number() == ride.line() && candidate.isHeadingTo(ride.to()) && candidate.tryBoard()) {
                bus = candidate;
                setState(State.IN_TRANSIT);
                metrics.recordBoarding(currentTick - waitingSince);
                payBusFareOnce();
                return;
            }
        }
    }

    /** Descend dès que le bus est arrêté au dernier arrêt de la course. */
    private void getOffAtLastStop(BusRide ride, long currentTick) {
        if (bus.isAtStop() && bus.currentNode() == ride.to()) {
            bus.alight();
            bus = null;
            finishRide(currentTick);
        }
    }

    // ------------------------------------------------------------------ piloté par un Uber

    /**
     * Appelé en même temps par tous les Uber libres qui veulent ce client.
     *
     * @return true pour exactement un Uber : celui qui prend désormais ce client en charge
     */
    public boolean tryClaim() {
        if (!(currentRide() instanceof UberRide)) {
            return false;
        }
        return state.compareAndSet(State.WAITING, State.ASSIGNED);
    }

    void pickedUp(long currentTick) {
        setState(State.IN_TRANSIT);
        uberPickupTick = currentTick;
        metrics.recordBoarding(currentTick - waitingSince);
    }

    /** Paie la course (montant de base + temps passé dans la voiture), puis la termine. */
    void droppedOff(long currentTick) {
        long fare = fares.uberRideCents(currentTick - uberPickupTick);
        paidCents += fare;
        metrics.recordUberFare(fare);
        finishRide(currentTick);
    }

    /** Le billet de bus couvre tout le trajet : seule la première montée paie. */
    private void payBusFareOnce() {
        if (!busFarePaid) {
            busFarePaid = true;
            paidCents += fares.busTripCents();
            metrics.recordBusFare(fares.busTripCents());
        }
    }

    // ------------------------------------------------------------------ commun

    /** Fin d'une course : soit le trajet est fini, soit le client attend la course suivante (une correspondance). */
    private void finishRide(long currentTick) {
        if (rideIndex == trip.size() - 1) {
            setState(State.ARRIVED);
            metrics.recordDelivery(currentTick - spawnTick, paidCents);
        } else {
            rideIndex++;
            waitingSince = currentTick;
            setState(State.WAITING);
            metrics.recordTransfer();
        }
    }

    private void setState(State next) {
        state.set(next);
    }

    public State state() {
        return state.get();
    }

    public Ride currentRide() {
        return trip.get(rideIndex);
    }

    /** En attente d'un bus ou d'un Uber (réservé ou non), à {@link #currentRide()}{@code .from()}. */
    public boolean isWaiting() {
        State s = state();
        return s == State.WAITING || s == State.ASSIGNED;
    }

    /** Le tick depuis lequel il attend sa course actuelle. */
    public long waitingSince() {
        return waitingSince;
    }

    /** Tout ce qui a été payé jusqu'ici, en cents. */
    public long paidCents() {
        return paidCents;
    }

    public int id() {
        return id;
    }

    public List<Ride> trip() {
        return trip;
    }
}

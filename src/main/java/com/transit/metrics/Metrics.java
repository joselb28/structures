package com.transit.metrics;

import com.transit.model.Time;
import java.util.concurrent.atomic.LongAdder;

/**
 * Les compteurs globaux du tableau de bord, mis à jour en même temps par les clients pendant la phase parallèle.
 *
 * <p>{@code LongAdder} plutôt que {@code AtomicLong} : beaucoup de threads écrivent, l'interface lit rarement,
 * et LongAdder répartit la contention sur plusieurs cellules au lieu de répéter un seul CAS.
 */
public final class Metrics {

    private final LongAdder spawned = new LongAdder();
    private final LongAdder boarded = new LongAdder();
    private final LongAdder delivered = new LongAdder();
    private final LongAdder totalWaitTicks = new LongAdder();
    private final LongAdder totalTripTicks = new LongAdder();
    private final LongAdder transfers = new LongAdder();
    private final LongAdder busFares = new LongAdder();
    private final LongAdder busRevenueCents = new LongAdder();
    private final LongAdder uberFares = new LongAdder();
    private final LongAdder uberRevenueCents = new LongAdder();
    private final LongAdder deliveredTripsPriceCents = new LongAdder();

    public void recordSpawn() {
        spawned.increment();
    }

    // TODO Q1 (★) : garder la plus longue attente avec un AtomicLong (accumulateAndGet) — voir docs/QUESTIONS.md
    public void recordBoarding(long waitTicks) {
        boarded.increment();
        totalWaitTicks.add(waitTicks);
    }

    /**
     * @param tripTicks  durée porte-à-porte, de l'apparition à l'arrivée
     * @param priceCents tout ce que le client a payé pour le trajet
     */
    public void recordDelivery(long tripTicks, long priceCents) {
        delivered.increment();
        totalTripTicks.add(tripTicks);
        deliveredTripsPriceCents.add(priceCents);
    }

    public void recordBusFare(long cents) {
        busFares.increment();
        busRevenueCents.add(cents);
    }

    public void recordUberFare(long cents) {
        uberFares.increment();
        uberRevenueCents.add(cents);
    }

    public void recordTransfer() {
        transfers.increment();
    }

    public long spawned() {
        return spawned.sum();
    }

    public long boarded() {
        return boarded.sum();
    }

    public long delivered() {
        return delivered.sum();
    }

    /** Attente moyenne à un arrêt avant chaque montée, en minutes simulées. */
    public double averageWaitMinutes() {
        return average(totalWaitTicks.sum(), boarded.sum());
    }

    /** Trajet porte-à-porte moyen (attentes + courses + correspondances), en minutes simulées. */
    public double averageTripMinutes() {
        return average(totalTripTicks.sum(), delivered.sum());
    }

    public long transfers() {
        return transfers.sum();
    }

    /** Trajets en bus payés (un par trajet, correspondances incluses). */
    public long busFares() {
        return busFares.sum();
    }

    public long busRevenueCents() {
        return busRevenueCents.sum();
    }

    /** Courses Uber payées. */
    public long uberFares() {
        return uberFares.sum();
    }

    public long uberRevenueCents() {
        return uberRevenueCents.sum();
    }

    /** Prix moyen d'un trajet terminé, en cents (0 avant la première arrivée). */
    public double averageTripPriceCents() {
        long count = delivered.sum();
        return count == 0 ? 0 : (double) deliveredTripsPriceCents.sum() / count;
    }

    private static double average(long ticks, long count) {
        return count == 0 ? 0 : (double) ticks / count / Time.TICKS_PER_SIM_MINUTE;
    }
}

package com.transit.trip;

/**
 * Une course en Uber : prise en charge au nœud {@code from}, dépôt au nœud {@code to}.
 */
public record UberRide(int from, int to) implements Ride {
}

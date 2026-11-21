package com.transit.trip;

/**
 * Une course en bus sur la ligne {@code line} : montée à l'arrêt {@code from}, descente à l'arrêt {@code to}.
 */
public record BusRide(int line, int from, int to) implements Ride {
}

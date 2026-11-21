package com.transit.trip;

/**
 * Une partie du trajet d'un client, d'un nœud à un autre, dans un seul véhicule.
 * Un trajet est une liste de courses, chacune partant de là où la précédente se termine.
 */
public sealed interface Ride permits BusRide, UberRide {

    int from();

    int to();
}
